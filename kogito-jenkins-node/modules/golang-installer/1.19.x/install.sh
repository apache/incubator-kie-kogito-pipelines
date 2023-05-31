#!/bin/sh
set -e
mkdir -p /opt/tools/golang/1.19
tar -C /opt/tools/golang/1.19 -xf /tmp/artifacts/go.linux-amd64.tar.gz
