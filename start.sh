#!/bin/bash
echo "🚀 Demarrage des microservices..."
cd "$(dirname "$0")"
docker compose up -d --build
echo ""
echo "✅ Services demarres!"
echo "📱 Frontend: http://localhost:3000"
echo "🔌 API Gateway: http://localhost:8000"
echo ""
echo "Pour voir les logs: docker-compose logs -f"
