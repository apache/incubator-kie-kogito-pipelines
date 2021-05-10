#!/bin/sh
set -euo pipefail

# kogito or optaplanner
PROJECT=kogito
BRANCH=master
DEFAULT_BRANCH=master
DRY_RUN=false
FORK=

usage() {
    echo 'Usage: update-kie-versions.sh -p $PROJECT -s $KIE_VERSION -b $BASE_BRANCH -f $FORK -n'
    echo
    echo 'Options:'
    echo '  -p $PROJECT      set kogito or optaplanner -- default is kogito'
    echo '  -s $KIE_VERSION  set version'
    echo '  -b $BASE_BRANCH  should be main or a version branch'
    echo '  -f $FORK         GH account where the branch should be pushed'
    echo '  -n               no execution: clones, creates the branch, but will not push or create the PR'

}

args=`getopt p:s:b:f:nh $*`
if [ $? != 0 ]
then
        echo 'Usage: ...'
        exit 2
fi
set -- $args
for i
do
        case "$i"
        in
                -p)
                        PROJECT=$2;
                        shift;shift ;;
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

case $PROJECT in
    kogito)
        REPO=kogito-runtimes
        ;;
    optaplanner)
        REPO=optaplanner
        ;;
    *)
        >&2 echo ERROR: Unknown project: $PROJECT.
        usage

        exit -1
esac


if [ "$BRANCH" = "" ]; then BRANCH=$DEFAULT_BRANCH; else PREFIX="${BRANCH}-"; fi
if [ "$BRANCH" = "master" ]; then PREFIX=""; else PREFIX="${BRANCH}-"; fi

if [ "$FORK" = "" ]; then 
        >&2 echo ERROR: no fork specified.
        usage()

        exit -1
fi


ORIGIN=kiegroup/$REPO
PR_FORK=$FORK/$REPO
PR_BRANCH=bump-${PREFIX}kie-$KIE_VERSION


echo PROJECT......$PROJECT 
echo ORIGIN.......$ORIGIN
echo PR_FORK......$PR_FORK
echo BRANCH.......$BRANCH
echo PR_BRANCH....$PR_BRANCH
echo VERSION......$KIE_VERSION
echo
echo DRY_RUN! No changes will be pushed!
echo


git clone https://github.com/$ORIGIN
cd $REPO

git checkout $BRANCH

# create branch named like version
git checkout -b $PR_BRANCH
 
# process versions
mvn -pl :${PROJECT}-build-parent \
versions:set-property \
-Dproperty=version.org.kie7 \
-DnewVersion=$KIE_VERSION \
-DgenerateBackupPoms=false
 
# commit all
git commit -am "[$BRANCH] Bump KIE $KIE_VERSION"
 
if [ "$DRY_RUN" = "" ]; then
    # push the branch to a remote
    git push -u $PR_FORK $PR_BRANCH
    
    # Open a PR to kogito-runtimes using the commit as a title
    # e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
    gh pr create --fill --base $BRANCH -R $ORIGIN
fi

