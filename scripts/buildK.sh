#!/usr/bin/env bash

echo "Starting Kubernetes-specific build process..."

echo "Cleaning and packaging the Maven project..."
mvn clean package
if [ $? -eq 0 ]; then
    echo "Maven build successful."
else
    echo "Maven build failed. Exiting..."
    exit 1
fi

echo "Building Docker image for Kubernetes..."
docker build -t github-search-app:latest .
if [ $? -eq 0 ]; then
    echo "Docker image built successfully."
else
    echo "Docker image build failed. Exiting..."
    exit 1
fi

echo "Kubernetes build process completed."