#!/bin/bash

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
seed_dir="${script_dir_path}/.."

echo '----- Copying jobs.groovy for testing'
cp jobs.groovy ${seed_dir}/jobs/