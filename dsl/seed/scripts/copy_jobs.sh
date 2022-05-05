#!/bin/bash

script_dir_path=`dirname "${BASH_SOURCE[0]}"`
seed_dir="${script_dir_path}/.."

echo '----- Copying jobs.groovy for testing'
cp jobs.groovy ${seed_dir}/jobs/