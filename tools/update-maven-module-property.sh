#!/usr/bin/env bash
set -eo pipefail
 
# kogito-runtimes, kogito-examples
PROPERTY=$1
VALUE=$2
MODULE=$3

if [ -z "${PROPERTY}" ]; then
  echo "Please provide which property to update as first argument"
  echo 1
fi

if [ -z "${VALUE}" ]; then
  echo "Please provide new value for the property as second argument"
  echo 1
fi

mvnArgs="versions:set-property \
      -Dproperty=${PROPERTY} \
      -DnewVersion=${VALUE} \
      -DgenerateBackupPoms=false"
if [ ! -z "${MODULE}" ]; then
  mvnArgs="-pl :${MODULE} ${mvnArgs}"
fi

mvn ${mvnArgs}