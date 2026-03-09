#/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cd $SCRIPT_DIR/qa_hub_backend
docker stop qa_hub_backend
docker rmi qa_hub_backend --force
docker compose up -d

cd $SCRIPT_DIR/qa_hub_frontend
docker stop qa_hub_frontend
docker rmi qa_hub_frontend --force
docker compose up -d