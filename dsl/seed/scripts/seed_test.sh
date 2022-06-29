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
    echo '  -r FULL_REPOSITORY            Full name of the repository, aka `owner/repo`'
    echo '  -o OVERRIDE_REPOSITORY        Override the repository to check'

    echo '  -h BRANCH                     Git Branch to checkout'
    echo '  -t TARGET FULL_REPOSITORY     Full name of the PR target repository, aka `owner/repo`'
    echo '  -b TARGET_BRANCH              PR target branch'
    echo '  -r REPOSITORY                 Repository to test jobs from. Please reference the PATH if the jobs.groovy file is not present at the root of the project'
    echo '  -p PATH_TO_PIPELINES          Absolute path to local pipelines repository. Else the pipelines repository will be checked out'

    echo '  -e MAIN_CONFIG_FILE_REPO      Full name of the main config file repository, aka `owner/repo`. Default can be given via `DSL_DEFAULT_MAIN_CONFIG_FILE_REPO` env var'
    echo '  -f MAIN_CONFIG_FILE_REF       Main config file branch to the MAIN_CONFIG_REPO. Default can be given via `DSL_DEFAULT_MAIN_CONFIG_FILE_REF` env var'
    echo '  -g MAIN_CONFIG_FILE_PATH      Path to the test config file in MAIN_CONFIG_REPO. If no MAIN_CONFIG_REPO is given, then it will look locally. Default can be given via `DSL_DEFAULT_MAIN_CONFIG_FILE_PATH` env var'
    echo '  -c BRANCH_CONFIG_FILE_PATH    Local Path to the branch config file. `MAIN_CONFIG_*` will be ignored if that one is given. Default can be given via `DSL_DEFAULT_BRANCH_CONFIG_FILE_PATH` env var'
    echo '  -d BRANCH_CONFIG_BRANCH       Base branch to checkout the branch config. Default is `TARGET_BRANCH` or via `DSL_DEFAULT_BRANCH_CONFIG_BRANCH` env var'
    echo '  PATH                          Path to test'
    echo
}

checkout_repository() {
  local output_dir=$1

  echo "----- Cloning ${git_server}${owner}/${repository} repo on branch ${branch}"
  git clone --single-branch --depth 1 --branch ${branch} ${git_server}${owner}/${repository} ${output_dir}
  clone_status=$?

  set -e

  if [ "${clone_status}" != '0' ]; then
    if [ -z ${target_repository} ]; then
      echo 'No target repository given. Cannot retrieve repository ...'
      exit 1
    else 
      echo "[WARN] Error cloning ${git_server}${target_owner}/${target_repository} on branch ${target_branch}"
      echo "----- Cloning ${git_server}${target_owner}/${target_repository} on branch ${target_branch}"
      git clone --single-branch --depth 1 --branch ${target_branch} ${git_server}${target_owner}/${target_repository} ${output_dir}
    fi
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

main_config_file_repo="${DSL_DEFAULT_MAIN_CONFIG_FILE_REPO}"
main_config_file_ref="${DSL_DEFAULT_MAIN_CONFIG_FILE_REF}"
main_config_file_path="${DSL_DEFAULT_MAIN_CONFIG_FILE_PATH}"
branch_config_file_path="${DSL_DEFAULT_BRANCH_CONFIG_FILE_PATH}"
branch_config_branch="${DSL_DEFAULT_BRANCH_CONFIG_BRANCH}"

while getopts "r:o:h:t:b:p:e:f:g:c:d:h" i
do
    case "$i"
    in
        r) full_repository=${OPTARG} ;;
        o) repository=${OPTARG}; target_repository=${repository} ;;
        h) branch=${OPTARG} ;;
        t) target_full_repository=${OPTARG} ;;
        b) target_branch=${OPTARG} ;;
        p) pipelines_repo=${OPTARG} ;;
        e) main_config_file_repo=${OPTARG} ;;
        f) main_config_file_ref=${OPTARG} ;;
        g) main_config_file_path=${OPTARG} ;;
        c) branch_config_file_path=${OPTARG} ;;
        d) branch_config_branch=${OPTARG} ;;
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

if [ -z ${target_branch} ]; then
  branch_config_branch=${target_branch}
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
echo "main_config_file_repo....${main_config_file_repo}"
echo "main_config_file_ref.....${main_config_file_ref}"
echo "main_config_file_path....${main_config_file_path}"
echo "branch_config_branch.....${branch_config_branch}"
echo "branch_config_file_path..${branch_config_file_path}"

current_repository="$(echo ${git_url}  | awk -F"${git_server}" '{print $2}' | awk -F'/' '{print $2}' | awk -F'.' '{print $1}')"
echo "Got current repository = ${current_repository}"

# retrieve repository
if [ "${repository}" != "${current_repository}" ]; then
  echo "Current repository is not the right one, checking out ${repository}"
  repo_tmp_dir=`mktemp -d`
  checkout_repository "${repo_tmp_dir}"
  echo "Going into ${repository} directory"
  cd ${repo_tmp_dir}
fi

if [ ! -z "${dsl_path}" ]; then
  echo "Moving to ${dsl_path}"
  cd ${dsl_path}
fi

pipelines_repo_path=
if [ "${repository}" = 'kogito-pipelines' ]; then
  echo '----- Copying seed repo'
  mkdir -p ${TEMP_DIR}/dsl
  pipelines_repo_path="${script_dir_path}/../../.."
  cp -r ${pipelines_repo_path}/dsl/seed ${TEMP_DIR}/dsl
else
  if [ -z ${pipelines_repo} ]; then
    repository='kogito-pipelines'
    target_repository='kogito-pipelines'
    checkout_repository "${TEMP_DIR}"
    pipelines_repo_path="${TEMP_DIR}"
  else
    echo '----- Copying given pipelines seed repo'
    mkdir -p ${TEMP_DIR}/dsl
    pipelines_repo_path=${pipelines_repo}
    cp -r ${pipelines_repo_path}/dsl/seed ${TEMP_DIR}/dsl
  fi
fi

if [ -z "${branch_config_file_path}" ]; then
  echo '----- Retrieve branch config file'

  temp_dir=$(mktemp -d)
  yq_path="$(command -v yq)"
  if [[ ! $(command -v yq) ]]; then
    echo '--------- Installing yq locally'  
    yq_path="${temp_dir}/yq"
    wget https://github.com/mikefarah/yq/releases/download/v4.25.1/yq_linux_amd64.tar.gz -O - |  tar xz && ls -al && mv yq_linux_amd64 "${yq_path}"
    yq --version
  fi

  if [ ! -z ${main_config_file_repo} ]; then
    echo '--------- Retrieve main config file'
    main_config_repo_path="${temp_dir}/main_config_repo"
    main_config_file_path="${main_config_repo_path}/${main_config_file_path}"
    mkdir -p "${main_config_repo_path}"
    
    owner=$(echo ${main_config_file_repo} | awk -F/ '{print $1}')
    repository=$(echo ${main_config_file_repo} | awk -F/ '{print $2}' | awk -F'.' '{print $1}')
    branch=${main_config_file_ref}
    checkout_repository "${main_config_repo_path}"
  fi

  if [ ! -z ${main_config_file_path} ]; then
    echo '--------- Read main project'
    main_project=$(yq '.ecosystem.main_project' ${main_config_file_path})
    echo "Use main project ${main_project}"

    echo '--------- Setup branch name for reading config'
    branch_config_file_ref="${branch_config_branch}"
    if [[ ${branch_config_file_ref} =~ ^[0-9]+\.[0-9]+\.x$ ]]; then 
      branch_config_file_ref="${main_project}-${branch_config_file_ref}"; 
    fi
    echo "Use branch config ${branch_config_file_ref}"

    echo '--------- Retrieve branch config file information'
    branch_config_file_git_repository="$(yq '.git.branches[] | select(.name == "${branch_config_file_ref}") | .seed.config_file.git.repository' ${main_config_file_path})"
    if [ -z ${branch_config_file_git_repository} ]; then 
      branch_config_file_git_repository=$(yq '.seed.config_file.git.repository' ${main_config_file_path}); 
    fi
    echo "Use branch config file repo ${branch_config_file_git_repository}"

    branch_config_file_git_author="$(yq '.git.branches[] | select(.name == "${branch_config_file_ref}") | .seed.config_file.git.author.name' ${main_config_file_path})"
    if [ -z ${branch_config_file_git_author} ]; then 
      branch_config_file_git_author=$(yq '.seed.config_file.git.author.name' ${main_config_file_path}); 
    fi
    echo "Use branch config file author ${branch_config_file_git_author}"

    branch_config_file_path="$(yq '.git.branches[] | select(.name == "${branch_config_file_ref}") | .seed.config_file.path' ${main_config_file_path})"
    if [ -z ${branch_config_file_path} ]; then 
      branch_config_file_path=$(yq '.seed.config_file.path' ${main_config_file_path}); 
    fi
    echo "Use branch config file path ${branch_config_file_path}"

    echo '--------- Retrieve branch config file'
    branch_config_repo_path="${temp_dir}/branch_config_repo"
    branch_config_file_path="${branch_config_repo_path}/${branch_config_file_path}"
    mkdir -p "${branch_config_repo_path}"
    
    owner=${branch_config_file_git_author}
    repository=${branch_config_file_git_repository}
    branch=${branch_config_file_ref}
    checkout_repository "${branch_config_repo_path}"

    echo "Use config file ${branch_config_file_path}"
  fi

  # Old format of seed config/repo
  if [ -z ${branch_config_file_path} ]; then
    echo '--------- Set old format config file'
    if [ -f "${pipelines_repo_path}/dsl/config/branch.yaml" ]; then
      branch_config_file_path="${pipelines_repo_path}/dsl/config/branch.yaml"
    else
      branch_config_file_path="${pipelines_repo_path}/dsl/seed/config/branch.yaml"
    fi
  fi
fi

echo "----- Copy branch config file ${branch_config_file_path}"
cp "${branch_config_file_path}" ${TEMP_DIR}/dsl/seed/branch_config.yaml

${TEMP_DIR}/dsl/seed/scripts/copy_jobs.sh

${TEMP_DIR}/dsl/seed/scripts/test_jobs.sh