#!/usr/bin/env bash

echo "Removing target directory..."
rm -rf target

echo "Stopping and removing containers..."
docker-compose down

echo "Removing Docker images..."
docker rmi redis:7.4.0
docker rmi github-search-app:latest

echo "Removing Docker volumes..."
docker volume rm github-search-app-redis-data

echo "All cleanup tasks are complete."
