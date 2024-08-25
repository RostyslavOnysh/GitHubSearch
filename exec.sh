#!/bin/bash

case "$1" in
  "build") scripts/build.sh ;;
  "run") scripts/run.sh ;;
  "bash") docker-compose exec -it app bash ;;
  "stop") scripts/stop.sh ;;
  *) docker-compose "$1"
esac