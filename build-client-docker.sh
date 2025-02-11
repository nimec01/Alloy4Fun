#!/usr/bin/env zsh

set -eux

CACHE=../.cache/client
CACHE_TO=--cache-to=type=local,dest=${CACHE}/dist,mode=max

mkdir -p ${CACHE}/base/ ${CACHE}/dist/

# Create a new builder instance

echo "Creating a new builder instance..."

docker buildx create --use --name=alloy4fun-builder --driver docker-container --node larger_log --driver-opt env.BUILDKIT_STEP_LOG_MAX_SIZE=500000000

# Build the client

echo "Building the client..."

cd meteor

docker buildx build --load --cache-from=type=local,src=${CACHE}/dist ${CACHE_TO} --tag alloy4fun-meteor:fmi --file Dockerfile .

cd -

# Copy the build folder

docker create --name alloy4fun-meteor alloy4fun-meteor:fmi

mkdir -p meteor-dist/

docker cp alloy4fun-meteor:/opt/meteor/dist/bundle/ meteor-dist/

docker rm alloy4fun-meteor
