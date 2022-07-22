#!/usr/bin/env bash
set -eo pipefail

script_dir_path=`dirname "${BASH_SOURCE[0]}"`
 
GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

REPO=kogito-runtimes
DRY_RUN=false
BRANCH=main

usage() {
    echo 'Usage: update-quarkus-versions.sh -p $PROJECT -q $QUARKUS_VERSION -b $BASE_BRANCH -f $FORK [-s] [-n]'
    echo
    echo 'Options:'
    echo '  -p $PROJECT          set kogito-runtimes, optaplanner. kogito-examples or optaplanner-quickstarts -- default is kogito-runtimes'
    echo '  -q $QUARKUS_VERSION  set version'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -b $BASE_BRANCH      should be main or a version branch'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -n                   no execution: clones, creates the branch, but will not push or create the PR'
    echo
    echo 'Examples:'
    echo '  #  - Update Kogito to Quarkus 2.0.0.Final, '
    echo '  #  - Base branch is main'
    echo '  #  - Push the branch to evacchi/quarkus-platform'
    echo '  #  - Dry Run '
    echo '  sh update-quarkus-versions.sh -p kogito -q 2.0.0.Final -m 3.6.2 -b main -f evacchi -n'
    echo
}

args=`getopt p:q:b:f:snh $*`
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
                        REPO=$2;
                        shift;shift ;;
                -q)
                        QUARKUS_VERSION=$2;
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

MODULES=
QUARKUS_PROPERTIES=
GRADLE_REGEX=

case $REPO in
    drools)
        MODULES[0]=drools-build-parent
        QUARKUS_PROPERTIES[0]=version.io.quarkus
        QUARKUS_PROPERTIES[1]=version.io.quarkus.quarkus-test-maven
        ;;
    kogito-runtimes)
        MODULES[0]=kogito-dependencies-bom
        MODULES[1]=kogito-build-parent
        MODULES[2]=kogito-quarkus-bom
        MODULES[3]=kogito-build-no-bom-parent
        QUARKUS_PROPERTIES[0]=version.io.quarkus
        QUARKUS_PROPERTIES[1]=version.io.quarkus.quarkus-test
        ;;
    optaplanner)
        MODULES[0]=optaplanner-build-parent
        QUARKUS_PROPERTIES[0]=version.io.quarkus
        ;;
    kogito-examples)
        QUARKUS_PROPERTIES[0]=quarkus-plugin.version
        QUARKUS_PROPERTIES[1]=quarkus.platform.version
        ;;
    optaplanner-quickstarts)
        QUARKUS_PROPERTIES[0]=version.io.quarkus
        GRADLE_REGEX[0]='id "io.quarkus" version'
        GRADLE_REGEX[1]='def quarkusVersion ='
        ;;
    *)
        >&2 echo ERROR: Unknown project: $REPO.
        usage

        exit 2
esac


if [ "$FORK" = "" ]; then 
        >&2 echo ERROR: no fork specified.
        usage

        exit 2
fi

ORIGIN=kiegroup/$REPO
PR_FORK=$FORK/$REPO

PR_BRANCH=${BRANCH}-bump-quarkus-$QUARKUS_VERSION

echo PROJECT................$REPO 
echo ORIGIN.................$ORIGIN
echo PR_FORK................$PR_FORK
echo BRANCH.................$BRANCH
echo PR_BRANCH..............$PR_BRANCH
echo VERSION................$QUARKUS_VERSION
echo QUARKUS_PROPERTIES.....${QUARKUS_PROPERTIES[@]}
echo
if [ "$DRY_RUN" = "true" ]; then
echo DRY_RUN! No changes will be pushed!
echo
fi

# print all commands
set -x

# git clone ${GITHUB_URL}${ORIGIN}
# cd $REPO

# git checkout $BRANCH

# # create branch named like version
# git checkout -b $PR_BRANCH

# update Quarkus version
function update_quarkus_properties() {
  for prop in "${QUARKUS_PROPERTIES[@]}"
  do
    ${script_dir_path}/update-maven-module-property.sh ${prop} ${QUARKUS_VERSION} $1
  done
}

function update_gradle_regexps() {
  for re in "${GRADLE_REGEX[@]}"
  do
    ${script_dir_path}/update-build-gradle-regex-line.sh "${re}" ${QUARKUS_VERSION}
  done
}

if [ -z $MODULES ]; then
  update_quarkus_properties
else
  for i in "${MODULES[@]}"
  do
    ${script_dir_path}/update-maven-compare-dependencies.sh 'io.quarkus:quarkus-bom' ${QUARKUS_VERSION} ${i}
  
    update_quarkus_properties ${i}
  done
fi

if [ ! -z "${GRADLE_REGEX}" ]; then
  update_gradle_regexps
fi
 
# commit all
git commit -am "Bump Quarkus $QUARKUS_VERSION"

# if [ "$DRY_RUN" = "false" ]; then
#    # push the branch to a remote
#    git push -u ${GITHUB_URL}${PR_FORK} ${PR_BRANCH}
   
#    # Open a PR to kogito-runtimes using the commit as a title
#    # e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
#    gh pr create --fill --base $BRANCH -R $ORIGIN
# fi