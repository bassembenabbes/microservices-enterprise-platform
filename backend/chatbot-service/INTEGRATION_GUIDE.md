# 🔗 GUIDE D'INTÉGRATION - CHATBOT-SERVICE

**Integration avec les autres microservices**

---

## 📋 Services Dépendants

```
chatbot-service
├── user-service (8001) ← Récupère infos utilisateur
├── product-service (8002) ← Recherche produits
├── order-service (8003) ← Statut commandes
└── Gemini API (Google Cloud) ← IA générative
```

---

## 🔌 User Service Integration

### 1. Health Check
```bash
curl http://user-service:8001/health
```

### 2. Get User Profile
```bash
curl -X GET http://user-service:8001/api/users/{userId}
```

**Réponse attendue**:
```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john@example.com",
  "tier": "premium"
}
```

### 3. Intégration dans ChatbotService

```java
public class UserServiceClient {
    
    @Value("${services.user.url}")
    private String userServiceUrl;
    
    @Value("${services.user.timeout}")
    private int timeout;
    
    public UserProfile getUserProfile(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(UserProfile.class)
                .block(Duration.ofMillis(timeout));
        } catch (WebClientException e) {
            log.error("Erreur user-service: {}", e.getMessage());
            throw new ExternalServiceException("user-service", "Impossible de récupérer profil utilisateur");
        }
    }
}
```

### 4. Fallback Strategy
```java
// Si user-service indisponible
private UserProfile getUserProfileFallback(String userId) {
    return UserProfile.builder()
        .id(userId)
        .name("Utilisateur " + userId)
        .tier("standard")
        .build();
}
```

---

## 🛍️ Product Service Integration

### 1. Health Check
```bash
curl http://product-service:8002/health
```

### 2. Search Products
```bash
curl -X GET "http://product-service:8002/api/products/search?query=iPhone&limit=10"
```

**Réponse attendue**:
```json
{
  "results": [
    {
      "id": "prod-1",
      "name": "iPhone 15 Pro",
      "price": 999.99,
      "stock": 5,
      "rating": 4.8
    }
  ],
  "total": 1,
  "page": 1
}
```

### 3. Intégration dans ChatbotService

```java
public class ProductServiceClient {
    
    @Value("${services.product.url}")
    private String productServiceUrl;
    
    @Value("${services.product.timeout}")
    private int timeout;
    
    @Retry(name = "product-service")
    @CircuitBreaker(name = "product-service")
    public List<Product> searchProducts(String query) {
        try {
            String url = productServiceUrl + "/api/products/search";
            return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(url)
                    .queryParam("query", query)
                    .queryParam("limit", 10)
                    .build())
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .map(SearchResponse::getResults)
                .block(Duration.ofMillis(timeout));
        } catch (Exception e) {
            log.error("Erreur product-service: {}", e.getMessage());
            throw new ExternalServiceException("product-service", "Erreur recherche produits");
        }
    }
}
```

### 4. Caching Recommandé
```java
@Cacheable(value = "product-search", key = "#query")
public List<Product> searchProducts(String query) {
    // Requête avec caching Redis (60 secondes)
}
```

---

## 📋 Order Service Integration

### 1. Health Check
```bash
curl http://order-service:8003/health
```

### 2. Get Order Status
```bash
curl -X GET "http://order-service:8003/api/orders/{orderId}?userId={userId}"
```

**Réponse attendue**:
```json
{
  "id": "order-12345",
  "userId": "user-123",
  "status": "shipped",
  "items": 3,
  "totalAmount": 299.99,
  "createdAt": "2026-04-10T10:30:00",
  "shippedAt": "2026-04-12T14:15:00",
  "estimatedDelivery": "2026-04-15"
}
```

### 3. Intégration dans ChatbotService

```java
public class OrderServiceClient {
    
    @Value("${services.order.url}")
    private String orderServiceUrl;
    
    @Value("${services.order.timeout}")
    private int timeout;
    
    @Retry(name = "order-service", delay = 1000)
    public OrderStatus getOrderStatus(String orderId, String userId) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId;
            return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(url)
                    .queryParam("userId", userId)
                    .build())
                .retrieve()
                .bodyToMono(OrderStatus.class)
                .block(Duration.ofMillis(timeout));
        } catch (WebClientException e) {
            log.error("Erreur order-service: {}", e.getMessage());
            throw new ExternalServiceException("order-service", "Service momentanément indisponible");
        }
    }
}
```

### 4. Security: Autorisation
```java
// S'assurer que l'utilisateur ne peut voir que ses commandes
public OrderStatus getOrderStatus(String orderId, String userId) {
    OrderStatus order = orderServiceClient.getOrderStatus(orderId, userId);
    
    if (!order.getUserId().equals(userId)) {
        throw new AccessDeniedException("Accès non autorisé à cette commande");
    }
    
    return order;
}
```

---

## 🤖 Gemini API Integration

### 1. Configuration
```yaml
google:
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: gemini-2.5-flash
    cache-enabled: true
    timeout-ms: 10000
```

### 2. Client Implémentation
```java
@Component
public class GeminiClient {
    
    @Value("${google.gemini.api-key}")
    private String apiKey;
    
    @Value("${google.gemini.model}")
    private String model;
    
    public String generateResponse(String prompt, String context) {
        try {
            // Appel à l'API Gemini
            String response = callGeminiAPI(prompt, context);
            
            log.info("Réponse Gemini générée - Tokens: {}", response.length());
            return response;
            
        } catch (Exception e) {
            log.error("Erreur Gemini API: {}", e.getMessage());
            throw new ExternalServiceException("gemini-api", "Erreur génération réponse");
        }
    }
    
    @Retry(name = "gemini")
    @CircuitBreaker(name = "gemini")
    private String callGeminiAPI(String prompt, String context) {
        // Implémentation avec WebClient ou RestTemplate
        return "...";
    }
}
```

### 3. Fallback
```java
private String generateResponseFallback(String prompt, String context) {
    // Réponse template si Gemini indisponible
    return """
        Je suis momentanément indisponible. 
        Veuillez réessayer dans quelques instants.
        """;
}
```

---

## 🔄 Circuit Breaker Configuration

Chaque service dépendant doit avoir une stratégie de résilience:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
      product-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
      order-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
      gemini:
        sliding-window-size: 5
        failure-rate-threshold: 30
```

### États du Circuit Breaker
```
CLOSED (Normal) → OPEN (Erreurs) → HALF_OPEN (Test) → CLOSED
```

---

## 📊 Monitoring des Intégrations

### Métriques à Tracker
```
- user-service.requests (total, errors, latency)
- product-service.requests (total, errors, latency)
- order-service.requests (total, errors, latency)
- gemini-api.requests (total, errors, latency, tokens)
- circuit-breaker.state (CLOSED, OPEN, HALF_OPEN)
```

### Prometheus Queries
```promql
# User service error rate
rate(http_client_requests_seconds_count{service="user-service",status=~"5.."}[5m])

# Product service latency P95
histogram_quantile(0.95, rate(http_client_requests_seconds_bucket{service="product-service"}[5m]))

# Circuit breaker state
resilience4j_circuitbreaker_state{name="user-service"}

# Gemini API token usage
increase(gemini_tokens_used_total[1h])
```

---

## 🔐 Security Best Practices

### 1. API Key Rotation
```bash
# Rotation tous les 90 jours
export GEMINI_API_KEY="new-key-here"
kubectl set env deployment/chatbot-service GEMINI_API_KEY=new-key-here -n microservices
```

### 2. Authentication Headers
```java
// Ajouter auth headers aux requêtes inter-services
private HttpHeaders getHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}
```

### 3. Request Validation
```java
// Valider responses des services
if (response == null || response.isEmpty()) {
    throw new ExternalServiceException("service-name", "Réponse invalide");
}
```

---

## 📝 Checklist d'Intégration

- [ ] Health check de chaque service
- [ ] Test de connectivité
- [ ] Circuit breaker configuré
- [ ] Retry policy définie
- [ ] Timeout configuré
- [ ] Fallback implémenté
- [ ] Logging des erreurs
- [ ] Monitoring des métriques
- [ ] Documentation des réponses
- [ ] Tests d'intégration

---

## 🚨 Troubleshooting

### Service Indisponible
```bash
# Vérifier DNS
nslookup user-service

# Vérifier le port
netstat -an | grep 8001

# Tester la connexion
curl http://user-service:8001/health
```

### Timeout
```bash
# Augmenter le timeout dans application.yml
services:
  user:
    timeout: 10000  # 10 secondes

# Vérifier la latence réseau
ping -c 5 user-service
```

### 404 Not Found
```bash
# Vérifier l'URL de l'endpoint
# Vérifier la version API
# Vérifier les paramètres de la requête
```

---

## 📞 Contacts Support

- **User Service**: #user-service on Slack
- **Product Service**: #product-service on Slack
- **Order Service**: #order-service on Slack
- **Gemini API**: https://ai.google.dev/support

---

**Dernière mise à jour**: 15 avril 2026

