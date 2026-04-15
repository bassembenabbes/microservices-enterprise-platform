# 🚀 QUICK START - CHATBOT-SERVICE

**Durée**: 5 minutes pour démarrer en local

---

## 1️⃣ Prérequis

```bash
# Vérifier les versions
java -version          # Doit être 21+
mvn -version          # Doit être 3.8+
docker --version      # Optionnel
```

---

## 2️⃣ Configuration Locale

```bash
# 1. Aller au dossier du service
cd backend/chatbot-service

# 2. Copier le fichier de configuration
cp .env.example .env

# 3. Éditer .env avec vos valeurs
cat .env
# Chercher et remplir:
# GEMINI_API_KEY=your_actual_key_here
```

---

## 3️⃣ Démarrer les Dépendances (Docker)

```bash
# Optionnel: Lancer PostgreSQL + Redis
docker-compose up -d postgres redis

# Vérifier que les services sont up
docker-compose ps
```

---

## 4️⃣ Compiler et Exécuter

```bash
# Compiler le projet
mvn clean package

# Exécuter le service
java -jar target/chatbot-service-1.0.0.jar

# Ou avec Spring Boot Maven plugin
mvn spring-boot:run
```

Vous devriez voir:
```
2026-04-15 10:30:00 INFO  ChatbotApplication - Started ChatbotApplication
```

---

## 5️⃣ Tester le Service

### A. Health Check
```bash
curl http://localhost:8005/api/chatbots/health

# Response
{
  "status": "UP",
  "service": "chatbot-service",
  "version": "1.0.0",
  "timestamp": "2026-04-15T10:30:00"
}
```

### B. API Documentation
```bash
# Ouvrir dans le navigateur
http://localhost:8005/swagger-ui.html
```

### C. Envoyer un Message
```bash
curl -X POST http://localhost:8005/api/chatbots/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "sessionId": "session-456",
    "message": "Bonjour, je cherche un iPhone"
  }'

# Response
{
  "response": "Réponse du chatbot...",
  "sessionId": "session-456",
  "intent": "PRODUCT_SEARCH",
  "timestamp": "2026-04-15T10:30:00"
}
```

---

## 6️⃣ Développer & Tester

### Exécuter les Tests
```bash
# Tests unitaires
mvn test

# Voir les résultats
# target/surefire-reports/
```

### Modifier le Code
```bash
# Structure du projet
src/main/java/com/microservices/chatbot/
├── controller/       # API endpoints
├── service/         # Logique métier
├── client/          # Appels externes
├── dto/            # Objects de transfert
├── model/          # Entités JPA
├── exception/      # Exceptions custom
└── config/         # Configuration

src/main/resources/
├── application.yml  # Configuration
└── docs/           # Documentation interne
```

### Format Code
```bash
# Vérifier le format
mvn spotbugs:gui

# Formater le code
mvn fmt:format
```

---

## 7️⃣ Problèmes Courants

### ❌ "Port 8005 already in use"
```bash
# Vérifier quel processus utilise le port
lsof -i :8005

# Soit changer le port
java -Dserver.port=8006 -jar target/chatbot-service-1.0.0.jar
```

### ❌ "Cannot connect to PostgreSQL"
```bash
# Vérifier que le container est up
docker-compose ps postgres

# Ou lancer manuellement
docker run -d -p 5432:5432 \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=chatbot_db \
  postgres:14
```

### ❌ "GEMINI_API_KEY not set"
```bash
# Vérifier la variable d'environnement
echo $GEMINI_API_KEY

# Ou exporter
export GEMINI_API_KEY=your_key
java -jar target/chatbot-service-1.0.0.jar
```

### ❌ "Validation failed: message required"
```bash
# La requête doit avoir tous les champs requis
{
  "userId": "user-123",           # ✅ REQUIS
  "sessionId": "session-456",     # ✅ REQUIS
  "message": "Bonjour"            # ✅ REQUIS (1-1000 chars)
}
```

---

## 📊 Accès aux Services

| Service | URL | Description |
|---------|-----|-------------|
| **API** | `http://localhost:8005` | Endpoints du chatbot |
| **Swagger UI** | `http://localhost:8005/swagger-ui.html` | API interactive |
| **Health** | `http://localhost:8005/api/chatbots/health` | Status check |
| **Metrics** | `http://localhost:8005/actuator/metrics` | Performance metrics |
| **PostgreSQL** | `localhost:5432` | Database |
| **Redis** | `localhost:6379` | Cache |

---

## 🔨 Commandes Utiles

```bash
# Voir les logs en temps réel
tail -f /app/logs/chatbot-service.log

# Redémarrer le service
mvn clean spring-boot:run

# Compiler sans tester
mvn clean package -DskipTests

# Voir les dépendances
mvn dependency:tree

# Nettoyer les artifacts
mvn clean

# Vérifier le code
mvn verify
```

---

## 📚 Ressources

- 📖 **README.md** - Documentation complète
- 🔒 **SECURITY_GUIDE.md** - Sécurité & secrets
- 🚀 **DEPLOYMENT_PLAN.md** - Déploiement
- 📋 **CHATBOT_SERVICE_ANALYSIS.md** - Analyse détaillée
- 📝 **CHANGELIST.md** - Résumé des modifications

---

## ✅ Checklist du Premier Démarrage

- [ ] Java 21 installé
- [ ] Maven configuré
- [ ] `.env` créé avec `GEMINI_API_KEY`
- [ ] PostgreSQL et Redis lancés (Docker)
- [ ] Service démarré sans erreurs
- [ ] Health check ok (`/api/chatbots/health`)
- [ ] Swagger UI accessible
- [ ] Requête de chat testé avec succès

---

## 🤔 Questions?

1. Vérifier le README.md
2. Lancer `mvn clean install` pour réinitialiser
3. Vérifier les logs: `tail -f logs/chatbot-service.log`
4. Contacter: backend-team@example.com

---

**Happy coding! 🚀**

