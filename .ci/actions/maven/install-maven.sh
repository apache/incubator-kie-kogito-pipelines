#!/bin/sh
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

set -e

install_path=$1
maven_version=$2

if [ -z "${maven_version}" ]; then
  maven_version=$(curl https://maven.apache.org/download.cgi --silent | grep "Downloading Apache Maven " | grep -oE '[0-9].[0-9]+.[0-9]+')
fi
maven_file="apache-maven-${maven_version}-bin.tar.gz"
download_url="https://archive.apache.org/dist/maven/maven-3/${maven_version}/binaries/${maven_file}"
download_path="${HOME}/.maven/download/"

echo "---> Maven version to install is ${maven_version}"

if [ -e "${download_path}/${maven_file}" ]; then
  echo "---> Maven ${maven_version} already exists in '${download_path}', skipping downloading"
else
  mkdir -p "${download_path}"
  cd "${download_path}"
  echo "---> Downloading maven ${maven_version} to ${download_path}"
  curl -LO "${download_url}"
  cd -
fi

if [ -z "${install_path}" ]; then
  install_path="${HOME}/runner/mvn"
fi

echo "---> Ensuring maven installation at ${install_path}"

mkdir -p "${install_path}"
tar -xf "${download_path}/${maven_file}" --strip-components=1 -C ${install_path}

sh "${install_path}/bin/mvn" --version