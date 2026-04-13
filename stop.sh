#!/bin/bash
echo "🛑 Arret des services..."
cd "$(dirname "$0")"
docker compose down
echo "✅ Services arretes"
sudo systemctl stop postgresql
docker stop rabbitmq
docker stop metasfresh-docker-webui-1
docker stop metasfresh-docker-app-1

