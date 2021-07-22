#!/usr/bin/env bash
set -eo pipefail
 
# kogito-runtimes, optaplanner. kogito-examples or optaplanner-quickstarts
REGEX=$1
VALUE=$2

if [ -z "${REGEX}" ]; then
  echo "Please provide which a regex to update"
  echo 1
fi

if [ -z "${VALUE}" ]; then
  echo "Please provide new value for the property as second argument"
  echo 1
fi

find . -name build.gradle -exec sed -i "s|${REGEX}.*|${REGEX} \"${VALUE}\"|g" {} \;