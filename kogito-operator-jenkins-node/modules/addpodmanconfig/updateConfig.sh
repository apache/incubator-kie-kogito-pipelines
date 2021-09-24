#!/bin/sh
set -e

mkdir -p /etc/containers/
cp /tmp/artifacts/libpod.conf  /etc/containers/libpod.conf
