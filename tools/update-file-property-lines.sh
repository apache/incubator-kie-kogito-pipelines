#!/usr/bin/env bash
set -eo pipefail
 
# kogito-runtimes, optaplanner. kogito-examples or optaplanner-quickstarts
FILE=$1
PROPERTY=$2
VALUE=$3

if [ -z "${FILE}" ]; then
  echo "Please provide which a file to update"
  echo 1
fi

if [ -z "${PROPERTY}" ]; then
  echo "Please provide which a property key corresponding to the line to update"
  echo 1
fi

if [ -z "${VALUE}" ]; then
  echo "Please provide new value for the property as second argument"
  echo 1
fi

find . -name ${FILE} -exec sed -i "s|${PROPERTY}.*|${PROPERTY} \"${VALUE}\"|g" {} \;