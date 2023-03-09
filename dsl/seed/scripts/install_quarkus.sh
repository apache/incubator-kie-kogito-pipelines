#!/bin/bash
set -euo pipefail

mvn_cmd="mvn ${BUILD_MVN_OPTS:-} ${BUILD_MVN_OPTS_QUARKUS_UPDATE:-}"

quarkus_version="${QUARKUS_VERSION:-}"
quarkus_branch="${QUARKUS_BRANCH:-${quarkus_version}}"

is_snapshot=false

if [ ! -z "${quarkus_version}" ]; then
    if [[ "${quarkus_version}" == *-SNAPSHOT ]]; then
        echo "Given quarkus version (${quarkus_version}) is a snapshot version. It will be built from quarkus repository."
        is_snapshot=true
    else
        echo "Given quarkus version (${quarkus_version}) is a release version. It will be downloaded directly."
        exit 0
    fi
else
    echo "No quarkus version given. Will get quarkus from the 'QUARKUS_BRANCH'"
fi

if [ -z "${quarkus_branch}" ]; then
    echo "Please provide the 'QUARKUS_BRANCH' and/or a 'QUARKUS_VERSION' version..."
    exit 1
fi

set -x

tmp_dir=/tmp/quarkus/"${quarkus_branch}"
mkdir -p "${tmp_dir}"
cd "${tmp_dir}"

if [ -f pom.xml ]; then
    echo "Quarkus already checked out. Fetching origin and rebuilding."
    git fetch origin
    git pull origin "${quarkus_branch}"
else
    echo "Checking out Quarkus into ${tmp_dir}"
    rm -rf "${tmp_dir}"
    mkdir -p "${tmp_dir}"
    git clone --depth 1 --single-branch --branch ${quarkus_branch} https://github.com/quarkusio/quarkus .
fi

# Read version
quarkus_version=$(mvn -q -Dexpression=project.version -DforceStdout help:evaluate)
echo "Got quarkus version ${quarkus_version} from repository"

# Install project
${mvn_cmd} clean install -Dquickly

export QUARKUS_VERSION="${quarkus_version}"