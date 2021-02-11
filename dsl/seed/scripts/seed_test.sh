#!/bin/bash -e

### This script should be launch by any dependent DSL scripts generation repo for local testing of changes
### Ideal dependent repo should have a `local_test.sh` script located in `.jenkins/dsl` folder
### With that implementation:
##!/bin/bash -e
# TEMP_DIR=`mktemp -d`
#
# author=$1
# branch=$2
# 
# if [ -z $author ]; then
#   author='kiegroup'
# fi
# 
# if [ -z $branch ]; then
#   branch='master'
# fi
# 
# echo '----- Cloning main dsl pipelines repo'
# git clone --single-branch --branch $branch https://github.com/${author}/kogito-pipelines.git $TEMP_DIR
# 
# echo '----- Launching seed tests'
# ${TEMP_DIR}/jenkins/dsl/seed/scripts/seed_test.sh ${TEMP_DIR}

script_dir_path=`dirname "${BASH_SOURCE[0]}"`
seed_dir="${script_dir_path}/.."

${script_dir_path}/copy_jobs.sh

${script_dir_path}/test.sh