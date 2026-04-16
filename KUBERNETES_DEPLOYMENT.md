# 🚀 Déploiement Professionnel E-commerce - Kubernetes + Helm + ArgoCD

## 📋 Vue d'ensemble

Ce guide vous explique comment déployer votre plateforme e-commerce complète sur Kubernetes en utilisant Helm pour la gestion des packages et ArgoCD pour le GitOps.

## 🏗️ Architecture Déployée

```
Internet
    ↓
[NGINX Ingress Controller]
    ↓
├── Frontend (React + Nginx)
├── API Gateway (FastAPI)
├── User Service (FastAPI + PostgreSQL)
├── Product Service (Express + PostgreSQL)
├── Order Service (Spring Boot + PostgreSQL)
├── Chatbot Service (Spring Boot + Gemini AI)
├── Notification Service (FastAPI + Kafka)
├── N8N Workflow Engine (Automation)
├── Monitoring Stack:
│   ├── Prometheus (Metrics)
│   ├── Grafana (Dashboards)
│   └── Elasticsearch + Kibana (Logs)
├── Message Queue:
│   └── Kafka + Zookeeper
└── Cache:
    └── Redis
```

## 🎯 Prérequis

### Cluster Kubernetes
- **Local**: minikube, k3s, kind
- **Cloud**: EKS (AWS), GKE (GCP), AKS (Azure)
- **Version**: 1.24+

### Outils
- `kubectl` configuré pour accéder au cluster
- `helm` 3.x installé
- `git` pour cloner le repository

### Ressources Requises
- **CPU**: 4+ cores
- **RAM**: 8GB+ minimum, 16GB+ recommandé
- **Storage**: 50GB+ pour les bases de données

## 🚀 Déploiement Rapide

### Option 1: Script Automatisé (Recommandé)

```bash
# Rendre le script exécutable
chmod +x deploy.sh

# Déploiement avec Helm
./deploy.sh helm

# Ou pour la production
./deploy.sh helm values-production.yaml

# Ou avec ArgoCD
./deploy.sh argocd

# Ou les deux
./deploy.sh both values-production.yaml
```

### Option 2: Déploiement Manuel

#### 1. Préparer l'environnement
```bash
# Créer le namespace
kubectl create namespace ecommerce

# Installer NGINX Ingress
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install nginx-ingress ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace
```

#### 2. Déployer l'application
```bash
# Aller dans le répertoire du chart
cd helm/ecommerce

# Déployer avec les valeurs par défaut
helm install ecommerce . -n ecommerce

# Ou avec les valeurs de production
helm install ecommerce . -n ecommerce -f values-production.yaml
```

#### 3. Vérifier le déploiement
```bash
# Vérifier les pods
kubectl get pods -n ecommerce

# Vérifier les services
kubectl get services -n ecommerce

# Vérifier l'Ingress
kubectl get ingress -n ecommerce
```

## 🔧 Configuration

### Secrets et API Keys

Avant le déploiement en production, configurez les secrets :

```bash
# Créer les secrets Kubernetes
kubectl create secret generic ecommerce-secrets \
  --from-literal=gemini-api-key="votre_clé_gemini" \
  --from-literal=db-password="mot_de_passe_db" \
  --from-literal=redis-password="mot_de_passe_redis" \
  -n ecommerce
```

### Variables d'environnement

Modifiez `helm/ecommerce/values-production.yaml` :

```yaml
# Domaine personnalisé
ingress:
  hosts:
    - host: ecommerce.votre-domaine.com

# Mots de passe sécurisés
postgresql:
  user:
    password: "mot_de_passe_très_sécurisé"
  product:
    password: "mot_de_passe_très_sécurisé"
  order:
    password: "mot_de_passe_très_sécurisé"
  vector:
    password: "mot_de_passe_très_sécurisé"

# API Key Gemini
chatbotService:
  geminiApiKey: "votre_clé_api_gemini"

# Mot de passe Grafana
grafana:
  adminPassword: "mot_de_passe_admin_grafana"
```

## 📊 Services Exposés

Après déploiement, accédez aux services via :

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | `http://ecommerce.local/` | Interface utilisateur React |
| API Gateway | `http://ecommerce.local/api/` | Point d'entrée API |
| Grafana | `http://ecommerce.local/grafana/` | Dashboards de monitoring |
| Prometheus | `http://ecommerce.local/prometheus/` | Métriques système |
| N8N | `http://ecommerce.local/n8n/` | Moteur de workflows |
| Kibana | `http://ecommerce.local/kibana/` | Interface de logs |

## 🎯 GitOps avec ArgoCD

### Installation d'ArgoCD

```bash
# Créer le namespace
kubectl create namespace argocd

# Installer ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Attendre que les pods soient prêts
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd

# Récupérer le mot de passe admin
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### Configuration de l'Application

```bash
# Appliquer la configuration ArgoCD
kubectl apply -f argocd/ecommerce-application.yaml

# Accéder à l'interface ArgoCD
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Se connecter avec admin/[mot_de_passe_récupéré]
```

### Synchronisation Automatique

ArgoCD surveillera automatiquement les changements dans le repository Git et mettra à jour le déploiement.

## 📈 Monitoring & Observabilité

### Grafana
- **URL**: `http://ecommerce.local/grafana/`
- **User**: `admin`
- **Password**: Configuré dans `values.yaml`

### Prometheus
- **URL**: `http://ecommerce.local/prometheus/`
- Métriques collectées automatiquement pour tous les services

### Logs Centralisés
- **Kibana**: `http://ecommerce.local/kibana/`
- Tous les logs des applications sont envoyés à Elasticsearch

## 🔄 Mise à Jour

### Via Helm
```bash
# Mise à jour des images
cd helm/ecommerce
helm upgrade ecommerce . -n ecommerce

# Mise à jour avec rollback possible
helm upgrade ecommerce . -n ecommerce --dry-run  # Test
helm upgrade ecommerce . -n ecommerce            # Appliquer
```

### Via ArgoCD
Les mises à jour sont automatiques lors des commits sur la branche principale.

## 🚨 Dépannage

### Pods en erreur
```bash
# Voir les logs détaillés
kubectl describe pod <pod-name> -n ecommerce
kubectl logs <pod-name> -n ecommerce --previous

# Redémarrer un déploiement
kubectl rollout restart deployment <deployment-name> -n ecommerce
```

### Problèmes de connectivité
```bash
# Vérifier les services
kubectl get endpoints -n ecommerce

# Vérifier l'Ingress
kubectl describe ingress ecommerce-ingress -n ecommerce
```

### Problèmes de stockage
```bash
# Vérifier les PVCs
kubectl get pvc -n ecommerce

# Vérifier les PVs
kubectl get pv
```

## 🔐 Sécurité

### Recommandations
- Activez RBAC sur le cluster
- Utilisez des certificats TLS
- Changez tous les mots de passe par défaut
- Scannez les images Docker pour les vulnérabilités
- Utilisez Network Policies pour isoler les services

### Secrets Management
```yaml
# Exemple de configuration sécurisée
apiVersion: v1
kind: Secret
metadata:
  name: ecommerce-secrets
type: Opaque
data:
  gemini-api-key: <base64-encoded>
  db-password: <base64-encoded>
  redis-password: <base64-encoded>
```

## 🎯 Performance

### Ressources par Défaut
- **Frontend**: 128Mi RAM, 100m CPU
- **API Gateway**: 256Mi RAM, 200m CPU
- **Services métier**: 512Mi RAM, 250m CPU
- **Chatbot**: 1Gi RAM, 500m CPU
- **Bases de données**: 2Gi RAM, variable CPU

### Scaling Horizontal
```bash
# Auto-scaling basé sur CPU
kubectl autoscale deployment ecommerce-api-gateway \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n ecommerce
```

## 📚 Documentation Détaillée

- **[Helm Chart README](helm/ecommerce/README.md)** - Guide complet Helm
- **[CI/CD Documentation](CI_CD_WORKFLOW.md)** - Pipeline CI/CD
- **[ArgoCD Setup](argocd/)** - Configuration GitOps

## 🧹 Nettoyage

```bash
# Supprimer l'application
helm uninstall ecommerce -n ecommerce

# Supprimer ArgoCD (optionnel)
kubectl delete -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Supprimer les namespaces
kubectl delete namespace ecommerce argocd ingress-nginx

# Supprimer les PVCs (⚠️ données perdues)
kubectl delete pvc --all -n ecommerce
```

## 🎉 Succès !

Votre plateforme e-commerce est maintenant déployée professionnellement avec :

✅ **Kubernetes** pour l'orchestration
✅ **Helm** pour la gestion des packages
✅ **ArgoCD** pour le GitOps
✅ **Monitoring complet** (Prometheus + Grafana)
✅ **Logs centralisés** (ELK Stack)
✅ **Workflows automatisés** (N8N)
✅ **Message queuing** (Kafka)
✅ **Cache distribué** (Redis)
✅ **Bases de données** (PostgreSQL)

**🚀 Prêt pour la production !**

---

**Besoin d'aide ?** Consultez les documentations détaillées ou ouvrez une issue sur GitHub.
