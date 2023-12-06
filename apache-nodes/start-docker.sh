#!/bin/bash
source /opt/bash-utils/logger.sh

INFO "Starting supervisor"
sudo bash -c "/usr/bin/supervisord >> /dev/null 2>&1" &

INFO "Waiting for docker to be running"
source wait-for-docker.sh
if [ $? -ne 0 ]; then
    ERROR "dockerd is not running after max time"
    exit 1
else
    sudo chown root:docker /var/run/docker.sock
    INFO "dockerd is running"
fi