#!/usr/bin/env bash

echo "Starting the clear process..."
sh exec.sh clear
if [ $? -eq 0 ]; then
    echo "Clear process completed successfully."
else
    echo "Clear process failed. Exiting script."
    exit 1
fi

echo "Starting the build process..."
sh exec.sh build
if [ $? -eq 0 ]; then
    echo "Build process completed successfully."
else
    echo "Build process failed. Exiting script."
    exit 1
fi
