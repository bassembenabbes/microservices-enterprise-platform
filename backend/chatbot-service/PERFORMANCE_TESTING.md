# Performance Testing Guide - Chatbot Service

## 📊 Outils Recommandés

### 1. Apache JMeter (GUI + CLI)
```bash
# Installation
wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.2.tgz
tar -xzf apache-jmeter-5.6.2.tgz
cd apache-jmeter-5.6.2/bin

# Lancer en GUI
./jmeter.sh

# Ou CLI
./jmeter.sh -n -t plan.jmx -l results.jtl -e -o report/
```

### 2. K6 (JavaScript-based)
```bash
# Installation
brew install k6

# Exécuter
k6 run script.js
```

### 3. Apache Bench (ab)
```bash
# Test simple
ab -n 1000 -c 10 http://localhost:8005/api/chatbots/health
```

---

## 🔧 Scénarios de Test

### Scénario 1: Chat Basique (Smoke Test)
```bash
# Test rapide: 10 utilisateurs, 1 minute
# Vérifie que le service fonctionne
ab -n 100 -c 10 \
  -T "application/json" \
  -p request.json \
  http://localhost:8005/api/chatbots/chat
```

Fichier `request.json`:
```json
{
  "userId": "test-user",
  "sessionId": "test-session",
  "message": "Bonjour"
}
```

### Scénario 2: Charge Progressive (Ramp-up)
```yaml
# K6 script: load-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 50,
  duration: '5m',
  ramp: {
    'http://localhost:8005/api/chatbots/chat': {
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },   // Ramp-up à 10 users
        { duration: '2m', target: 50 },   // Ramp-up à 50 users
        { duration: '1m', target: 10 },   // Ramp-down à 10 users
        { duration: '1m', target: 0 },    // Stop
      ],
    }
  }
};

export default function () {
  let url = 'http://localhost:8005/api/chatbots/chat';
  let payload = JSON.stringify({
    userId: `user-${Math.random()}`,
    sessionId: `session-${Math.random()}`,
    message: 'Test message'
  });

  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '10s'
  };

  let res = http.post(url, payload, params);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has response': (r) => r.body.length > 0
  });
}
```

Lancer:
```bash
k6 run --vus 50 --duration 5m load-test.js
```

### Scénario 3: Stress Testing
```yaml
# stress-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp-up
    { duration: '5m', target: 200 },   // Stress
    { duration: '2m', target: 0 },     // Stop
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // P95 < 1s
    'http_req_duration{staticAsset:yes}': ['p(99)<300'],
    http_req_failed: ['rate<0.1'],  // <10% errors
  }
};

export default function () {
  // Test logic
}
```

---

## 📈 Métriques Clés

| Métrique | Cible | Seuil d'alerte |
|----------|-------|----------------|
| **Response Time (P50)** | <200ms | >300ms |
| **Response Time (P95)** | <500ms | >700ms |
| **Response Time (P99)** | <1s | >1.5s |
| **Error Rate** | <0.5% | >1% |
| **Throughput** | >100 req/s | <50 req/s |
| **CPU** | <70% | >85% |
| **Memory** | <70% | >85% |
| **DB Connections** | <80% | >90% |

---

## 🚀 Processus de Test

### Avant Production
```bash
# 1. Smoke test (10 min)
k6 run smoke-test.js

# 2. Load test (30 min)
k6 run --vus 100 --duration 30m load-test.js

# 3. Stress test (15 min)
k6 run stress-test.js

# 4. Soak test (2-4 hours)
k6 run --vus 50 --duration 4h soak-test.js
```

### Analyse des Résultats
```bash
# Créer rapport JMeter
jmeter -n -t plan.jmx -l results.jtl -e -o report/

# Visualiser
open report/index.html
```

---

## 🔍 Cas d'Usage Spécifiques

### Test Product Search Intent
```json
{
  "userId": "user-123",
  "sessionId": "session-456",
  "message": "Je cherche un iPhone"
}
```

### Test Order Status Intent
```json
{
  "userId": "user-123",
  "sessionId": "session-456",
  "message": "Quel est le statut de ma commande #12345?"
}
```

### Test Long Message
```json
{
  "userId": "user-123",
  "sessionId": "session-456",
  "message": "Lorem ipsum dolor sit amet, consectetur adipiscing elit..."
}
```

---

## 📊 Interprétation des Résultats

### ✅ Résultats Acceptables
```
Response Time P95: 450ms ✅
Error Rate: 0.1% ✅
Throughput: 150 req/s ✅
Success Rate: 99.9% ✅
```

### ⚠️ Résultats À Investiguer
```
Response Time P95: 750ms ⚠️
Error Rate: 1.5% ⚠️
Throughput: 80 req/s ⚠️
5xx Errors: 50+ ⚠️
```

### 🔴 Échec du Test
```
Response Time P95: >1s 🔴
Error Rate: >5% 🔴
Throughput: <50 req/s 🔴
Pod restarts detected 🔴
```

---

## 🔧 Optimisations Basées sur Résultats

### Si Latence Élevée
1. Vérifier Redis fonctionne
2. Vérifier CPU/Memory sur le pod
3. Augmenter pool de connexions DB
4. Analyser Gemini API response times
5. Optimiser requêtes DB

### Si Error Rate Élevé
1. Vérifier les logs (erreurs)
2. Vérifier dépendances externes
3. Vérifier limite de connexions
4. Vérifier rate limiting
5. Vérifier secrets/credentials

### Si Throughput Faible
1. Augmenter replicas Kubernetes
2. Vérifier affinity rules
3. Vérifier resource limits
4. Augmenter pool size Hikari
5. Optimiser requêtes

---

## 📝 Checklist de Test

- [ ] Smoke test PASS
- [ ] Load test PASS (100 users, 30 min)
- [ ] Stress test PASS (200 users, 5 min)
- [ ] Soak test PASS (50 users, 4h) - optional
- [ ] Error rate <0.5%
- [ ] P95 latency <500ms
- [ ] No pod restarts
- [ ] Memory stable
- [ ] CPU <70%
- [ ] Document résultats

---

## 📋 Rapports

Après chaque test, générer un rapport:
```bash
# JMeter
jmeter -n -t plan.jmx -l results.jtl -e -o report/

# K6
k6 run -o json=results.json script.js
```

Inclure:
- Response time distribution
- Error breakdown
- Throughput graph
- Resource usage
- Recommendations

