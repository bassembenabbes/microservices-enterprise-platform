#!/bin/bash
echo "🚀 Demarrage des microservices..."
# La clé API Gemini est configurée dans le fichier .env
 export GOOGLE_GEMINI_API_KEY="AIzaSyD3QdPN0H2PsA_jz-t8yTlXF5wbYaUszFI"
cd "$(dirname "$0")"
docker compose --env-file .env up -d --build
echo ""
echo "✅ Services demarres!"
echo "📱 Frontend: http://localhost:3000"
echo "🔌 API Gateway: http://localhost:8000"
echo ""
echo "Pour voir les logs: docker-compose logs -f"
