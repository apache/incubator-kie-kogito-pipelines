#!/bin/bash -e

script_dir_path=`dirname "${BASH_SOURCE[0]}"`
seed_dir="${script_dir_path}/.."

echo '----- Launch tests'
echo "Results will be available here after execution: ${seed_dir}/build/reports/tests/test/classes/org.kie.jenkins.jobdsl.JobScriptsSpec.html"
cd ${seed_dir} && ./gradlew clean test