#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

### MANDATORY VARIABLES DEFINITION - UNCOMMENT FOR LOCAL USE     ###
### In Jenkins these VARIABLES are set as jenkins job parameters ###

#TARGET_VERSION="10.0.0"
#SOURCES_DEFAULT_BRANCH="main"
#GIT_AUTHOR="apache"
#
## Configuration in format "repository_name;branch(if-override-needed)"
## - eg.not all repositories have main branch
#SOURCES_REPOSITORIES="incubator-kie-drools
#incubator-kie-kogito-runtimes
#incubator-kie-kogito-apps
#incubator-kie-kogito-images
#incubator-kie-optaplanner
#incubator-kie-tools
#incubator-kie-sandbox-quarkus-accelerator"

function zip_sources() {
  SOURCES_DIRECTORY_NAME="sources"
  OUTPUT_DIR="sources-out"

  if [ -d ${SOURCES_DIRECTORY_NAME} ]; then
    echo "Directory '${SOURCES_DIRECTORY_NAME}' already exists. Deleting..."
    rm -rf ${SOURCES_DIRECTORY_NAME}
  fi

  if [ -d ${OUTPUT_DIR} ]; then
    echo "Directory '${OUTPUT_DIR}' already exists. Deleting..."
    rm -rf ${OUTPUT_DIR}
  fi

  while read line; do
    BRANCH=${SOURCES_DEFAULT_BRANCH}
    #get rid of carriage return character if present
    line="$(echo $line | sed 's#\r##g')"

    #Clone
    echo "Clone $( echo $line | awk -F';' '{ print $1 }' | sed 's\incubator-\\g' )"
    REPO_NAME=$( echo $line | awk -F';' '{print $1 }' )
    REPO_DIRECTORY=${SOURCES_DIRECTORY_NAME}/${REPO_NAME}
    #Leaving branch specifying functionality here in case we need to specify branch for any repo in the future
    REPO_BRANCH=$( echo $line | awk -F';' '{print $2}' )
    if [[ ! -z ${REPO_BRANCH} ]]; then
      BRANCH=$REPO_BRANCH
    fi
    git clone --branch ${BRANCH} --depth 1 "https://github.com/${GIT_AUTHOR}/${REPO_NAME}.git" ${REPO_DIRECTORY}
    STATE=$?
    if [[ ${STATE} != 0 ]]; then
      echo "Cloning ${REPO_NAME} was NOT successful. Failing."
      exit 1
    fi

    #Remove unnecessary dirs
    pushd $REPO_DIRECTORY
    CURRENT_DIRECTORY=$(pwd)
    echo "Current directory is ${CURRENT_DIRECTORY}"
    echo "Before .git removal"
    ls -lha
    echo "Searching for .git directory"
    if [[ -d '.git' ]]; then
        echo ".git directory found, deleting..."
        rm -rf ".git"
    fi
    echo "After .git removal"
    ls -lha
    popd

  done <<< $SOURCES_REPOSITORIES

  #Add LICENSE, NOTICE and DISCLAIMER files to the root folder of the zip file
  echo "Adding LICENSE, NOTICE and DISCLAIMER files to the zip file"
  cp ./tools/zip-sources-files/{LICENSE,NOTICE,DISCLAIMER-WIP} ${SOURCES_DIRECTORY_NAME}

  #Creating ZIP
  pushd ${SOURCES_DIRECTORY_NAME}
  ZIP_FILE_NAME=$1
  echo "Creating ${ZIP_FILE_NAME}"
  mkdir "../${OUTPUT_DIR}"
  zip -ry "../${OUTPUT_DIR}/${ZIP_FILE_NAME}" *
  if [[ ! -f "../${OUTPUT_DIR}/${ZIP_FILE_NAME}" ]]; then
    echo "${ZIP_FILE_NAME} has not been created."
    exit 2
  fi
  ls ../${OUTPUT_DIR}/${ZIP_FILE_NAME}
  popd
}

zip_sources $1
