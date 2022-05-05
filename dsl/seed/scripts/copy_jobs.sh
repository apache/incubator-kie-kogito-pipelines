#!/bin/bash

script_dir_path=$(cd `dirname "${BASH_SOURCE[0]}"`; pwd -P)
seed_dir="${script_dir_path}/.."

echo "scripts dir = ${${script_dir_path}}"
echo "current dir = $(pwd)"
ls -al

echo '----- Copying jobs.groovy for testing'
cp jobs.groovy ${seed_dir}/jobs/