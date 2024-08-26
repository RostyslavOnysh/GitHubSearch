#!/usr/bin/env bash

sh exec.sh stop

DIRECTORY=target
if [ -d "$DIRECTORY" ]; then
    echo "The project is already built, skipping docker-compose build"
    mvn clean package
else
    echo "Building docker images..."
    sh exec.sh build
fi

docker-compose up -d --force-recreate

echo "The server is running on http://127.0.0.1:8000"
