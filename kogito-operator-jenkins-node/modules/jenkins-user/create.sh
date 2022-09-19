#!/bin/sh

set -e
groupadd -r jenkins -g 1001 &&  useradd -u 1001 -r -g jenkins  -m -d /home/jenkins -c "jenkins user" jenkins

usermod --add-subuids 10000-75535 jenkins
usermod --add-subgids 10000-75535 jenkins
