#!/bin/bash -e

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
seed_dir="${script_dir_path}/.."

cd ${seed_dir}
echo "----- Launch tests in $(pwd)"
echo "Results will be available here after execution: $(pwd)/build/reports/tests/test/classes/org.kie.jenkins.jobdsl.JobScriptsSpec.html"
./gradlew clean test