# 🔒 GUIDE DE SÉCURITÉ - CHATBOT-SERVICE

**Version**: 1.0.0  
**Date**: 15 avril 2026  
**Révisé par**: Équipe Backend  

---

## 📋 Table des matières

1. [Gestion des Secrets](#gestion-des-secrets)
2. [Authentification & Autorisation](#authentification--autorisation)
3. [Validation des Entrées](#validation-des-entrées)
4. [Communication Sécurisée](#communication-sécurisée)
5. [Gestion des Erreurs](#gestion-des-erreurs)
6. [Auditing & Logging](#auditing--logging)
7. [Dépendances & CVE](#dépendances--cve)
8. [Checklist de Déploiement](#checklist-de-déploiement)

---

## 🔐 Gestion des Secrets

### ✅ Bonnes Pratiques

**1. Jamais en Code**
```java
// ❌ MAUVAIS
String apiKey = "AIzaSyD3QdPN0H2PsA_jz-t8yTlXF5wbYaUszFI";

// ✅ BON
String apiKey = System.getenv("GEMINI_API_KEY");
```

**2. Utiliser des Variables d'Environnement**
```yaml
# application.yml
google:
  gemini:
    api-key: ${GEMINI_API_KEY:}  # REQUIS avant production
    model: ${GEMINI_MODEL:gemini-2.5-flash}
```

**3. Rotation Régulière**
- Clés API Gemini: Tous les 90 jours
- Mots de passe database: Tous les 30 jours
- Certificats TLS: Tous les 365 jours

### 🛠️ Implémentation

**Option 1: AWS Secrets Manager (Recommandé)**
```java
@Configuration
public class SecretsConfig {
    
    @Bean
    public String geminiApiKey() {
        // Auto-injected from AWS Secrets Manager
        return "from-secrets-manager";
    }
}
```

**Option 2: Vault**
```yaml
spring:
  cloud:
    vault:
      host: ${VAULT_HOST}
      token: ${VAULT_TOKEN}
      uri: https://${VAULT_HOST}:8200
```

**Option 3: Kubernetes Secrets**
```bash
kubectl create secret generic chatbot-secrets \
  --from-literal=gemini-api-key=xxxx \
  -n microservices
```

### 🚨 Audit Trail

```java
@Aspect
@Component
public class SecretAccessAudit {
    
    @Before("execution(* *..SecretsConfig.geminiApiKey(..))")
    public void auditSecretAccess(JoinPoint jp) {
        log.warn("Secret access attempt - User: {}, Time: {}", 
                SecurityContextHolder.getContext().getAuthentication(), 
                LocalDateTime.now());
    }
}
```

---

## 🔑 Authentification & Autorisation

### ⚠️ Problèmes Actuels

- ❌ Pas d'authentification sur les endpoints
- ❌ Pas de validation de l'utilisateur
- ❌ Pas d'autorisation (RBAC)

### ✅ Solutions Proposées

**1. Spring Security + JWT**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.3</version>
</dependency>
```

**2. Filter JWT**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractTokenFromRequest(request);
        
        if (token != null && validateToken(token)) {
            UserDetails user = loadUserFromToken(token);
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**3. Endpoint Sécurisé**
```java
@PostMapping("/chat")
@PreAuthorize("hasRole('USER')")  // Seulement utilisateurs autorisés
public ResponseEntity<ChatResponse> chat(
        @Valid @RequestBody ChatRequest request,
        @AuthenticationPrincipal UserDetails user) {
    
    // S'assurer que l'utilisateur ne peut accéder que ses données
    if (!request.getUserId().equals(user.getUsername())) {
        throw new AccessDeniedException("Unauthorized access");
    }
    
    return ResponseEntity.ok(chatbotService.processMessage(request));
}
```

### 🔐 Exemple d'Authentification

```bash
# 1. Login et obtenir token
curl -X POST http://localhost:8005/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "password": "password"
  }'

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

# 2. Utiliser le token
curl -X POST http://localhost:8005/api/v1/chatbots/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "userId": "user123",
    "sessionId": "session-456",
    "message": "Bonjour"
  }'
```

---

## ✅ Validation des Entrées

### ✅ Implémentation (DÉJÀ FAITE)

**DTOs avec Jakarta Validation:**
```java
@Data
public class ChatRequest {
    
    @NotBlank(message = "userId requis")
    private String userId;
    
    @NotBlank(message = "sessionId requis")
    private String sessionId;
    
    @NotBlank(message = "message requis")
    @Size(min = 1, max = 1000, message = "1-1000 caractères")
    private String message;
    
    @Valid  // Valider aussi les objets imbriqués
    private Map<String, Object> context;
}
```

### 🛡️ Protection Contre les Attaques

**1. SQL Injection**
```java
// ❌ MAUVAIS - Vulnérable
String query = "SELECT * FROM users WHERE id = " + userId;

// ✅ BON - Paramétrisé
repository.findById(userId);
```

**2. XSS (Cross-Site Scripting)**
```java
// ❌ MAUVAIS
response.append("<p>" + userMessage + "</p>");

// ✅ BON - Échapper le HTML
String escaped = HtmlUtils.htmlEscape(userMessage);
```

**3. CSRF (Cross-Site Request Forgery)**
```yaml
spring:
  security:
    csrf:
      enabled: true
      cookie:
        http-only: true
        same-site: Strict
```

---

## 🔐 Communication Sécurisée

### 🔒 HTTPS/TLS (OBLIGATOIRE)

**Production:**
```yaml
server:
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
    protocol: TLSv1.3
```

**Certificate Management:**
```bash
# Générer un certificat auto-signé (dev uniquement)
keytool -genkey -alias tomcat -storetype PKCS12 \
  -keyalg RSA -keysize 2048 \
  -keystore keystore.p12 \
  -validity 3650

# Utiliser Let's Encrypt en production
certbot certonly --standalone -d chatbot-service.example.com
```

### 🔗 Communication Inter-Services

**Avec MTls:**
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() throws Exception {
        // Charger le certificat client
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(
            new FileInputStream("client-keystore.p12"),
            "password".toCharArray()
        );
        
        return WebClient.builder()
            .clientConnector(
                new ReactorNettyClientRequestFactory()
                    // Ajouter config SSL/mTLS
            )
            .build();
    }
}
```

---

## 🚨 Gestion des Erreurs

### ✅ Implémentation (DÉJÀ FAITE)

**1. Pas de Détails Sensibles au Client**
```java
// ❌ MAUVAIS - Expose stack trace
{
  "error": "NullPointerException at ChatbotService.java:123",
  "stackTrace": "..."
}

// ✅ BON - Message générique
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "Une erreur inattendue s'est produite",
  "timestamp": "2026-04-15T10:30:00"
}
```

**2. Logging Sécurisé**
```java
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Log le stack trace complet EN INTERNE
        log.error("Exception interne complète:", ex);
        
        // Retourner un message générique au CLIENT
        ErrorResponse response = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Une erreur inattendue s'est produite",
            "/api/v1/chatbots/chat",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
```

### 🔴 Erreurs Fréquentes à Éviter

```java
// ❌ Leak d'info système
throw new Exception("Database connection to postgres:5432 failed");

// ✅ Message générique
throw new ExternalServiceException(
    "database", 
    "Service temporarily unavailable"
);
```

---

## 📊 Auditing & Logging

### 🔍 Audit Trail

```java
@Aspect
@Component
@Slf4j
public class AuditAspect {
    
    @Before("@annotation(Audit)")
    public void auditMethod(JoinPoint jp) {
        String user = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String action = jp.getSignature().getName();
        
        log.info("AUDIT: User={}, Action={}, Time={}, Args={}", 
                user, action, LocalDateTime.now(), jp.getArgs());
    }
}

// Utilisation
@Service
public class ChatbotService {
    
    @Audit
    public ChatResponse processMessage(ChatRequest request) {
        // Automatiquement loggé pour audit
        return ...;
    }
}
```

### 📋 Informations à Logger

```java
log.info("SECURITY_EVENT: " +
    "User={}, " +
    "Action={}, " +
    "Resource={}, " +
    "Result={}, " +
    "Timestamp={}, " +
    "IP={}, " +
    "UserAgent={}",
    userId,
    action,
    resourceId,
    result,
    LocalDateTime.now(),
    request.getRemoteAddr(),
    request.getHeader("User-Agent")
);
```

### ⚠️ Ne JAMAIS logger

```java
// ❌ NE JAMAIS logger les secrets
log.info("API Key: " + apiKey);
log.info("Password: " + password);
log.info("Token: " + jwtToken);

// ✅ Logger seulement les métadonnées
log.info("Authentication attempt for user: {}", username);
```

---

## 🔍 Dépendances & CVE

### 📦 Vérifier les CVE

```bash
# Utiliser OWASP Dependency-Check
mvn org.owasp:dependency-check-maven:check

# Ou npm audit pour dépendances JS
npm audit

# Mise à jour des dépendances
mvn versions:display-dependency-updates
mvn versions:use-latest-versions
```

### 🛡️ Dépendances Recommandées

**À Ajouter:**
```xml
<!-- Audit & Scanning -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.1</version>
</plugin>

<!-- Code Quality -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

### 🔄 Processus de Mise à Jour

```bash
# 1. Vérifier les mises à jour
mvn org.apache.maven.plugins:maven-versions-plugin:display-dependency-updates

# 2. Tester les dépendances
mvn clean test

# 3. Vérifier les CVE
mvn org.owasp:dependency-check-maven:check

# 4. Mettre à jour pom.xml
mvn versions:update-properties -DincludesList="*:*" -DallowMajorUpdates=false

# 5. Retester
mvn clean test

# 6. Merger en production après review
```

---

## ✅ Checklist de Déploiement

### Avant Déploiement

- [ ] **Secrets**
  - [ ] Clé API Gemini externalisée (pas en code)
  - [ ] Database password changé
  - [ ] Certificats SSL générés et validés
  - [ ] Tokens JWT signés avec secret robuste

- [ ] **Configuration**
  - [ ] Profile de production activé (`spring.profiles.active=prod`)
  - [ ] Logs en INFO level (pas DEBUG)
  - [ ] CORS restreint aux domaines autorisés
  - [ ] CSRF protection activée

- [ ] **Sécurité**
  - [ ] HTTPS/TLS configuré
  - [ ] Validation des inputs complète
  - [ ] Exception handling sans leaks
  - [ ] Authentication/Authorization implémentée
  - [ ] Audit logging actif

- [ ] **Qualité**
  - [ ] Tests passent (>80% coverage)
  - [ ] SonarQube passing
  - [ ] Pas de CVE critiques
  - [ ] Code review approuvée

- [ ] **Infra**
  - [ ] Database migratée et vérifiée
  - [ ] Redis configuré et testés
  - [ ] Health checks fonctionnent
  - [ ] Monitoring & alerting en place

### Après Déploiement

- [ ] Vérifier les logs pour erreurs
- [ ] Tester l'authentification
- [ ] Vérifier les métriques Prometheus
- [ ] Tester le failover
- [ ] Confirmer l'audit logging fonctionne

---

## 📞 Contacts

- **Security Team**: security@example.com
- **Slack**: #security-incidents
- **On-Call**: [Rotation On-Call]

---

**Status**: 📋 Production Ready  
**Last Updated**: 15 avril 2026  
**Next Security Audit**: Trimestrial

