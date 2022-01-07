#!/usr/bin/env bash
set -e

GITHUB_URL="https://github.com/"
GITHUB_URL_SSH="git@github.com:"

ORIGIN=
FORK=
REPO=
BRANCH=main
DRY_RUN=false

SCRIPT_FILE=
SCRIPT_FILE_OPTIONS=

usage() {
    echo 'Usage: run-git-script-modification [-o $ORIGIN] -f $FORK [-p $PROJECT] [-b BRANCH] [-s] [-n] -- SCRIPT_FILE SCRIPT_FILE_OPTIONS'
    echo
    echo '\tSCRIPT_FILE will be sourced and need to provide at least a `execute` method. It can also provide `DEFAULT_GIT` env variables (see options)'
    echo '\SCRIPT_FILE_OPTIONS will be passed to the `execute` method and are optional. Please make sure to define the `parseArgs` method which should export all var as env.'
    echo
    echo 'Options:'
    echo '  -o $ORIGIN           Project to checkout (optional. Default can be taken from the called sourced script via `DEFAULT_GIT_ORIGIN` env var)'
    echo '  -f $FORK             GH account where the branch should be pushed'
    echo '  -p $PROJECT          Project to checkout (optional. Default can be taken from the called sourced script via `DEFAULT_GIT_REPOSITORY` env var)'
    echo '  -b $BRANCH           Project branch (optional. Default is `main`)'
    echo '  -s                   Use SSH to connect to GitHub'
    echo '  -n                   no execution: clones, creates the branch, but will not push or create the PR'
    echo
    echo 'Examples:'
    echo '  # Call update-quarkus-platform without options'
    echo '  sh run-git-script-modification -f evacchi -- ./update-quarkus-platform.sh -v 15.0.Final -n stage'
    echo
    echo '  # Call update-quarkus-platform with options'
    echo '  sh run-git-script-modification -o quarkusio -f evacchi -p quarkus-platform -b 2.6 -- ./update-quarkus-platform.sh -v 15.0.Final -n finalize'
}

while getopts "o:f:b:p:snh" i
do
    case "$i"
    in
        o)  ORIGIN=${OPTARG} ;;
        f)  FORK=${OPTARG} ;;
        p)  REPO=${OPTARG} ;;
        b)  BRANCH=${OPTARG} ;;
        s)  GITHUB_URL=${GITHUB_URL_SSH} ;;
        n)  DRY_RUN=true ;;
        h)  usage; exit 0 ;;
        \?) usage; exit 1 ;;
    esac
done
shift "$((OPTIND-1))"

if [ "$1" = "" ]; then 
    >&2 echo ERROR: no script file specified.
    usage
    exit 2
else
    SCRIPT_FILE=$1
    shift
fi

SCRIPT_FILE_OPTIONS=$@

shift $#

echo "SCRIPT_FILE...............${SCRIPT_FILE}"
echo "SCRIPT_FILE_OPTIONS.......${SCRIPT_FILE_OPTIONS}"
echo

source ${SCRIPT_FILE}
args=$(parseArgsAsEnv ${SCRIPT_FILE_OPTIONS})
status=$?
if [ "$status" != "0" ]; then
    exit status
fi
eval $args
args=$(retrieveGitDefaultValuesAsEnv ${BRANCH})
status=$?
if [ "$status" != "0" ]; then
    exit status
fi
eval $args

if [ "${FORK}" = "" ]; then 
        >&2 echo ERROR: no fork specified.
        usage

        exit 2
fi

if [ "${ORIGIN}" = "" ]; then 
    ORIGIN=${DEFAULT_GIT_ORIGIN}
    if [ "${ORIGIN}" = "" ]; then 
        ORIGIN=${FORK}
    fi
fi

if [ "${REPO}" = "" ]; then 
    REPO=${DEFAULT_GIT_REPOSITORY}
    if [ "${REPO}" = "" ]; then 
        >&2 echo ERROR: cannot retrieve project from script. Make sure `DEFAULT_GIT_REPOSITORY` is defined.
        exit 2
    fi
fi

PR_FORK="${FORK}/${REPO}"
ORIGIN="${ORIGIN}/${REPO}"

CHECKOUT_BRANCH=${DEFAULT_GIT_CHECKOUT_BRANCH}
if [ "${CHECKOUT_BRANCH}" = "" ]; then 
    CHECKOUT_BRANCH="${BRANCH}"
fi

PR_BRANCH=${DEFAULT_GIT_PR_BRANCH}
if [ "${PR_BRANCH}" = "" ]; then 
    PR_BRANCH="${BRANCH}_${SCRIPT_FILE}"
fi


echo "GITHUB_URL...............${GITHUB_URL}"
echo "ORIGIN...................${ORIGIN}"
echo "PR_FORK..................${PR_FORK}"
echo "CHECKOUT_BRANCH..........${CHECKOUT_BRANCH}"
echo "PR_BRANCH................${PR_BRANCH}"
echo
showEnv

if [ "${DRY_RUN}" = "true" ]; then
    echo 'DRY_RUN! No changes will be pushed!'
    echo
fi

if [ -d "${REPO}" ]; then
    cd ${REPO}
else
    git clone ${GITHUB_URL}${ORIGIN}
    cd ${REPO};
fi

set -x

git checkout ${CHECKOUT_BRANCH}

if [ "${PR_BRANCH}" != "${CHECKOUT_BRANCH}" ]; then
    # create fork branch
    git checkout -b ${PR_BRANCH}
fi

execute

if [ "${DRY_RUN}" = "false" ]; then
    echo "git commit -am \"${DEFAULT_GIT_COMMIT_MESSAGE}\""

    # push the branch to a fork
    echo "git push --force-with-lease -u ${GITHUB_URL}${PR_FORK} ${PR_BRANCH}"

    if [ "${PR_BRANCH}" != "${CHECKOUT_BRANCH}" ]; then
        # Open a PR to kogito-runtimes using the commit as a title
        echo "gh pr create --fill --base ${CHECKOUT_BRANCH} -R ${ORIGIN}"
    fi
else
    echo 'DRY_RUN! No changes has been pushed!'
    echo
fi