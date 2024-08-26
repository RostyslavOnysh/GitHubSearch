#!/usr/bin/env bash

echo "Starting the build process..."

echo "Cleaning and packaging the Maven project..."
mvn clean package
if [ $? -eq 0 ]; then
    echo "Maven build successful."
else
    echo "Maven build failed. Exiting..."
    exit 1
fi

echo "Building Docker images with docker-compose..."
docker-compose build
if [ $? -eq 0 ]; then
    echo "Docker images built successfully."
else
    echo "Docker image build failed. Exiting..."
    exit 1
fi

echo "Build process completed."
