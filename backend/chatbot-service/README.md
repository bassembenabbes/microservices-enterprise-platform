# 🤖 CHATBOT-SERVICE - Documentation Complète

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Installation & Déploiement](#installation--déploiement)
5. [API Documentation](#api-documentation)
6. [Exemples d'utilisation](#exemples-dutilisation)
7. [Dépannage](#dépannage)
8. [Contribution](#contribution)

---

## Vue d'ensemble

**Chatbot-Service** est un micro-service Spring Boot qui fournit une API intelligente de chat conversationnel avec:

- 🤖 Intégration **Google Gemini** pour l'IA générative
- 🔍 Système **RAG** (Retrieval-Augmented Generation) pour le contexte
- 🎯 **Détection d'intention** automatique
- 💾 Gestion des **sessions utilisateur**
- 🔗 Intégration avec d'autres micro-services (User, Product, Order)
- 🛡️ Gestion des erreurs robuste
- 📊 Monitoring et métriques

**Version**: 1.0.0  
**Status**: Production Ready (Phase 1)  
**Java**: 21 LTS  
**Spring Boot**: 3.2.0  

---

## Architecture

### Composants Principaux

```
┌─────────────────────────────────────────────────────────┐
│                   CLIENT (Frontend)                      │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ↓
    ┌──────────────────────────────────────┐
    │     API Gateway / Load Balancer      │
    └──────────────────┬───────────────────┘
                       │
                       ↓
    ┌──────────────────────────────────────┐
    │     ChatbotController                │
    │     (/api/chatbots/chat)          │
    └──────────────────┬───────────────────┘
                       │
                       ↓
    ┌──────────────────────────────────────┐
    │     ChatbotService (Orchestration)   │
    └──┬──────────┬──────────┬────────┬───┘
       │          │          │        │
       ↓          ↓          ↓        ↓
   ┌─────┐   ┌──────┐   ┌──────┐  ┌─────┐
   │User │   │Prod. │   │Order │  │RAG  │
   │Srvc │   │Srvc  │   │Srvc  │  │Srvc │
   └─────┘   └──────┘   └──────┘  └─────┘
       │          │          │        │
       └──────────┴──────────┴────────┘
              ↓
    ┌──────────────────────────────┐
    │  Gemini API (Google Cloud)   │
    └──────────────────────────────┘
```

### Flux de Traitement d'un Message

```
1. Requête utilisateur → ChatbotController
2. Validation (Jakarta Validation)
3. ChatbotService.processMessage()
   ├─ Détection d'intention
   ├─ Appel service(s) pertinent(s) (avec retry/circuit breaker)
   ├─ Enrichissement contexte (RAG)
   └─ Appel Gemini pour génération réponse
4. Session storage (PostgreSQL + Redis cache)
5. Réponse JSON → Client
```

### Schéma Détection d'Intention

| Intent | Conditions | Services appelés | Exemple |
|--------|-----------|-----------------|---------|
| `PRODUCT_SEARCH` | contient "iphone", "produit", "cherche" | ProductService | "Cherche un iPhone" |
| `ORDER_STATUS` | contient "commande" + "statut" | OrderService | "Statut de ma commande #12345" |
| `USER_INFO` | contient "compte", "profil" | UserService | "Mon compte" |
| `GENERAL_CHAT` | Autre | RAG + Gemini | Tout autre message |

---

## Configuration

### Variables d'Environnement

Voir le fichier `.env.example` pour la liste complète. Principales variables:

```bash
# Secrets (À définir obligatoirement)
export GEMINI_API_KEY=your_api_key_here
export DB_PASSWORD=secure_password

# Services
export USER_SERVICE_URL=http://user-service:8001
export PRODUCT_SERVICE_URL=http://product-service:8002
export ORDER_SERVICE_URL=http://order-service:8003

# Database
export DB_URL=jdbc:postgresql://postgres:5432/chatbot_db
export DB_USERNAME=chatbot_user

# Redis (Caching)
export REDIS_HOST=redis
export REDIS_PORT=6379

# Profile
export SPRING_PROFILES_ACTIVE=prod
```

### Profiles Spring

- **dev**: Configuration de développement avec logs DEBUG
- **prod**: Configuration de production optimisée
- **staging**: Configuration de pré-production

Activation:
```bash
java -jar chatbot-service.jar --spring.profiles.active=prod
```

### Sécurité des Secrets

⚠️ **JAMAIS** commiter les secrets en clair!

Options recommandées:
1. **Environment Variables** (Local & Docker)
2. **AWS Secrets Manager** (Cloud AWS)
3. **HashiCorp Vault** (On-premise & Cloud)
4. **Spring Cloud Config** (Centralisé)
5. **Kubernetes Secrets** (Containerized)

Exemple avec Vault:
```yaml
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      authentication: KUBERNETES
```

---

## Installation & Déploiement

### Prérequis

- Java 21 (ou plus)
- Maven 3.8+
- PostgreSQL 14+
- Redis 7+ (optionnel, mais recommandé)
- Docker & Docker Compose (pour containerization)

### Installation Locale

```bash
# 1. Cloner le repository
cd backend/chatbot-service

# 2. Configurer les variables d'environnement
cp .env.example .env
# Éditer .env avec vos valeurs

# 3. Compiler
mvn clean package

# 4. Exécuter
java -jar target/chatbot-service-1.0.0.jar
```

Service accessible: `http://localhost:8005`  
API Docs: `http://localhost:8005/swagger-ui.html`

### Déploiement Docker

```bash
# 1. Construire l'image
docker build -t chatbot-service:1.0.0 .

# 2. Exécuter avec docker-compose
docker-compose up -d chatbot-service

# 3. Vérifier la santé
curl http://localhost:8005/api/chatbots/health
```

### Déploiement Kubernetes

```bash
# Appliquer les manifests
kubectl apply -f k8s/chatbot-service-deployment.yaml
kubectl apply -f k8s/chatbot-service-service.yaml

# Vérifier le déploiement
kubectl get pods -l app=chatbot-service
kubectl logs -f deployment/chatbot-service
```

---

## API Documentation

### Base URL

```
Development: http://localhost:8005
Docker: http://chatbot-service:8005
Production: https://api.example.com/chatbot
```

### Documentation Interactive

Swagger UI: `GET /swagger-ui.html`  
OpenAPI JSON: `GET /v3/api-docs`

### Endpoints Principaux

#### 1. POST /api/chatbots/chat

Envoyer un message au chatbot.

**Request:**
```json
{
  "userId": "user-123",
  "sessionId": "session-456",
  "message": "Cherche un iPhone 15",
  "context": {
    "language": "fr",
    "location": "paris"
  }
}
```

**Response (200 OK):**
```json
{
  "response": "Nous avons trouvé 3 iPhones disponibles...",
  "sessionId": "session-456",
  "intent": "PRODUCT_SEARCH",
  "action": "search",
  "suggestions": [
    "Voir les détails du produit",
    "Ajouter au panier",
    "Comparer les prix"
  ],
  "data": {
    "products": [
      {
        "id": "prod-1",
        "name": "iPhone 15 Pro",
        "price": 999.99,
        "stock": 5
      }
    ]
  },
  "timestamp": "2026-04-15T10:30:00"
}
```

**Error Response (400 Bad Request):**
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "message: Le message ne peut pas être vide",
  "status": 400,
  "path": "/api/chatbots/chat",
  "timestamp": "2026-04-15T10:30:00"
}
```

**Error Response (503 Service Unavailable):**
```json
{
  "errorCode": "EXTERNAL_SERVICE_ERROR",
  "message": "Service indisponible. Veuillez réessayer plus tard.",
  "status": 503,
  "path": "/api/chatbots/chat",
  "timestamp": "2026-04-15T10:30:00"
}
```

**Codes HTTP:**
- `200 OK`: Réponse générée avec succès
- `400 Bad Request`: Données invalides ou erreur de validation
- `503 Service Unavailable`: Service Gemini ou micro-service indisponible

#### 2. GET /api/chatbots/health

Vérifier la santé du service.

**Response:**
```json
{
  "status": "UP",
  "service": "chatbot-service",
  "version": "1.0.0",
  "timestamp": "2026-04-15T10:30:00"
}
```

#### 3. GET /actuator/metrics

Métriques de performance.

---

## Exemples d'utilisation

### cURL

```bash
# 1. Recherche de produit
curl -X POST http://localhost:8005/api/chatbots/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "sessionId": "session-456",
    "message": "Je cherche un iPhone"
  }'

# 2. Statut de commande
curl -X POST http://localhost:8005/api/chatbots/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "sessionId": "session-456",
    "message": "Quel est le statut de ma commande #12345?"
  }'

# 3. Health check
curl http://localhost:8005/api/chatbots/health
```

### JavaScript/Fetch

```javascript
const chatWithBot = async (userId, sessionId, message) => {
  const response = await fetch(
    'http://localhost:8005/api/chatbots/chat',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId,
        sessionId,
        message
      })
    }
  );
  
  if (!response.ok) {
    throw new Error(`Error: ${response.status}`);
  }
  
  return response.json();
};

// Utilisation
chatWithBot('user-123', 'session-456', 'Cherche une iPhone')
  .then(response => console.log(response.response))
  .catch(error => console.error(error));
```

### Python

```python
import requests
import json

def chat_with_bot(user_id, session_id, message):
    url = "http://localhost:8005/api/chatbots/chat"
    payload = {
        "userId": user_id,
        "sessionId": session_id,
        "message": message
    }
    
    response = requests.post(url, json=payload)
    response.raise_for_status()
    return response.json()

# Utilisation
result = chat_with_bot("user-123", "session-456", "Je cherche un iPhone")
print(result['response'])
```

### Java

```java
@RestClient
public interface ChatbotClient {
    @PostExchange("/api/chatbots/chat")
    ChatResponse chat(@RequestBody ChatRequest request);
}

// Utilisation
ChatRequest request = new ChatRequest();
request.setUserId("user-123");
request.setSessionId("session-456");
request.setMessage("Je cherche un iPhone");

ChatResponse response = chatbotClient.chat(request);
System.out.println(response.getResponse());
```

---

## Dépannage

### 🔴 Erreur: "GEMINI_API_KEY not set"

**Cause**: Variable d'environnement `GEMINI_API_KEY` manquante  
**Solution**:
```bash
export GEMINI_API_KEY=your_actual_key
# ou dans .env
GEMINI_API_KEY=your_actual_key
```

### 🔴 Erreur: "Cannot connect to PostgreSQL"

**Cause**: Base de données indisponible  
**Solution**:
```bash
# Vérifier la connexion
psql -h localhost -U chatbot_user -d chatbot_db

# Vérifier les variables d'environnement
echo $DB_URL
echo $DB_USERNAME
```

### 🔴 Erreur: "EXTERNAL_SERVICE_ERROR"

**Cause**: Micro-service (User, Product, Order) indisponible  
**Solution**:
```bash
# Vérifier les services
curl http://user-service:8001/health
curl http://product-service:8002/health
curl http://order-service:8003/health

# Vérifier la configuration
cat application.yml | grep services
```

### 🟡 Logs vides ou insuffisants

**Solution**:
```bash
# Augmenter le log level
export LOG_LEVEL=DEBUG

# Ou dans application.yml
logging:
  level:
    com.microservices.chatbot: DEBUG
```

### 🟡 Performance lente

**Solutions**:
1. Vérifier Redis est actif (cache)
2. Vérifier la taille des réponses Gemini
3. Augmenter le pool de connexions:
   ```yaml
   spring.datasource.hikari.maximum-pool-size: 20
   ```
4. Analyser avec le profiler
   ```bash
   # Activation
   management.endpoints.web.exposure.include=health,metrics,prometheus
   ```

---

## Contribution

### Développement Local

```bash
# 1. Créer une branche feature
git checkout -b feature/ma-fonctionnalite

# 2. Implémenter & tester
mvn clean test
mvn spotbugs:check

# 3. Commit & Push
git commit -m "feat: description de la feature"
git push origin feature/ma-fonctionnalite

# 4. Créer une Pull Request
```

### Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Coverage
mvn jacoco:report
# Rapport: target/site/jacoco/index.html

# Analyse de code
mvn spotbugs:gui
mvn pmd:pmd
```

### Standards de Code

- Suivre les conventions Google Java Style Guide
- Coverage minimum: 80%
- Utiliser Lombok pour réduire le boilerplate
- JavaDoc obligatoire pour les API publiques

---

## Support & Contact

**Slack**: #chatbot-service  
**Email**: backend-team@example.com  
**GitHub**: https://github.com/microservices/chatbot-service  
**Issues**: Créer une issue GitHub  

---

**Dernière mise à jour**: 15 avril 2026  
**Mainteneur**: Équipe Backend

