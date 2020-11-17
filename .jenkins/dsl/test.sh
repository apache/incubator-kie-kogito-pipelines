#!/bin/bash -e

TEMP_DIR=`mktemp -d`

echo '----- Copying seed repo'
cp -r ../../dsl/seed $TEMP_DIR

echo '----- Launching seed tests'
${TEMP_DIR}/seed/scripts/seed_test.sh