#!/bin/bash -e

### This script should be launch by any dependent DSL scripts generation repo for local testing of changes
### Ideal dependent repo should have a `local_test.sh` script located in `.ci/jenkins/dsl` folder
### With that implementation:
# #!/bin/bash -e

# file=$(mktemp)
# curl -o ${file} https://raw.githubusercontent.com/kiegroup/kogito-pipelines/main/dsl/seed/scripts/seed_test.sh
# chmod u+x ${file}
# ${file} $1

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

TEMP_DIR=`mktemp -d`
GIT_SERVER='github.com'

usage() {
    echo "Usage: $(basename $0) [-b BRANCH] [-o GIT_OWNER] [PATH]"
    echo
    echo 'Options: (only used for any repository different from current pipelines)'
    echo '  -b BRANCH            Git Branch to checkout'
    echo '  -o OWNER             Git Owner to checkout'
    echo '  -t TARGET BRANCH     PR target branch'
    echo '  -a TARGET_AUTHOR     PR target author'
    echo '  -r REPOSITORY        Repository to test jobs from. Please reference the PATH if the jobs.groovy file is not present at the root of the project'
    echo '  -p PATH_TO_PIPELINES Absolute path to local pipelines repository. Else the pipelines repository will be checked out'
    echo '  PATH                 Path to test'
    echo
}

retrieve_current_repository() {
  sub_git_url=$(echo ${git_url} | awk -F"${GIT_SERVER}" '{print $2}')
  [[ ${sub_git_url} = :* ]] && sub_git_url="${sub_git_url:1}"
  [[ ${sub_git_url} = /* ]] && sub_git_url="${sub_git_url:1}"
  local current_repository=$(echo ${sub_git_url} | awk -F'/' '{print $2}' | awk -F'.' '{print $1}')
  echo "${current_repository}"
}

checkout_repository() {
  local repository=$1
  local output_dir=$2

  echo "----- Cloning ${git_server}${owner}/${repository}.git repo on branch ${branch}"
  git clone --single-branch --depth 1 --branch ${branch} ${git_server}${owner}/${repository}.git ${output_dir}
  clone_status=$?

  set -e

  if [ "${clone_status}" != '0' ]; then
    echo "[WARN] Error cloning ${repository} repo from ${target_author} on branch ${target_branch}"
    echo "----- Cloning ${repository} repo from ${target_author} on branch ${target_branch}"
    git clone --single-branch --depth 1 --branch ${target_branch} ${git_server}${target_author}/${repository}.git ${output_dir}
  fi
}

branch=
owner=
target_branch='main'
target_author='kiegroup'
repository=
pipelines_repo=

while getopts "b:o:t:a:r:p:h" i
do
    case "$i"
    in
        b) branch=${OPTARG} ;;
        o) owner=${OPTARG} ;;
        t) target_branch=${OPTARG} ;;
        o) target_author=${OPTARG} ;;
        r) repository=${OPTARG} ;;
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

# retrieve owner
if [ -z "${owner}" ]; then
  owner=$(echo ${git_url} | awk -F"${git_server}" '{print $2}' | awk -F'/' '{print $1}')
  echo "Got current owner = ${owner}"
fi

echo "git_server...............${git_server}"
echo "branch...................${branch}"
echo "owner....................${owner}"
echo "target_branch............${target_branch}"
echo "target_author............${target_author}"
echo "repository...............${repository}"
echo "pipelines_repo...........${pipelines_repo}"

# retrieve repository
if [ -z ${repository} ]; then
  repository="$(retrieve_current_repository)"
  echo "Got current repository = ${repository}"
else
  repo_tmp_dir=`mktemp -d`
  checkout_repository "${repository}" "${repo_tmp_dir}"
  echo "Going into ${repository} directory"
  cd ${repo_tmp_dir}
fi

if [ ! -z "${dsl_path}" ]; then
  cd ${dsl_path}
fi

if [ "${repository}" = 'kogito-pipelines' ]; then
  echo '----- Copying seed repo'
  mkdir -p ${TEMP_DIR}/dsl
  cp -r ${SCRIPT_DIR_PATH}/../../../dsl/seed ${TEMP_DIR}/dsl
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