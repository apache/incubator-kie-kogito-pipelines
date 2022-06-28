#!/bin/bash

SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

export DSL_DEFAULT_MAIN_CONFIG_FILE_REPO=kiegroup/kogito-pipelines
export DSL_DEFAULT_MAIN_CONFIG_FILE_REF=main
export DSL_DEFAULT_MAIN_CONFIG_FILE_PATH=.ci/jenkins/config/main.yaml

${SCRIPT_DIR_PATH}/../../../dsl/seed/scripts/seed_test.sh $@ 