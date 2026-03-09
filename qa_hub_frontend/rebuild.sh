#/bin/bash

docker stop qa_hub_frontend
docker rmi qa_hub_frontend --force
docker compose up -d