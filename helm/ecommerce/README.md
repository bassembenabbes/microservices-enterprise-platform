# 🚀 Déploiement E-commerce avec Helm & Kubernetes

## 📋 Prérequis

- Kubernetes cluster (minikube, k3s, EKS, GKE, AKS)
- Helm 3.x installé
- kubectl configuré
- NGINX Ingress Controller installé

## 🛠️ Installation

### 1. Ajouter le repo Helm (optionnel)
```bash
helm repo add ecommerce https://github.com/bassembenabbes/ecommerce
helm repo update
```

### 2. Installer NGINX Ingress Controller
```bash
# Avec Helm
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install nginx-ingress ingress-nginx/ingress-nginx

# Ou avec kubectl
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

### 3. Créer un namespace
```bash
kubectl create namespace ecommerce
```

### 4. Déployer l'application
```bash
# Depuis le répertoire du chart
cd helm/ecommerce

# Installation avec valeurs par défaut
helm install ecommerce . -n ecommerce

# Ou avec des valeurs personnalisées
helm install ecommerce . -n ecommerce -f values-production.yaml
```

## 🔧 Configuration

### Variables d'environnement importantes

Créez un fichier `values-production.yaml` :

```yaml
# API Keys
chatbotService:
  geminiApiKey: "votre_clé_gemini_ici"

# Base de données
postgresql:
  user:
    password: "mot_de_passe_sécurisé"
  product:
    password: "mot_de_passe_sécurisé"
  order:
    password: "mot_de_passe_sécurisé"
  vector:
    password: "mot_de_passe_sécurisé"

# Redis
redis:
  password: "mot_de_passe_redis"

# Ingress
ingress:
  hosts:
    - host: votre-domaine.com
      paths:
        - path: /
          pathType: Prefix
          service:
            name: frontend
            port: 80
        - path: /api
          pathType: Prefix
          service:
            name: api-gateway
            port: 8000

# TLS (optionnel)
ingress:
  tls:
    - secretName: ecommerce-tls
      hosts:
        - votre-domaine.com
```

### Secrets Kubernetes

```bash
# Créer les secrets pour les API keys
kubectl create secret generic ecommerce-secrets \
  --from-literal=gemini-api-key="votre_clé_gemini" \
  --from-literal=db-password="mot_de_passe_db" \
  --from-literal=redis-password="mot_de_passe_redis" \
  -n ecommerce
```

## 📊 Services exposés

Après déploiement, les services sont accessibles via l'Ingress :

- **Frontend**: `http://votre-domaine.com/`
- **API Gateway**: `http://votre-domaine.com/api/`
- **Grafana**: `http://votre-domaine.com/grafana/`
- **Prometheus**: `http://votre-domaine.com/prometheus/`
- **N8N**: `http://votre-domaine.com/n8n/`
- **Kibana**: `http://votre-domaine.com/kibana/`

## 🔍 Vérification du déploiement

### 1. Vérifier les pods
```bash
kubectl get pods -n ecommerce
```

### 2. Vérifier les services
```bash
kubectl get services -n ecommerce
```

### 3. Vérifier l'Ingress
```bash
kubectl get ingress -n ecommerce
```

### 4. Logs des services
```bash
# API Gateway
kubectl logs -f deployment/ecommerce-api-gateway -n ecommerce

# Frontend
kubectl logs -f deployment/ecommerce-frontend -n ecommerce

# User Service
kubectl logs -f deployment/ecommerce-user-service -n ecommerce
```

### 5. Test de connectivité
```bash
# Test API Gateway
curl http://votre-domaine.com/api/health

# Test Frontend
curl http://votre-domaine.com/
```

## 📈 Monitoring & Observabilité

### Grafana
- URL: `http://votre-domaine.com/grafana/`
- User: `admin`
- Password: `admin` (configurable dans values.yaml)

### Prometheus
- URL: `http://votre-domaine.com/prometheus/`
- Métriques disponibles pour tous les services

### Kibana (Logs)
- URL: `http://votre-domaine.com/kibana/`
- Connecté à Elasticsearch pour l'analyse des logs

## 🔄 Mise à jour

### Mise à jour des images
```bash
# Mettre à jour les tags dans values.yaml
# Puis upgrade
helm upgrade ecommerce . -n ecommerce
```

### Rolling update
```bash
kubectl rollout restart deployment -n ecommerce
```

## 🧹 Nettoyage

### Supprimer le déploiement
```bash
helm uninstall ecommerce -n ecommerce
kubectl delete namespace ecommerce
```

### Supprimer les PVCs (attention: données perdues)
```bash
kubectl delete pvc --all -n ecommerce
```

## 🚨 Dépannage

### Pods en CrashLoopBackOff
```bash
kubectl describe pod <pod-name> -n ecommerce
kubectl logs <pod-name> -n ecommerce
```

### Problèmes de connectivité
```bash
# Vérifier les services
kubectl get endpoints -n ecommerce

# Vérifier l'Ingress
kubectl describe ingress ecommerce-ingress -n ecommerce
```

### Problèmes de base de données
```bash
# Vérifier les PVCs
kubectl get pvc -n ecommerce

# Vérifier les PVs
kubectl get pv
```

## 📚 Architecture

```
Internet
    ↓
[NGINX Ingress]
    ↓
├── Frontend (React)
├── API Gateway (FastAPI)
├── User Service (FastAPI)
├── Product Service (Express)
├── Order Service (Spring Boot)
├── Chatbot Service (Spring Boot)
├── Notification Service (FastAPI)
├── N8N (Workflow Engine)
├── Grafana (Monitoring)
├── Prometheus (Metrics)
├── Kibana (Logs)
└── Elasticsearch (Search)
```

## 🔐 Sécurité

### Recommandations
- Utilisez des certificats TLS
- Changez les mots de passe par défaut
- Utilisez des secrets Kubernetes pour les credentials
- Activez RBAC
- Utilisez Network Policies
- Scannez les images pour les vulnérabilités

### Secrets à configurer
```yaml
# Dans values.yaml ou via --set
chatbotService.geminiApiKey: "your_secure_key"
postgresql.*.password: "secure_passwords"
redis.password: "secure_redis_password"
```

## 🎯 Performance

### Ressources recommandées (Production)
```yaml
# Services principaux
userService.resources.limits.memory: "1Gi"
productService.resources.limits.memory: "1Gi"
orderService.resources.limits.memory: "1Gi"
chatbotService.resources.limits.memory: "2Gi"

# Bases de données
postgresql.*.resources.limits.memory: "2Gi"
redis.resources.limits.memory: "1Gi"
elasticsearch.resources.limits.memory: "4Gi"
```

### Scaling
```bash
# Horizontal Pod Autoscaler
kubectl autoscale deployment ecommerce-api-gateway --cpu-percent=70 --min=2 --max=10 -n ecommerce
```

## 📞 Support

Pour les problèmes :
1. Vérifiez les logs des pods
2. Consultez la documentation
3. Ouvrez une issue sur GitHub

---

**🎉 Déploiement réussi !**

Votre plateforme e-commerce est maintenant déployée sur Kubernetes avec Helm.
