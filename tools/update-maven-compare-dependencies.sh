#!/usr/bin/env bash
set -eo pipefail
 
REMOTE_POM=$1
REMOTE_POM_VERSION=$2
MODULE=$3

if [ -z "${REMOTE_POM}" ]; then
  echo "Please provide a remote pom to compare with (groupId:artifactId)"
  echo 1
fi

if [ -z "${REMOTE_POM_VERSION}" ]; then
  echo "Please provide the version of the remote pom"
  echo 1
fi

mvnArgs="versions:compare-dependencies \
      -DremotePom=${REMOTE_POM}:${REMOTE_POM_VERSION} \
      -DupdatePropertyVersions=true \
      -DupdateDependencies=true \
      -DgenerateBackupPoms=false"
if [ ! -z "${MODULE}" ]; then
  mvnArgs="-pl :${MODULE} ${mvnArgs}"
fi

mvn ${mvnArgs}