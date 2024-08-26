#!/bin/bash

case "$1" in
  "build") scripts/build.sh ;;
  "rebuild") scripts/rebuild.sh ;;
  "clear") scripts/clear.sh ;;
  "run") scripts/run.sh ;;
  "bash:app") docker-compose exec -it app bash ;;
  "bash:redis") docker-compose exec -it redis bash ;;
  "logs:app") docker-compose logs app ;;
  "logs:redis") docker-compose logs redis ;;
  "stop") scripts/stop.sh ;;
  *) docker-compose "$1"
esac