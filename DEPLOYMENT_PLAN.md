# 🚀 PLAN DE DÉPLOIEMENT - CHATBOT-SERVICE v1.0.0

## 📅 Timeline & Phases

```
┌─────────────────────────────────────────────────────────┐
│  SEMAINE 1: Sécurité & Stabilité (URGENT)              │
│  ✓ Externaliser secrets                                 │
│  ✓ Ajouter validation & exception handling              │
│  ✓ Fixer dépendances                                    │
│  → DÉPLOIEMENT BETA                                     │
└─────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────┐
│  SEMAINE 2: Testing & Documentation                     │
│  ✓ Tests unitaires (>80% coverage)                      │
│  ✓ API Swagger/OpenAPI                                  │
│  ✓ README & examples                                    │
│  → REVIEW QUALITÉ                                       │
└─────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────┐
│  SEMAINE 3: Performance & Features                      │
│  ✓ Redis pour cache/sessions                            │
│  ✓ Distributed tracing                                  │
│  ✓ Métriques Micrometer                                 │
│  → LOAD TESTING                                         │
└─────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────┐
│  SEMAINE 4: Production Deployment                       │
│  ✓ Kubernetes manifests                                 │
│  ✓ Production hardening                                 │
│  ✓ Monitoring & alerting                                │
│  → PRODUCTION RELEASE                                   │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ PHASE 1: SÉCURITÉ & STABILITÉ (URGENTE)

**Duration**: 3-5 jours  
**Priority**: 🔴 CRITIQUE

### Tâches Complétées ✓

- [x] Externaliser la clé API Gemini
  - Fichier: `.env.example`
  - Configuration: `application.yml` avec variables d'environnement

- [x] Ajouter validation des entrées
  - Fichier: `ChatRequest.java` avec Jakarta Validation
  - Annotations: `@NotBlank`, `@NotNull`, `@Size`

- [x] Exception Handling
  - Fichiers: `ChatbotException.java`, `ExternalServiceException.java`, `IntentDetectionException.java`
  - Handler global: `GlobalExceptionHandler.java`
  - DTO d'erreur: `ErrorResponse.java`

- [x] Versioning API
  - Endpoint ancien: `/api/chatbots/chat`
  - Endpoint nouveau: `/api/v1/chatbots/chat`
  - Permet les futures versions sans breaking changes

- [x] Fixer pom.xml
  - Suppression duplication `spring-boot-starter-webflux`
  - Ajout dépendances essentielles (Validation, Testing, Redis, Metrics)

- [x] Améliorer application.yml
  - Configuration externalisée avec variables d'environnement
  - Profiles Spring (dev, prod, staging)
  - Paramètres optimisés pour production

- [x] Dockerfile optimisé
  - Multi-stage build (reduces image size)
  - Utilisateur non-root pour sécurité
  - JVM tuned for containers
  - Health checks intégrés

### ⚠️ Actions Requises

**Configuration Secrets Manager:**

Avant le déploiement, configurer l'un de ces systèmes:

**Option 1: AWS Secrets Manager (Recommandé Cloud)**
```bash
# Créer un secret
aws secretsmanager create-secret \
  --name chatbot-service/gemini-api-key \
  --secret-string "your_actual_key"

# Spring Cloud AWS intégration
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-secretsmanager</artifactId>
</dependency>

# application.yml
spring:
  cloud:
    aws:
      secretsmanager:
        prefix: chatbot-service
```

**Option 2: HashiCorp Vault (Recommandé On-Premise)**
```bash
# Créer un secret Vault
vault kv put secret/chatbot-service \
  gemini-api-key="your_actual_key" \
  db-password="secure_password"

# Spring intégration
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      uri: https://vault.example.com:8200
      authentication: KUBERNETES
      kv-version: 2
```

**Option 3: Kubernetes Secrets (Recommandé Kubernetes)**
```bash
# Créer un secret Kubernetes
kubectl create secret generic chatbot-secrets \
  --from-literal=gemini-api-key=your_actual_key \
  --from-literal=db-password=secure_password

# deployment.yaml
env:
  - name: GEMINI_API_KEY
    valueFrom:
      secretKeyRef:
        name: chatbot-secrets
        key: gemini-api-key
```

**Configuration Test:**
```bash
# 1. Copier et configurer .env
cp backend/chatbot-service/.env.example backend/chatbot-service/.env
# Éditer avec valeurs réelles

# 2. Compiler
cd backend/chatbot-service
mvn clean package

# 3. Tester localement
java -jar target/chatbot-service-1.0.0.jar

# 4. Vérifier
curl http://localhost:8005/api/v1/chatbots/health
curl http://localhost:8005/swagger-ui.html
```

---

## 📋 PHASE 2: TESTING & DOCUMENTATION

**Duration**: 5-7 jours  
**Priority**: 🟡 HAUTE

### Tâches

- [ ] Tests unitaires
  - Coverage cible: >80%
  - Fichier exemple: `ChatbotControllerTest.java`
  - Exécuter: `mvn test`

- [ ] Tests d'intégration
  - TestContainers pour PostgreSQL
  - Exemple:
    ```java
    @Testcontainers
    @SpringBootTest
    class ChatbotServiceIntegrationTest {
        @Container
        static PostgreSQLContainer<?> postgres = 
            new PostgreSQLContainer<>("postgres:14");
    }
    ```

- [ ] Documentation API
  - Swagger UI: http://localhost:8005/swagger-ui.html
  - OpenAPI JSON: http://localhost:8005/v3/api-docs
  - Fichier: `OpenApiConfig.java` (déjà créé)

- [ ] README complet
  - Fichier: `README.md` (✓ créé)
  - Contient: Installation, API docs, examples, troubleshooting

- [ ] JavaDoc
  - Ajouter sur toutes les classes publiques
  - Template:
    ```java
    /**
     * Description courte.
     * 
     * Description longue avec détails.
     * 
     * @param paramName description
     * @return description du retour
     * @throws ExceptionType situation
     */
    ```

---

## 🚀 PHASE 3: PERFORMANCE & FEATURES

**Duration**: 7-10 jours  
**Priority**: 🟡 HAUTE

### Tâches

- [ ] Redis Configuration
  ```yaml
  spring:
    data:
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
  
  @EnableCaching
  @Configuration
  public class CacheConfig {
      @Bean
      public RedisCacheManager cacheManager() { ... }
  }
  ```

- [ ] Session Caching
  ```java
  @Cacheable(value = "sessions", key = "#sessionId")
  public ChatSession getSession(String sessionId) { ... }
  
  @CachePut(value = "sessions", key = "#session.sessionId")
  public ChatSession updateSession(ChatSession session) { ... }
  ```

- [ ] Distributed Tracing
  ```xml
  <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-tracing-bridge-brave</artifactId>
  </dependency>
  ```

- [ ] Métriques Micrometer
  ```java
  @Component
  public class ChatbotMetrics {
      private final MeterRegistry meterRegistry;
      
      public void recordChatProcessed(String intent) {
          Counter.builder("chatbot.messages.processed")
              .tag("intent", intent)
              .register(meterRegistry)
              .increment();
      }
  }
  ```

- [ ] Performance Testing
  ```bash
  # Avec Apache JMeter ou Gatling
  mvn gatling:test
  ```

---

## 📦 PHASE 4: PRODUCTION DEPLOYMENT

**Duration**: 3-5 jours  
**Priority**: 🔴 CRITIQUE

### Docker Build & Push

```bash
# Build image
docker build -t chatbot-service:1.0.0 .
docker build -t chatbot-service:latest .

# Tag for registry
docker tag chatbot-service:1.0.0 myregistry.azurecr.io/chatbot-service:1.0.0
docker tag chatbot-service:latest myregistry.azurecr.io/chatbot-service:latest

# Push to registry
docker login myregistry.azurecr.io
docker push myregistry.azurecr.io/chatbot-service:1.0.0
docker push myregistry.azurecr.io/chatbot-service:latest

# Verify
docker run --rm -it chatbot-service:1.0.0 /healthcheck
```

### Kubernetes Deployment

**Fichier**: `k8s/chatbot-service-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chatbot-service
  namespace: microservices
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  
  selector:
    matchLabels:
      app: chatbot-service
  
  template:
    metadata:
      labels:
        app: chatbot-service
        version: "1.0.0"
    spec:
      serviceAccountName: chatbot-service
      
      containers:
      - name: chatbot-service
        image: myregistry.azurecr.io/chatbot-service:1.0.0
        imagePullPolicy: IfNotPresent
        
        ports:
        - name: http
          containerPort: 8005
          protocol: TCP
        
        env:
        - name: GEMINI_API_KEY
          valueFrom:
            secretKeyRef:
              name: chatbot-secrets
              key: gemini-api-key
        
        - name: DB_URL
          valueFrom:
            configMapKeyRef:
              name: chatbot-config
              key: db-url
        
        - name: REDIS_HOST
          value: redis-service
        
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        
        resources:
          requests:
            cpu: 200m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 512Mi
        
        livenessProbe:
          httpGet:
            path: /api/v1/chatbots/health
            port: http
            scheme: HTTP
          initialDelaySeconds: 40
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /api/v1/chatbots/health
            port: http
            scheme: HTTP
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        volumeMounts:
        - name: logs
          mountPath: /app/logs
      
      volumes:
      - name: logs
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: chatbot-service
  namespace: microservices
spec:
  type: ClusterIP
  selector:
    app: chatbot-service
  ports:
  - name: http
    port: 8005
    targetPort: 8005
    protocol: TCP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: chatbot-service-hpa
  namespace: microservices
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: chatbot-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**Déploiement:**
```bash
# Créer secrets
kubectl create secret generic chatbot-secrets \
  --from-literal=gemini-api-key=$GEMINI_API_KEY \
  -n microservices

# Créer configmap
kubectl create configmap chatbot-config \
  --from-literal=db-url=jdbc:postgresql://postgres:5432/chatbot_db \
  -n microservices

# Déployer
kubectl apply -f k8s/chatbot-service-deployment.yaml

# Vérifier
kubectl get pods -n microservices -l app=chatbot-service
kubectl logs -n microservices -l app=chatbot-service -f
kubectl get svc -n microservices chatbot-service
```

### Monitoring & Alerting

**Prometheus Scrape Config:**
```yaml
- job_name: 'chatbot-service'
  static_configs:
    - targets: ['chatbot-service:8005']
  metrics_path: '/actuator/prometheus'
  scrape_interval: 15s
```

**Grafana Dashboard:**
```json
{
  "dashboard": {
    "title": "Chatbot Service - Production",
    "panels": [
      {
        "title": "Messages Processed per Minute",
        "targets": [
          {
            "expr": "rate(chatbot_messages_processed_total[1m])"
          }
        ]
      },
      {
        "title": "P95 Response Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{endpoint='/api/v1/chatbots/chat'}[1m]))"
          }
        ]
      }
    ]
  }
}
```

### Rollback Procedure

```bash
# Si problème détecté
kubectl rollout undo deployment/chatbot-service -n microservices

# Vérifier status
kubectl rollout status deployment/chatbot-service -n microservices

# Voir l'historique
kubectl rollout history deployment/chatbot-service -n microservices
```

---

## 🎯 MÉTRIQUES DE SUCCÈS

| Métrique | Target | Méthode |
|----------|--------|---------|
| **Availability** | 99.9% | Monitoring Kubernetes |
| **Response Time (p95)** | <500ms | Prometheus metrics |
| **Error Rate** | <0.5% | Application logs |
| **Code Coverage** | >80% | JaCoCo reports |
| **Security Score** | A+ | OWASP Top 10 check |

---

## 🔗 Ressources

- Spring Boot 3.2: https://spring.io/projects/spring-boot
- Kubernetes: https://kubernetes.io/docs/
- Prometheus: https://prometheus.io/
- Grafana: https://grafana.com/
- HashiCorp Vault: https://www.vaultproject.io/

---

**Statut**: 📋 Planifié  
**Last Updated**: 15 avril 2026  
**Next Review**: Après Phase 1 completion

