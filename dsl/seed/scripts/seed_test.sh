#!/bin/bash

### This script should be launch by any dependent DSL scripts generation repo for local testing of changes
### Ideal dependent repo should have a `local_test.sh` script located in `.ci/jenkins/dsl` folder
### With that implementation:
# #!/bin/bash -e

# file=$(mktemp)
# curl -o ${file} https://raw.githubusercontent.com/kiegroup/kogito-pipelines/main/dsl/seed/scripts/seed_test.sh
# chmod u+x ${file}
# ${file} $1

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)

TEMP_DIR=`mktemp -d`
GIT_SERVER='github.com'

usage() {
    echo "Usage: $(basename $0) [-b BRANCH] [-o GIT_OWNER] [PATH]"
    echo
    echo 'Options: (only used for any repository different from current pipelines)'
    echo '  -r FULL_REPOSITORY          Full name of the repository, aka `owner/repo`'
    echo '  -o OVERRIDE_REPOSITORY      Override the repository to check'
    echo '  -h BRANCH                   Git Branch to checkout'
    echo '  -t TARGET FULL_REPOSITORY   Full name of the PR target repository, aka `owner/repo`'
    echo '  -b TARGET_BRANCH            PR target branch'
    echo '  -r REPOSITORY        Repository to test jobs from. Please reference the PATH if the jobs.groovy file is not present at the root of the project'
    echo '  -p PATH_TO_PIPELINES Absolute path to local pipelines repository. Else the pipelines repository will be checked out'
    echo '  PATH                 Path to test'
    echo
}

checkout_repository() {
  local repository=$1
  local output_dir=$2

  echo "----- Cloning ${git_server}${owner}/${repository} repo on branch ${branch}"
  git clone --single-branch --depth 1 --branch ${branch} ${git_server}${owner}/${repository} ${output_dir}
  clone_status=$?

  set -e

  if [ "${clone_status}" != '0' ]; then
    echo "[WARN] Error cloning ${git_server}${owner}/${repository} on branch ${target_branch}"
    echo "----- Cloning ${git_server}${target_owner}/${target_repository} on branch ${target_branch}"
    git clone --single-branch --depth 1 --branch ${target_branch} ${git_server}${target_owner}/${target_repository} ${output_dir}
  else
    echo "Clone succeeded"
  fi

  set +e
}

full_repository=
repository=
branch=
target_full_repository=
target_branch='main'
pipelines_repo=

while getopts "r:o:h:t:b:p:h" i
do
    case "$i"
    in
        r) full_repository=${OPTARG} ;;
        o) repository=${OPTARG}; target_repository=${repository} ;;
        h) branch=${OPTARG} ;;
        t) target_full_repository=${OPTARG} ;;
        b) target_branch=${OPTARG} ;;
        p) pipelines_repo=${OPTARG} ;;
        h) usage; exit 0 ;;
        \?) usage; exit 1 ;;
    esac
done
shift "$((OPTIND-1))"

dsl_path=
if [ ! -z "$1" ]; then
  dsl_path=$1
  shift
fi

git_url=$(git remote -v | grep origin | awk -F' ' '{print $2}' | head -n 1)
echo "Got Git URL = ${git_url}"

git_server=
if [[ "${git_url}" = https://* ]]; then
  git_server="https://${GIT_SERVER}/"
elif [[ "${git_url}" = git@* ]]; then 
  git_server="git@${GIT_SERVER}:"
else
  echo "Unknown protocol for url ${git_url}"
  exit 1
fi

if [ -z "${branch}" ]; then
  branch=$(git branch --show-current)
  echo "Got current branch = ${branch}"
fi

# parse full repository
if [ -z "${full_repository}" ]; then
  full_repository=$(echo ${git_url} | awk -F"${git_server}" '{print $2}')
fi
owner=$(echo ${full_repository} | awk -F/ '{print $1}')
if [ -z "${repository}" ]; then
  repository=$(echo ${full_repository} | awk -F/ '{print $2}' | awk -F'.' '{print $1}')
fi

if [ -z "${target_full_repository}" ]; then
  target_owner="kiegroup"
  target_repository=${repository}
else
  target_owner=$(echo ${target_full_repository} | awk -F/ '{print $1}')
  if [ -z "${target_repository}" ]; then
    target_repository=$(echo ${target_full_repository} | awk -F/ '{print $2}' | awk -F'.' '{print $1}')
  fi
fi

echo "git_server...............${git_server}"
echo "owner....................${owner}"
echo "repository...............${repository}"
echo "branch...................${branch}"
echo "target_owner.............${target_owner}"
echo "target_repository........${target_repository}"
echo "target_branch............${target_branch}"
echo "pipelines_repo...........${pipelines_repo}"

current_repository="$(echo ${git_url}  | awk -F"${git_server}" '{print $2}' | awk -F'/' '{print $2}' | awk -F'.' '{print $1}')"
echo "Got current repository = ${current_repository}"

# retrieve repository
if [ "${repository}" != "${current_repository}" ]; then
  echo "Current repository is not the right one, checking out ${repository}"
  repo_tmp_dir=`mktemp -d`
  checkout_repository "${repository}" "${repo_tmp_dir}"
  echo "Going into ${repository} directory"
  cd ${repo_tmp_dir}
fi

if [ ! -z "${dsl_path}" ]; then
  echo "Moving to ${dsl_path}"
  cd ${dsl_path}
fi

if [ "${repository}" = 'kogito-pipelines' ]; then
  echo '----- Copying seed repo'
  mkdir -p ${TEMP_DIR}/dsl
  cp -r ${script_dir_path}/../../../dsl/seed ${TEMP_DIR}/dsl
else
  if [ -z ${pipelines_repo} ]; then
    checkout_repository 'kogito-pipelines' "${TEMP_DIR}"
  else
    echo '----- Copying given pipelines seed repo'
    mkdir -p ${TEMP_DIR}/dsl
    cp -r ${pipelines_repo}/dsl/seed ${TEMP_DIR}/dsl
  fi
fi

${TEMP_DIR}/dsl/seed/scripts/copy_jobs.sh

${TEMP_DIR}/dsl/seed/scripts/test_jobs.sh