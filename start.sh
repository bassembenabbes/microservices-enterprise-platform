#!/bin/bash
echo "🚀 Demarrage des microservices..."
# Définir la clé API Gemini
export GOOGLE_GEMINI_API_KEY="votre-clé-api-gemini"
cd "$(dirname "$0")"
docker compose up -d --build
echo ""
echo "✅ Services demarres!"
echo "📱 Frontend: http://localhost:3000"
echo "🔌 API Gateway: http://localhost:8000"
echo ""
echo "Pour voir les logs: docker-compose logs -f"
