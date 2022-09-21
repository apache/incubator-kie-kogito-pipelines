#!/usr/bin/env bash

set -euo pipefail

GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

VERSION=
PROJECT=kogito
DRY_RUN=false
BASE_BRANCH=main
PR_BRANCH=
DISABLE_CHECKOUT=false
IGNORE_GIT=false
COMMIT_MSG=

usage() {
    echo 'Usage: update-quarkus-platform.sh -v $VERSION [-p PROJECT] -f $FORK [-d] [-s] [-b BASE_BRANCH] [-h PR_BRANCH] [-m COMMIT_MESSAGE] [-n] [-r] COMMAND'
    echo
    echo 'Options:'
    echo '  -v $VERSION          set version'
    echo '  -p project           `kogito` or `optaplanner`. Default is kogito.'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -b $BASE_BRANCH      Quarkus Platform branch (optional. Default is `main`)'
    echo '  -h $PR_BRANCH        Branch to use for the PR'
    echo '  -m $COMMIT_MSG       Commit message to put'
    echo '  -d                   Disable checkout. This means you are running the script in an already checked out quarkus-platform repository.'
    echo '  -n                   no execution: this will neither push nor create the PR'
    echo '  COMMAND              may be `stage` or `finalize`'
    echo '  -r                   where does the script run: on Jenkins or locally'
    echo
    echo 'Examples:'
    echo '  # Stage the PR'
    echo '  #  - Bump Kogito 1.24.0.Final'
    echo '  #  - Add staging repositories'
    echo '  #  - Commit the changes'
    echo '  #  - Dry Run'
    echo '  sh update-quarkus-platform.sh -v 1.24.0.Final -p kogito -f evacchi -n stage'
    echo
    echo '  # Update a current checked out repository'
    echo '  #  - Bump Kogito 1.24.0.Final'
    echo '  #  - Add staging repositories'
    echo '  #  - Commit the changes'
    echo '  #  - Push the `bump-kogito-world` branch to evacchi/quarkus-platform'
    echo '  sh update-quarkus-platform.sh -v 1.24.0.Final -p kogito -d -f evacchi -h bump-kogito-world stage'
    echo
    echo '  # Finalize the PR:'
    echo '  #  - Remove staging repositories'
    echo '  #  - Force-push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run'
    echo '  sh update-quarkus-platform.sh -v 1.24.0.Final -p kogito -f evacchi -n finalize'
}

while getopts "v:p:f:b:h:m:dsnrh" i
do
    case "$i"
    in
        v)  VERSION=${OPTARG} ;;
        p)  PROJECT=${OPTARG} ;;
        f)  FORK=${OPTARG} ;;
        b)  BASE_BRANCH=${OPTARG} ;;
        h)  PR_BRANCH=${OPTARG} ;;
        m)  COMMIT_MSG=${OPTARG} ;;
        s)  GITHUB_URL=${GITHUB_URL_SSH} ;;
        d)  DISABLE_CHECKOUT=true ;;
        n)  DRY_RUN=true ;;
        r)  IGNORE_GIT=true ;;
        h)  usage; exit 0 ;;
        \?) usage; exit 1 ;;
    esac
done
shift "$((OPTIND-1))"

case "$1"
in
    stage)
        COMMAND=stage
        ;;
    finalize)
        COMMAND=finalize
        ;;
    *)
        >&2 echo ERROR: invalid command $COMMAND.
        usage

        exit 2
esac

if [ -z "$VERSION" ]; then
    >&2 echo ERROR: no version specified.
    usage

    exit 2
fi

if [ -z "$FORK" ]; then
    >&2 echo ERROR: no fork specified.
    usage

    exit 2
fi

if [ -z "$PR_BRANCH" ]; then
    PR_BRANCH=bump-${PROJECT}-${VERSION}
fi

if [ -z "$COMMIT_MSG" ]; then
    COMMIT_MSG="${PROJECT} ${VERSION}"
fi

REPO=quarkus-platform
PR_FORK=$FORK/$REPO
ORIGIN=quarkusio/$REPO

echo GITHUB_URL...............$GITHUB_URL
echo ORIGIN...................$ORIGIN
echo PR_FORK..................$PR_FORK
echo BASE_BRANCH..............$BASE_BRANCH
echo PR_BRANCH................$PR_BRANCH
echo COMMIT_MSG...............$COMMIT_MSG
echo VERSION..................$VERSION
echo COMMAND..................$COMMAND
echo
if [ "$DRY_RUN" = "true" ]; then
echo DRY_RUN! No changes will be pushed!
echo
fi

stage() {
    set -x
    if [ "$IGNORE_GIT" != 'true' ]; then

      if [ "${DISABLE_CHECKOUT}" != 'true' ]; then
          git clone ${GITHUB_URL}${ORIGIN}
          cd $REPO
      fi

      # ensure base branch
      git checkout $BASE_BRANCH

      # create branch if needed
      set +e
      git checkout $PR_BRANCH
      if [ "$?" != "0" ]; then
          git checkout -b $PR_BRANCH
      fi
      set -e
    fi

    # add custom repositories
    set +e
    cat ${MAVEN_SETTINGS_FILE} | grep ${KOGITO_STAGING_REPOSITORY}
    if [ "$?" != "0" ]; then
      echo "$DIFF_FILE" | patch ${MAVEN_SETTINGS_FILE}
    fi
    set -e

    # process versions
    ./mvnw \
    -s ${MAVEN_SETTINGS_FILE} \
    versions:set-property \
    -Dproperty=${PROJECT}-quarkus.version \
    -DnewVersion=${VERSION} \
    -DgenerateBackupPoms=false

    # update pom metadata
    ./mvnw -s ${MAVEN_SETTINGS_FILE} -Dsync

    if [ "$IGNORE_GIT" != 'true' ]; then
        # commit all
        git commit -am "${COMMIT_MSG}"

        if [ "$DRY_RUN" = "false" ]; then
            git push -u ${GITHUB_URL}$PR_FORK $PR_BRANCH
            gh pr create --fill --base $BASE_BRANCH -R $ORIGIN
        else
            echo 'Do not push/create PR as per parameters...'
        fi
    fi
}

finalize() {
    set -x
    if [ "$IGNORE_GIT" != 'true' ]; then

        if [ "${DISABLE_CHECKOUT}" != 'true' ]; then
            if [ -d "$REPO" ]; then
                cd $REPO
            else
                git clone ${GITHUB_URL}$PR_FORK
                cd $REPO;
            fi
        fi

        git checkout $PR_BRANCH
    fi

    # undo patch to add repos
    set +e
    cat ${MAVEN_SETTINGS_FILE} | grep ${KOGITO_STAGING_REPOSITORY}
        if [ "$?" = "0" ]; then
            echo "$DIFF_FILE" | patch -R ${MAVEN_SETTINGS_FILE}
        fi
    set -e

    ./mvnw -Dsync

    if [ "$IGNORE_GIT" != 'true' ]; then
        # squash commits
        git reset $(git merge-base main $(git rev-parse --abbrev-ref HEAD))
        git add -A
        git commit -m "${COMMIT_MSG}"

        if [ "$DRY_RUN" = "false" ]; then
            # push forced (we are overwriting the old commit)
            git push --force-with-lease
        else
            echo 'Do not push as per parameters...'
        fi
    fi
}

KOGITO_STAGING_REPOSITORY='https://repository.jboss.org/nexus/content/groups/kogito-public/'
MAVEN_SETTINGS_FILE='.github/mvn-settings.xml'
DIFF_FILE="diff --git a/${MAVEN_SETTINGS_FILE} b/${MAVEN_SETTINGS_FILE}
index d5e4664b..b03cc023 100644
--- a/${MAVEN_SETTINGS_FILE}
+++ b/${MAVEN_SETTINGS_FILE}
@@ -14,6 +14,14 @@
             <enabled>false</enabled>
           </snapshots>
         </repository>
+        <repository>
+          <snapshots>
+              <enabled>false</enabled>
+          </snapshots>
+          <id>kogito</id>
+          <name>kogito</name>
+          <url>${KOGITO_STAGING_REPOSITORY}</url>
+        </repository>
       </repositories>
       <pluginRepositories>
         <pluginRepository>
"
# execute
$COMMAND
