#/bin/bash

# Works only with default mongo host
docker stop qa_hub_backend
docker rmi qa_hub_backend --force
docker compose up -d