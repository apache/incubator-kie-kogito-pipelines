#!/bin/sh

set -e
groupadd -r jenkins -g 1001 &&  useradd -u 1001 -r -g jenkins  -m -d /home/jenkins -c "jenkins user" jenkins
