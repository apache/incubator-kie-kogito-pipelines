#!/bin/sh
set -euo pipefail
 
GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

MAVEN_VERSION=3.6.2
PROJECT=kogito

DRY_RUN=false

usage() {
    echo 'Usage: update-quarkus-versions.sh -p $PROJECT -s $QUARKUS_VERSION -m $MAVEN_VERSION -b $BASE_BRANCH -f $FORK [-s] [-n]'
    echo
    echo 'Options:'
    echo '  -p $PROJECT          set kogito or optaplanner -- default is kogito'
    echo '  -q $QUARKUS_VERSION  set version'
    echo '  -m $MAVEN_VERSION    set version'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -b $BASE_BRANCH      should be main or a version branch'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -n                   no execution: clones, creates the branch, but will not push or create the PR'
    echo
    echo 'Examples:'
    echo '  #  - Update Kogito to Quarkus 2.0.0.Final, '
    echo '  #  - Pin MAVEN_VERSION to 3.6.2'
    echo '  #  - Base branch is main'
    echo '  #  - Push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run '
    echo '  sh update-quarkus-versions.sh -p kogito -q 2.0.0.Final -m 3.6.2 -b main -f evacchi -n'
    echo
}

args=`getopt p:q:m:s:b:f:nh $*`
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
                -p)
                        PROJECT=$2;
                        shift;shift ;;
                -q)
                        QUARKUS_VERSION=$2;
                        shift;shift ;;
                -m)
                        MAVEN_VERSION=$2
                        shift;shift ;;
                -s)     
                        GITHUB_URL=${GITHUB_URL_SSH}
                        shift;;
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

        exit 2
esac


if [ "$FORK" = "" ]; then 
        >&2 echo ERROR: no fork specified.
        usage

        exit 2
fi


# kogito or optaplanner
PROJECT=kogito
# kogito-runtimes or optaplanner
REPO=kogito-runtimes

ORIGIN=kiegroup/$REPO
PR_FORK=$FORK/$REPO
BRANCH=main
PREFIX=""
if [ "$BRANCH" = "" ]; then BRANCH=$DEFAULT_BRANCH; else PREFIX="${BRANCH}-"; fi
if [ "$BRANCH" = "main" ]; then PREFIX=""; else PREFIX="${BRANCH}-"; fi

# kogito-runtimes or optaplanner
PR_BRANCH=bump-${PREFIX}quarkus-$QUARKUS_VERSION

echo PROJECT......$PROJECT 
echo ORIGIN.......$ORIGIN
echo PR_FORK......$PR_FORK
echo BRANCH.......$BRANCH
echo PR_BRANCH....$PR_BRANCH
echo VERSION......$QUARKUS_VERSION
echo
if [ "$DRY_RUN" = "true" ]; then
echo DRY_RUN! No changes will be pushed!
echo
fi

# print all commands
set -x

git clone ${GITHUB_URL}${ORIGIN}
cd $REPO

# create branch named like version
git checkout -b $PR_BRANCH
 
# align third-party dependencies with Quarkus
mvn versions:compare-dependencies \
-pl :${PROJECT}-build-parent \
-DremotePom=io.quarkus:quarkus-bom:$QUARKUS_VERSION \
-DupdatePropertyVersions=true \
-DupdateDependencies=true \
-DgenerateBackupPoms=false
 
# update Quarkus version
mvn -pl :${PROJECT}-build-parent \
   versions:set-property \
   -Dproperty=version.io.quarkus \
   -DnewVersion=$QUARKUS_VERSION \
   -DgenerateBackupPoms=false
 
# pin Maven version
mvn -pl :${PROJECT}-build-parent \
versions:set-property \
-Dproperty=version.maven \
-DnewVersion=$MAVEN_VERSION \
-DgenerateBackupPoms=false
 
# commit all
git commit -am "Bump Quarkus $QUARKUS_VERSION"

if [ "$DRY_RUN" = "false" ]; then
   # push the branch to a remote
   git push -u ${GITHUB_URL}${PR_FORK} ${PR_BRANCH}
   
   # Open a PR to kogito-runtimes using the commit as a title
   # e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
   gh pr create --fill --base $BRANCH -R $ORIGIN
fi
