# Readme.md

## go inside docker terminal using docker compose

- docker-compose -f user-service/docker-compose.yml exec user-service /bin/sh

## run docker compose

- docker-compose -f user-service/docker-compose.yml up -d

## check docker logs

- docker logs test-project-user-service-1

spring boot version: 4.0.0-M3

## user-service

port 8081

run- local : run main class

run-docker : docker build -t user-service-app ./user-service
docker run -d -p 8081:8080 --name user-service user-service-app

