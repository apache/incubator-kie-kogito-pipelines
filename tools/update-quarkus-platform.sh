#!/usr/bin/env bash

set -euo pipefail

GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

VERSION=
PROJECT=kogito
DRY_RUN=false
BRANCH=main

usage() {
    echo 'Usage: update-quarkus-platform.sh -v $VERSION -f $FORK [-s] [-b] [-n] COMMAND'
    echo
    echo 'Options:'
    echo '  -v $VERSION          set version'
    echo '  -p project           `kogito` or `optaplanner`. Default is kogito.'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -b $BRANCH           Quarkus Platform branch (optional. Default is `main`)'
    echo '  -n                   no execution: clones, creates the branch, but will not push or create the PR'
    echo '  COMMAND              may be `stage` or `finalize`'
    echo
    echo 'Examples:'#!/bin/sh
    echo '  # Stage the PR'
    echo '  #  - Bump Kogito 1.7.0.Final
    echo '  #  - Add staging repositories'
    echo '  #  - Push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run'
    echo '  sh update-quarkus-platform.sh -v 1.7.0.Final -p kogito -f evacchi -n stage'
    echo
    echo '  # Finalize the PR:'
    echo '  #  - Remove staging repositories'
    echo '  #  - Force-push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run'
    echo '  sh update-quarkus-platform.sh -v 7.0.Final -f evacchi -n finalize'
}

args=`getopt v:p:f:b:snh $*`
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
                -s)     
                        GITHUB_URL=${GITHUB_URL_SSH}
                        shift;;
                -b)     
                        BRANCH=$2
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


if [ "$VERSION" = "" ]; then 
    >&2 echo ERROR: no version specified.
    usage

    exit 2
fi

if [ "$FORK" = "" ]; then 
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

PR_BRANCH=bump-${PROJECT}-${VERSION}


echo GITHUB_URL...............$GITHUB_URL
echo ORIGIN...................$ORIGIN
echo PR_FORK..................$PR_FORK
echo BRANCH...................$BRANCH
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

    git clone ${GITHUB_URL}${ORIGIN}
    cd $REPO

    # ensure main branch
    git checkout $BRANCH
    # create branch
    git checkout -b $PR_BRANCH
    
    # add custom repositories
    echo "$DIFF_FILE" | patch .github/mvn-settings.xml
    
    # process versions
    ./mvnw \
    -s .github/mvn-settings.xml \
    versions:set-property \
    -Dproperty=${PROJECT}-quarkus.version \
    -DnewVersion=${VERSION} \
    -DgenerateBackupPoms=false
    
    # update pom metadata
    ./mvnw -s .github/mvn-settings.xml -Dsync
    
    # commit all
    git commit -am "${PROJECT} ${VERSION}"

    if [ "$DRY_RUN" = "false" ]; then
        git push -u ${GITHUB_URL}$PR_FORK $PR_BRANCH
        gh pr create --fill --base $BRANCH -R $ORIGIN
    fi
}

finalize() {
    set -x

    if [ -d "$REPO" ]; then
        cd $REPO
    else
        git clone ${GITHUB_URL}$PR_FORK
        cd $REPO;
    fi

    git checkout $PR_BRANCH

    # undo patch to add repos
    echo "$DIFF_FILE" | patch -R .github/mvn-settings.xml

    ./mvnw -Dsync

    # overwrite old commit (no need to squash)
    git commit --amend --no-edit pom.xml
    
    if [ "$DRY_RUN" = "false" ]; then
        # push forced (we are overwriting the old commit)
        git push --force-with-lease
    fi
}

DIFF_FILE='diff --git a/.github/mvn-settings.xml b/.github/mvn-settings.xml
index d5e4664b..b03cc023 100644
--- a/.github/mvn-settings.xml
+++ b/.github/mvn-settings.xml
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
+          <url>https://repository.jboss.org/nexus/content/groups/kogito-public/</url>
+        </repository>
       </repositories>
       <pluginRepositories>
         <pluginRepository>
'
# execute
$COMMAND
