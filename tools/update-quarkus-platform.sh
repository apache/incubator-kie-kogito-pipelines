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

usage() {
    echo 'Usage: update-quarkus-platform.sh -v $VERSION [-p PROJECT] -f $FORK [-d] [-s] [-b BASE_BRANCH] [-h PR_BRANCH] [-n] COMMAND'
    echo
    echo 'Options:'
    echo '  -v $VERSION          set version'
    echo '  -p project           `kogito` or `optaplanner`. Default is kogito.'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -d                   Disable checkout. This means you are running the script in an already checked out quarkus-platform repository.'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -b $BASE_BRANCH      Quarkus Platform branch (optional. Default is `main`)'
    echo '  -h $PR_BRANCH        Branch to use for the PR'
    echo '  -n                   no execution: clones, creates the branch, but will not push or create the PR'
    echo '  COMMAND              may be `stage` or `finalize`'
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

args=`getopt v:p:f:b:h:dsnh $*`
if [ "$#" -eq 0 -o $? != 0 ]; then
    >&2 echo ERROR: no args given.

    usage
    exit 2
fi
set -- $args
for i
do
        case "$i"
        in
                -v)
                        VERSION=$2;
                        shift;shift ;;
                -p)
                        PROJECT=$2
                        shift;shift ;;
                -f)
                        FORK=$2
                        shift;shift ;;
                -d)     
                        DISABLE_CHECKOUT=true
                        shift;;
                -s)     
                        GITHUB_URL=${GITHUB_URL_SSH}
                        shift;;
                -b)     
                        BASE_BRANCH=$2
                        shift;shift ;;
                -h)     
                        PR_BRANCH=$2
                        shift;shift ;;
                -n)     
                        DRY_RUN=true
                        shift;;
                -h)     
                        usage;
                        exit 0;
                        ;;
                --)
                        COMMAND=$2
                        shift; shift
                        ;;
                            
        esac
done


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

case "$COMMAND"
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

REPO=quarkus-platform
PR_FORK=$FORK/$REPO
ORIGIN=quarkusio/$REPO

if [ -z "$PR_BRANCH" ]; then 
    PR_BRANCH=bump-${PROJECT}-${VERSION}
fi


echo GITHUB_URL...............$GITHUB_URL
echo ORIGIN...................$ORIGIN
echo PR_FORK..................$PR_FORK
echo BASE_BRANCH..............$BASE_BRANCH
echo PR_BRANCH................$PR_BRANCH
echo VERSION..................$VERSION
echo COMMAND..................$COMMAND
echo
if [ "$DRY_RUN" = "true" ]; then
echo DRY_RUN! No changes will be pushed!
echo
fi


stage() {
    set -x

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
    
    # add custom repositories
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
    
    # commit all
    git commit -am "${PROJECT} ${VERSION}"

    if [ "$DRY_RUN" = "false" ]; then
        git push -u ${GITHUB_URL}$PR_FORK $PR_BRANCH
        gh pr create --fill --base $BASE_BRANCH -R $ORIGIN
    else
        echo 'Do not push/create PR as per parameters...'
    fi
}

finalize() {
    set -x

    if [ "${DISABLE_CHECKOUT}" != 'true' ]; then
        if [ -d "$REPO" ]; then
            cd $REPO
        else
            git clone ${GITHUB_URL}$PR_FORK
            cd $REPO;
        fi
    fi

    git checkout $PR_BRANCH

    # undo patch to add repos
    set +e
    cat ${MAVEN_SETTINGS_FILE} | grep ${KOGITO_STAGING_REPOSITORY}
    if [ "$?" = "0" ]; then
        echo "$DIFF_FILE" | patch -R ${MAVEN_SETTINGS_FILE}
    fi
    set -e

    ./mvnw -Dsync

    # overwrite old commit (no need to squash)
    git commit --amend --no-edit pom.xml
    
    if [ "$DRY_RUN" = "false" ]; then
        # push forced (we are overwriting the old commit)
        git push --force-with-lease
    else
        echo 'Do not push as per parameters...'
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
