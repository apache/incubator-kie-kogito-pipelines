#!/bin/sh
set -euo pipefail

GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

BRANCH=main
DEFAULT_BRANCH=main
DRY_RUN=false
FORK=
KIE_VERSION=

usage() {
    echo 'Usage: update-kie7-versions.sh -s $KIE_VERSION -b $BASE_BRANCH -f $FORK [-s] [-n]'
    echo
    echo 'Options:'
    echo '  -v $KIE_VERSION  set version'
    echo '  -b $BASE_BRANCH  should be main or a version branch'
    echo '  -s               Use SSH to connect to GitHub'
    echo '  -f $FORK         GH account where the branch should be pushed'
    echo '  -n               no execution: clones, creates the branch, but will not push or create the PR'
    echo
    echo 'Examples:'
    echo '  #  - Update Kogito to KIE 7.54.0.Final, '
    echo '  #  - Base branch is main'
    echo '  #  - Push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run '
    echo '  sh update-kie7-versions.sh -v 7.54.0.Final -b main -f evacchi -n'
    echo
}

args=`getopt v:b:f:snh $*`
if [ $? != 0 ]
then
        usage
        exit 2
fi
set -- $args
for i
do
        case "$i"
        in
                -s)
                        KIE_VERSION=$2;
                        shift;shift ;;
                -b)
                        BRANCH=$2
                        shift;shift ;;
                -s)     
                        GITHUB_URL=${GITHUB_URL_SSH}
                        shift;;
                -f)
                        FORK=$2
                        shift;shift ;;

                -n)     
                        DRY_RUN=true
                        shift;;
                -h)     
                        usage;
                        exit 0;
                        ;;
        esac
done

## validation

if [ "$KIE_VERSION" = "" ]; then 
        >&2 echo ERROR: no version specified.
        usage

        exit -1
fi

if [ "$BRANCH" = "" ]; then BRANCH=$DEFAULT_BRANCH; else PREFIX="${BRANCH}-"; fi
if [ "$BRANCH" = "$DEFAULT_BRANCH" ]; then PREFIX=""; else PREFIX="${BRANCH}-"; fi

if [ "$FORK" = "" ]; then 
        >&2 echo ERROR: no fork specified.
        usage

        exit -1
fi

REPO=kogito-runtimes
ORIGIN=kiegroup/$REPO
PR_FORK=$FORK/$REPO
PR_BRANCH=bump-${PREFIX}kie-$KIE_VERSION


echo ORIGIN.......$ORIGIN
echo PR_FORK......$PR_FORK
echo BRANCH.......$BRANCH
echo PR_BRANCH....$PR_BRANCH
echo VERSION......$KIE_VERSION
echo
if [ "$DRY_RUN" = "true" ]; then
echo DRY_RUN! No changes will be pushed!
echo
fi


git clone ${GITHUB_URL}${ORIGIN}
cd $REPO

git checkout $BRANCH

# create branch named like version
git checkout -b $PR_BRANCH
 
# process versions
mvn -pl :kogito-kie7-bom \
versions:set-property \
-Dproperty=version.org.kie7 \
-DnewVersion=$KIE_VERSION \
-DgenerateBackupPoms=false
 
# commit all
git commit -am "[$BRANCH] Bump KIE $KIE_VERSION"
 
if [ "$DRY_RUN" = "false" ]; then
    # push the branch to a remote
    git push -u ${GITHUB_URL}${PR_FORK} ${PR_BRANCH}
    
    # Open a PR to kogito-runtimes using the commit as a title
    # e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
    gh pr create --fill --base $BRANCH -R $ORIGIN
fi

