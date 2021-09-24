#!/bin/sh

set -e

echo 'export PATH=$GOROOT/bin:$PATH' >> /home/jenkins/.bashrc
echo 'export GOPATH=$(go env GOPATH)' >> /home/jenkins/.bashrc
echo 'export PATH=$GOPATH/bin:$PATH' >> /home/jenkins/.bashrc
