#!/bin/bash

### This script should be launch by any dependent DSL scripts generation repo for local testing of changes
### Ideal dependent repo should have a `local_test.sh` script located in `.ci/jenkins/dsl` folder
### With that implementation:
# #!/bin/bash -e
# file=$(mktemp)
# # For more usage of the script, use ./test.sh -h
# curl -o ${file} https://raw.githubusercontent.com/kiegroup/kogito-pipelines/main/dsl/seed/scripts/seed_test.sh
# chmod u+x ${file}
# ${file} $@

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
GITHUB_SERVER='github.com'
declare -A checkout_map

usage() {
    echo "Usage: $(basename $0)"
    echo
    echo 'This script is using environment variables as configuration.'
    echo 'Here is a list of those:'
    echo
    echo '  Main config file configuration:'
    echo '    DSL_DEFAULT_MAIN_CONFIG_FILE_REPO             Main config file repository (owner/repo)'
    echo '    DSL_DEFAULT_MAIN_CONFIG_FILE_REF              Main config file reference'
    echo '    DSL_DEFAULT_MAIN_CONFIG_FILE_PATH             Main config file path. Default is `dsl/config/main.yaml`'
    echo '    DSL_DEFAULT_FALLBACK_MAIN_CONFIG_FILE_REPO    Fallback main config repository (owner/repo). Default is `kiegroup/kogito-pipelines`'
    echo '    DSL_DEFAULT_FALLBACK_MAIN_CONFIG_FILE_REF     Fallback main config reference. Default is `main`'
    echo '    DSL_DEFAULT_MAIN_CONFIG_FILE_LOCAL_PATH       Main config file local path. If set, the other `DSL_DEFAULT_MAIN_CONFIG_FILE_*` envs will be ignored'
    echo
    echo '  Branch config file configuration:'
    echo '    DSL_DEFAULT_BRANCH_CONFIG_NAME                DSL branch config name. Corresponding to the match in the main config branch'
    echo '    DSL_DEFAULT_BRANCH_CONFIG_FILE_REPO           Branch config file repository (owner/repo). Override the branch config repo. Else it is read from main config.'
    echo '    DSL_DEFAULT_BRANCH_CONFIG_FILE_REF            Branch config file reference. Override the branch config ref. Else it is read from main config.'
    echo '    DSL_DEFAULT_BRANCH_CONFIG_FILE_PATH           Branch config file path. Override the branch config path. Else it is read from main config.'
    echo '    DSL_DEFAULT_BRANCH_CONFIG_FILE_LOCAL_PATH     DSL local branch config file path. If that is set, the `DSL_DEFAULT_MAIN_CONFIG_*` and other `DSL_DEFAULT_BRANCH_CONFIG_*` envs are ignored'
    echo 
    echo '  Seed repository configuration:'
    echo '    DSL_DEFAULT_SEED_REPO                         DSL seed repository (owner/repo). Else it will be calculated from the test repository.'
    echo '    DSL_DEFAULT_SEED_REF                          DSL seed reference. Else it will be calculated from the branch config'
    echo '    DSL_DEFAULT_SEED_REPO_LOCAL_PATH              DSL seed repository local path. If set, the other `DSL_DEFAULT_SEED_*` envs will be ignored'
    echo '    DSL_DEFAULT_FALLBACK_SEED_REPO                Fallback seed repository (owner/repo). Default is `kiegroup/kogito-pipelines`'
    echo '    DSL_DEFAULT_FALLBACK_SEED_REF                 Fallback seed reference. Default is `main`'
    echo
    echo '   Test repository configuration:'
    echo '    DSL_DEFAULT_TEST_REPO                         Repository to test. Default will be handled from current folder'
    echo '    DSL_DEFAULT_TEST_REF                          Repository reference to test. Default will be guessed from current folder'
    echo '    DSL_DEFAULT_TEST_JOBS_PATH                    Path on repository where to find the jobs file. Default to `.ci/jenkins/dsl`'
    echo
    echo '  Current repository information:'
    echo '    DSL_DEFAULT_CURRENT_REPOSITORY                Force the current repository (owner/repo). Useful if the remote `origin` is not the needed one.'
    echo '    DSL_DEFAULT_CURRENT_REF                       Force the current ref. Useful if the checkout was not done on a branch.'
    echo
    echo '  Other configuration:'
    echo '    DSL_DEFAULT_PIPELINES_TEST_DIR                Where will be the pipelines test dir'
    echo
}

# checkout_repository $OUTPUT_DIR $OWNER $REPOSITORY $BRANCH $TARGER_OWNER $TARGET_REPOSITORY $TARGET_BRANCH
checkout_repository() {
  local output_dir=$1

  local repository=$2
  local branch=$3

  local target_repository=$4
  local target_branch=$5

  echo "Cloning ${git_server_url}${repository} repo on branch ${branch} in directory ${output_dir}"
  git clone --single-branch --depth 1 --branch ${branch} ${git_server_url}${repository} ${output_dir} &> /dev/null
  clone_status=$?

  set -e

  if [ "${clone_status}" != '0' ]; then
    echo "[WARN] Error cloning ${git_server_url}${repository} on branch ${branch}"
    if [ -z ${target_repository} ]; then
      echo 'No target repository given. Cannot retrieve repository ...'
      exit 1
    else 
      echo "Cloning ${git_server_url}${target_repository} on branch ${target_branch} in directory ${output_dir}"
      git clone --single-branch --depth 1 --branch ${target_branch} ${git_server_url}${target_repository} ${output_dir} &> /dev/null
    fi
  else
    echo "Clone succeeded"
  fi

  checkout_map["${repository}/${branch}"]="${output_dir}"

  set +e
}

installYqIfNeeded() {
  yq_path="$(command -v yq)"
  if [[ ! $(command -v yq) ]]; then
    echo 'yq command cannot be found ... Installing it locally'
    yq_path="$(mktemp -d)/yq"
    wget https://github.com/mikefarah/yq/releases/download/v4.25.1/yq_linux_amd64.tar.gz -O - |  tar xz && ls -al && mv yq_linux_amd64 "${yq_path}"
    yq --version

    export PATH="${PATH}:${yq_path}"
  fi
}

while getopts "h" i
do
    case "$i"
    in
        h) usage; exit 0 ;;
        \?) usage; exit 1 ;;
    esac
done
shift "$((OPTIND-1))"

echo
echo "--------- Start configuration ---------"

pipelines_final_dir=${DSL_DEFAULT_PIPELINES_TEST_DIR:-$(mktemp -d)}

git_url=$(git remote -v | grep origin | awk -F' ' '{print $2}' | head -n 1)
if [ -z "${git_url}" ]; then
  echo "Script must be executed in a Git repository for this script to run currently"
  exit 1
fi

git_server_url=
if [[ "${git_url}" = https://* ]]; then
  git_server_url="https://${GITHUB_SERVER}/"
elif [[ "${git_url}" = git@* ]]; then 
  git_server_url="git@${GITHUB_SERVER}:"
else
  echo "Unknown protocol for url ${git_url}"
  exit 1
fi

current_repository="${DSL_DEFAULT_CURRENT_REPOSITORY:-$(echo ${git_url} | awk -F"${git_server_url}" '{print $2}' | awk -F. '{print $1}')}"
current_ref="${DSL_DEFAULT_CURRENT_REF:-$(git branch --show-current)}"

echo "-----------------------------------------------------------------"
echo "-- GLOBAL CONFIGURATION"
echo '--'
echo "-- Git info"
echo "--   git_server_url.................. ${git_server_url}"
echo "--   git_url......................... ${git_url}"
echo '--'
echo "-- Current Repository"
echo "--   current_repository.............. ${current_repository}"
echo "--   current_ref..................... ${current_ref}"
echo '--'
echo "-- Other info"
echo "--   pipelines_final_dir..............${pipelines_final_dir}"
echo "-----------------------------------------------------------------"

echo "Register current repository into the checkout map"
checkout_map["${current_repository}/${current_ref}"]="$(pwd)"

installYqIfNeeded

##############################################################################################################
##  Main / Branch config files
##############################################################################################################

echo
echo
echo "--------- Main / Branch config files ---------"

main_config_file_repo=${DSL_DEFAULT_MAIN_CONFIG_FILE_REPO}
main_config_file_ref=${DSL_DEFAULT_MAIN_CONFIG_FILE_REF}
main_config_file_path=${DSL_DEFAULT_MAIN_CONFIG_FILE_PATH:-'dsl/config/main.yaml'}
main_config_file_local_path=${DSL_DEFAULT_MAIN_CONFIG_FILE_LOCAL_PATH}

branch_config_name=${DSL_DEFAULT_BRANCH_CONFIG_NAME}
branch_config_file_repo=${DSL_DEFAULT_BRANCH_CONFIG_FILE_REPO}
branch_config_file_ref=${DSL_DEFAULT_BRANCH_CONFIG_FILE_REF}
branch_config_file_path=${DSL_DEFAULT_BRANCH_CONFIG_FILE_PATH}
branch_config_file_local_path=${DSL_DEFAULT_BRANCH_CONFIG_FILE_LOCAL_PATH:-''}

fallback_main_config_file_repo=${DSL_DEFAULT_FALLBACK_MAIN_CONFIG_FILE_REPO:-'kiegroup/kogito-pipelines'}
fallback_main_config_file_ref=${DSL_DEFAULT_FALLBACK_MAIN_CONFIG_FILE_REF:-'main'}
fallback_branch_config_file_repo=${DSL_DEFAULT_FALLBACK_BRANCH_CONFIG_FILE_REPO:-'kiegroup/kogito-pipelines'}
fallback_branch_config_file_ref=${DSL_DEFAULT_FALLBACK_BRANCH_CONFIG_FILE_REF:-'main'}
fallback_branch_config_name="${fallback_main_config_file_ref}"

if [ -z "${main_config_file_repo}" ]; then
  main_config_file_owner="$(echo "${DSL_DEFAULT_TEST_REPO:-${current_repository}}" | awk -F/ '{print $1}')"
  main_config_file_repo_name="$(echo "${fallback_main_config_file_repo}" | awk -F/ '{print $2}')"
  main_config_file_repo="${main_config_file_owner}/${main_config_file_repo_name}"
fi
if [ -z "${main_config_file_ref}" ]; then
  main_config_file_ref="${DSL_DEFAULT_TEST_REF:-${current_ref}}"
fi
if [ -z "${branch_config_name}" ]; then
  branch_config_name="${DSL_DEFAULT_TEST_REF:-${current_ref}}"
fi

echo "-----------------------------------------------------------------"
echo "-- MAIN CONFIG FILE CONFIGURATION"
echo '--'
echo "-- main_config_file_repo........... ${main_config_file_repo}"
echo "-- main_config_file_ref............ ${main_config_file_ref}"
echo "-- main_config_file_path........... ${main_config_file_path}"
echo "-- main_config_file_local_path..... ${main_config_file_local_path}"
echo "-- fallback_main_config_file_repo.. ${fallback_main_config_file_repo}"
echo "-- fallback_main_config_file_ref... ${fallback_main_config_file_ref}"
echo "-----------------------------------------------------------------"
echo "-- BRANCH CONFIG FILE CONFIGURATION"
echo '--'
echo "-- branch_config_name.............. ${branch_config_name}"
echo "-- branch_config_file_local_path... ${branch_config_file_local_path}"
echo "-- fallback_branch_config_name..... ${fallback_branch_config_name}"
echo "-----------------------------------------------------------------"

if [ -z "${branch_config_file_local_path}" ]; then
  if [ -z "${main_config_file_local_path}" ]; then
    echo "Checkout main config file"
    main_config_repo_path=${checkout_map["${main_config_file_repo}/${main_config_file_ref}"]}
    if [ -z "${main_config_repo_path}" ]; then
      main_config_repo_path=$(mktemp -d)
      mkdir -p "${main_config_repo_path}"
      checkout_repository "${main_config_repo_path}" "${main_config_file_repo}" "${main_config_file_ref}" "${fallback_main_config_file_repo}" "${fallback_main_config_file_ref}"
    else
      echo "Main config file repository (${main_config_file_repo}/${main_config_file_ref}) already checked out. Using it ..."
    fi
    main_config_file_path="${main_config_repo_path}/${main_config_file_path}"
  else
    main_config_file_path="${main_config_file_local_path}"
    echo "Main config file local path has been set.\n\n Ignoring the retrieval of main/branch config..."
  fi
  echo "Use main config file ${main_config_file_path}"

  echo 'Read main project'
  main_project=$(yq '.ecosystem.main_project' ${main_config_file_path})
  echo "Use main project ${main_project}"

  echo 'Read branch config file from repository'

  echo "Check branch config name ${branch_config_name} is existing"
  yq -e ".git.branches[] | select(.name == \"${branch_config_name}\")" ${main_config_file_path} &> /dev/null
  if [ $? != 0 ]; then 
    branch_config_name="${fallback_branch_config_name}"
    echo "Branch config name cannot be found. Fallback to default branch config name: ${fallback_branch_config_name}"
  fi

  if [ -z "${branch_config_file_repo}" ]; then
    branch_config_file_owner="$(echo "${DSL_DEFAULT_TEST_REPO:-${current_repository}}" | awk -F/ '{print $1}')"
    branch_config_file_repo_name="$(echo "${fallback_branch_config_file_repo}" | awk -F/ '{print $2}')"
    branch_config_file_repo="${branch_config_file_owner}/${branch_config_file_repo_name}"
  fi
  if [ -z "${branch_config_file_ref}" ]; then
    branch_config_file_ref="${DSL_DEFAULT_TEST_REF:-${current_ref}}"
  fi

  echo "Retrieve branch config file information with branch config name ${branch_config_name}"
  fallback_branch_config_file_ref="$(yq -e ".git.branches[] | select(.name == \"${branch_config_name}\") | .seed.config_file.git.branch" ${main_config_file_path} 2> /dev/null)"
  if [ $? != 0 ]; then 
    fallback_branch_config_file_ref="${branch_config_name}"
  fi

  branch_config_file_git_repository="$(yq -e ".git.branches[] | select(.name == \"${branch_config_name}\") | .seed.config_file.git.repository" ${main_config_file_path} 2> /dev/null)"
  if [ $? != 0 ]; then
    branch_config_file_git_repository="$(yq '.seed.config_file.git.repository' ${main_config_file_path})"
  fi
  branch_config_file_git_author="$(yq -e ".git.branches[] | select(.name == \"${branch_config_name}\") | .seed.config_file.git.author.name" ${main_config_file_path} 2> /dev/null)"
  if [ $? != 0 ]; then 
    branch_config_file_git_author="$(yq '.seed.config_file.git.author.name' ${main_config_file_path})"
  fi
  fallback_branch_config_file_repo="${branch_config_file_git_author}/${branch_config_file_git_repository}"

  if [ -z "${branch_config_file_path}" ]; then
    branch_config_file_path="$(yq -e ".git.branches[] | select(.name == \"${branch_config_name}\") | .seed.config_file.path" ${main_config_file_path} 2> /dev/null)"
    if [ $? != 0 ]; then 
      branch_config_file_path="$(yq '.seed.config_file.path' ${main_config_file_path})"
    fi
  fi

  echo "-----------------------------------------------------------------"
  echo "-- BRANCH CONFIG FILE CONFIGURATION"
  echo '--'
  echo "-- branch_config_file_repo.............. ${branch_config_file_repo}"
  echo "-- branch_config_file_ref............... ${branch_config_file_ref}"
  echo "-- branch_config_file_path.............. ${branch_config_file_path}"
  echo "-- fallback_branch_config_file_repo..... ${fallback_branch_config_file_repo}"
  echo "-- fallback_branch_config_file_ref...... ${fallback_branch_config_file_ref}"
  echo "-----------------------------------------------------------------"

  branch_config_repo_path=${checkout_map["${branch_config_file_repo}/${branch_config_file_ref}"]}
  if [ -z "${branch_config_repo_path}" ]; then
    echo 'Retrieve branch config file'
    branch_config_repo_path=$(mktemp -d)
    mkdir -p "${branch_config_repo_path}"
    checkout_repository "${branch_config_repo_path}" "${branch_config_file_repo}" "${branch_config_file_ref}" "${fallback_branch_config_file_repo}" "${fallback_branch_config_file_ref}"
  else
    echo "Branch config file repository (${branch_config_file_repo}/${branch_config_file_ref}) already checked out. Using it ..."
  fi
  branch_config_file_path="${branch_config_repo_path}/${branch_config_file_path}"
else
  branch_config_file_path="${branch_config_file_local_path}"
  echo "Branch config file local path has been set.\n\n Ignoring the retrieval of main/branch config..."
fi
echo "Use branch config file ${branch_config_file_path}"

##############################################################################################################
##  Seed repository
##############################################################################################################

echo
echo
echo "--------- Seed repository ---------"

seed_repo=${DSL_DEFAULT_SEED_REPO}
seed_ref=${DSL_DEFAULT_SEED_REF}
seed_local_path=${DSL_DEFAULT_SEED_REPO_LOCAL_PATH}

fallback_seed_repo=${DSL_DEFAULT_FALLBACK_SEED_REPO:-'kiegroup/kogito-pipelines'}
fallback_seed_ref=${DSL_DEFAULT_FALLBACK_SEED_REF}
if [ -z ${fallback_seed_ref} ]; then
  fallback_seed_ref="$(yq -e ".git.branches[] | select(.name == \"${branch_config_name}\") | .seed.branch" ${main_config_file_path} 2> /dev/null)"
  if [ $? != 0 ]; then 
    fallback_seed_ref='main'
  fi
fi

if [ -z "${seed_repo}" ]; then
  seed_owner="$(echo "${DSL_DEFAULT_TEST_REPO:-${current_repository}}" | awk -F/ '{print $1}')"
  seed_repo_name="$(echo "${fallback_seed_repo}" | awk -F/ '{print $2}')"
  seed_repo="${seed_owner}/${seed_repo_name}"
fi
if [ -z "${seed_ref}" ]; then
  seed_ref="${DSL_DEFAULT_TEST_REF:-${current_ref}}"
fi

echo "-----------------------------------------------------------------"
echo "-- SEED CONFIGURATION"
echo '--'
echo "-- seed_repo....................... ${seed_repo}"
echo "-- seed_ref........................ ${seed_ref}"
echo "-- seed_local_path................. ${seed_local_path}"
echo "-- fallback_seed_repo.............. ${fallback_seed_repo}"
echo "-- fallback_seed_ref............... ${fallback_seed_ref}"
echo "-----------------------------------------------------------------"

seed_repo_path=
if [ -z "${seed_local_path}" ]; then
  seed_repo_path=${checkout_map["${seed_repo}/${seed_ref}"]}
  if [ -z "${seed_repo_path}" ]; then
    echo 'Retrieve seed repository'
    seed_repo_path=$(mktemp -d)
    mkdir -p "${seed_repo_path}"
    checkout_repository "${seed_repo_path}" "${seed_repo}" "${seed_ref}" "${fallback_seed_repo}" "${fallback_seed_ref}"
  else
    echo "Seed repository (${seed_repo}/${seed_ref}) already checked out. Using it ..."
  fi
else
  echo "Seed repository local path has been set.\n\n Copying to final folder..."
  seed_repo_path="${seed_local_path}"
fi
echo "Copying seed repo to pipelines final directory ${pipelines_final_dir}"
mkdir -p ${pipelines_final_dir}/dsl
cp -r ${seed_repo_path}/dsl/seed ${pipelines_final_dir}/dsl

echo "Copy branch config file ${branch_config_file_path} to pipelines final directory ${pipelines_final_dir}"
cp "${branch_config_file_path}" ${pipelines_final_dir}/dsl/seed/branch_config.yaml

##############################################################################################################
##  Test repository
##############################################################################################################

echo
echo
echo "--------- Test repository ---------"

test_repository=${DSL_DEFAULT_TEST_REPO}
test_ref=${DSL_DEFAULT_TEST_REF}
test_jobs_path=${DSL_DEFAULT_TEST_JOBS_PATH:-'.ci/jenkins/dsl'}

if [ -z "${test_repository}" ]; then
  test_repository="${current_repository}"
  test_ref="${current_ref}"
  echo "Got test repo = ${test_repository}"
fi
if [ -z "${test_ref}" ]; then
  test_ref="${current_ref}"
  echo "Got test branch = ${test_ref}"
fi

test_repo_name="$(echo "${test_repository}" | awk -F/ '{print $2}')"
fallback_test_repository="$(yq -e ".repositories[] | select(.name == \"${test_repo_name}\") | .git.author.name" ${branch_config_file_path} 2> /dev/null)/${test_repo_name}"
if [ $? != 0 ]; then 
  fallback_test_repository="$(yq -e ".git.author.name" ${branch_config_file_path})/${test_repo_name}"
fi
fallback_test_ref="$(yq -e ".repositories[] | select(.name == \"${test_repo_name}\") | .branch" ${branch_config_file_path} 2> /dev/null)"
if [ $? != 0 ]; then 
  fallback_test_ref="${fallback_branch_config_file_ref}"
fi

echo "-----------------------------------------------------------------"
echo "-- TEST REPOSITORY CONFIGURATION"
echo '--'
echo "-- test_repository................. ${test_repository}"
echo "-- test_ref........................ ${test_ref}"
echo "-- test_jobs_path.................. ${test_jobs_path}"
echo "-- fallback_test_repository........ ${fallback_test_repository}"
echo "-- fallback_test_ref............... ${fallback_test_ref}"
echo "-----------------------------------------------------------------"

if [ "${test_repository}/${test_ref}" != "${current_repository}/${current_ref}" ]; then
  echo "Retrieve repository to test ${test_repository} on ${test_ref}"
  repository_to_test_path=$(mktemp -d)
  checkout_repository "${repository_to_test_path}" "${test_repository}" "${test_ref}" "${fallback_test_repository}" "${fallback_test_ref}"
else
  repository_to_test_path=$(pwd)
fi
echo "Use test repository ${repository_to_test_path}"

##############################################################################################################
##  Finalize
##############################################################################################################

echo
echo
echo "--------- Finalize ---------"

seed_dir="${pipelines_final_dir}/dsl/seed"

echo "Moving to jobs path for testing"
cd "${repository_to_test_path}/${test_jobs_path}"

echo "Copying jobs.groovy for testing from $(pwd)"
cp jobs.groovy ${seed_dir}/jobs/

echo "Test jobs in ${seed_dir}"
cd ${seed_dir}
echo "Results will be available here after execution: $(pwd)/build/reports/tests/test/classes/org.kie.jenkins.jobdsl.JobScriptsSpec.html"
./gradlew clean test