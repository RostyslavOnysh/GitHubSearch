#!/usr/bin/env bash

docker-compose stop

mvn clean package

DIRECTORY=target
if [ -d "$DIRECTORY" ]; then
    echo "The project is already built, skipping docker-compose build"
else
    echo "Building docker images..."
    docker-compose build
fi

docker-compose up --force-recreate

echo "The server is running on http://127.0.0.1:8000"