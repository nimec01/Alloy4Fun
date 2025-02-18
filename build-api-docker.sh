#!/usr/bin/env zsh

set -eux

CACHE=../.cache/api
CACHE_TO=--cache-to=type=local,dest=${CACHE}/dist,mode=max

mkdir -p ${CACHE}/base/ ${CACHE}/dist/

# Create a new builder instance

echo "Creating a new builder instance..."

docker buildx create --use --name=alloy4fun-builder --driver docker-container --node larger_log --driver-opt env.BUILDKIT_STEP_LOG_MAX_SIZE=500000000

# Build the api

echo "Building the api..."

cd api

docker buildx build --load --cache-from=type=local,src=${CACHE}/dist ${CACHE_TO} --tag alloy4fun-quarkus-api:fmi --file Dockerfile .

cd -

# Copy the api jar

docker create --name alloy4fun-api alloy4fun-quarkus-api:fmi

docker cp alloy4fun-api:/app/target/quarkus-app/ quarkus-app/

docker rm alloy4fun-api
