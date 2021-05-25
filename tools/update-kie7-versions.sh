#!/bin/sh
set -euo pipefail

BRANCH=master
DEFAULT_BRANCH=master
DRY_RUN=false
FORK=
KIE_VERSION=

usage() {
    echo 'Usage: update-kie7-versions.sh -s $KIE_VERSION -b $BASE_BRANCH -f $FORK [-n]'
    echo
    echo 'Options:'
    echo '  -s $KIE_VERSION  set version'
    echo '  -b $BASE_BRANCH  should be main or a version branch'
    echo '  -f $FORK         GH account where the branch should be pushed'
    echo '  -n               no execution: clones, creates the branch, but will not push or create the PR'
    echo
    echo 'Examples:'
    echo '  #  - Update Kogito to KIE 7.54.0.Final, '
    echo '  #  - Base branch is master'
    echo '  #  - Push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run '
    echo '  sh update-kie7-versions.sh -s 7.54.0.Final -b master -f evacchi -n'
    echo
}

args=`getopt s:b:f:nh $*`
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


git clone https://github.com/$ORIGIN
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
    git push -u $PR_FORK $PR_BRANCH
    
    # Open a PR to kogito-runtimes using the commit as a title
    # e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
    gh pr create --fill --base $BRANCH -R $ORIGIN
fi

