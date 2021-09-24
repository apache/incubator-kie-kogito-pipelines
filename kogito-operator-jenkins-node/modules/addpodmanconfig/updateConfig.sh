#!/bin/sh
set -e

mkdir -p /etc/containers/
cp /tmp/artifacts/containers.conf  /etc/containers/containers.conf

sed -i 's|,metacopy=on||g' /etc/containers/storage.conf