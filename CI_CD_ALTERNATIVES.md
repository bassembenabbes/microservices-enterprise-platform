# 🔧 Solutions CI/CD Alternatives

Si vous ne souhaitez pas configurer Docker Hub ou préférez une autre approche, voici les alternatives.

## 1. ✅ Option par défaut - Build local sans push

**État actuel** : C'est ce qui est configuré maintenant

```yaml
✅ Les images se construisent systématiquement
✅ Le push Docker Hub est optionnel
✅ Pas de secrets requis
❌ Les images ne sont pas stockées pour réutilisation
```

**Avantages** :
- Pas de configuration nécessaire
- Tests les Dockerfile sans publier
- Validation de la capacité à construire les images

**Inconvénients** :
- Les images ne sont pas persistées
- À chaque workflow, reconstruction complète

## 2. 🐳 GitLab Registry (Gratuit, pas de compte requis)

Si vous migrez vers GitLab :

```yaml
# .gitlab-ci.yml
build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker build -t registry.gitlab.com/$CI_PROJECT_PATH/service:latest .
    - docker push registry.gitlab.com/$CI_PROJECT_PATH/service:latest
```

**Avantages** :
- Intégré nativement à GitLab
- Registe de conteneurs gratuit
- Pipeline CI/CD puissant

## 3. 🏗️ GitHub Container Registry (GHCR)

Alternative native à GitHub Actions :

```yaml
name: Build and Push to GHCR

on:
  push:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./backend/user-service
          push: true
          tags: ghcr.io/${{ github.repository }}/user-service:latest
```

**Avantages** :
- Authentification avec `GITHUB_TOKEN` (automatique)
- Registre gratuit inclus avec GitHub
- Pas de configuration supplémentaire
- Images privées par défaut

**Inconvénients** :
- Images liées à votre compte GitHub

## 4. 🏛️ Auto-hébergé - Registry Docker local

```yaml
name: Build and Push to Self-Hosted Registry

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Self-Hosted Registry
        uses: docker/login-action@v3
        with:
          registry: docker.example.com
          username: ${{ secrets.REGISTRY_USERNAME }}
          password: ${{ secrets.REGISTRY_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./backend/user-service
          push: true
          tags: docker.example.com/ecommerce/user-service:latest
```

**Avantages** :
- Contrôle total
- Pas de dépendance externe
- Sécurité améliorée

**Inconvénients** :
- Coût de serveur
- Maintenance requise

## 5. 💾 Amazon ECR (AWS)

```yaml
name: Build and Push to ECR

on:
  push:
    branches: [main]

env:
  AWS_REGION: us-east-1
  ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}

jobs:
  build:
    runs-on: ubuntu-latest

    permissions:
      id-token: write
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::ACCOUNT_ID:role/github-actions
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./backend/user-service
          push: true
          tags: |
            ${{ env.ECR_REGISTRY }}/ecommerce/user-service:latest
            ${{ env.ECR_REGISTRY }}/ecommerce/user-service:${{ github.sha }}
```

**Avantages** :
- Intégration AWS
- Sécurité IAM avancée
- Scalabilité

**Inconvénients** :
- Frais AWS possibles
- Configuration IAM complexe

## 6. 🎯 Azure Container Registry

```yaml
name: Build and Push to ACR

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Azure Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ secrets.ACR_LOGIN_SERVER }}
          username: ${{ secrets.ACR_USERNAME }}
          password: ${{ secrets.ACR_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: ./backend/user-service
          push: true
          tags: ${{ secrets.ACR_LOGIN_SERVER }}/ecommerce/user-service:latest
```

## Tableau Comparatif

| Solution | Coût | Configuration | Facilité | Recommandé |
|----------|------|---------------|----------|-----------|
| Build Local (Actuellement) | Gratuit | Aucune | ⭐⭐⭐⭐⭐ | ✅ Démarrage |
| Docker Hub | Gratuit* | Modérée | ⭐⭐⭐⭐ | ✅ Production |
| GHCR (GitHub) | Gratuit | Facile | ⭐⭐⭐⭐⭐ | ✅ Recommandé |
| GitLab | Gratuit | Facile | ⭐⭐⭐⭐ | ✅ Alternative |
| Auto-hébergé | Variable | Complexe | ⭐⭐⭐ | ⚠️ Avancé |
| AWS ECR | Payant | Complexe | ⭐⭐⭐ | ⚠️ Avancé |
| Azure ACR | Payant | Complexe | ⭐⭐⭐ | ⚠️ Avancé |

*Docker Hub offre des images publiques gratuites

## 🎯 Recommandation

Pour votre cas d'usage d'apprentissage/développement:

1. **Court terme** : Gardez le build local (configuration actuelle)
2. **Moyen terme** : Passez à **GitHub Container Registry** (GHCR)
3. **Long terme** : Si production, choisissez selon votre infrastructure

## 📝 Configuration GHCR (Recommandée)

La solution la plus simple est d'utiliser GHCR. Créez un fichier `.github/workflows/ci-ghcr.yml` :

```yaml
name: Build and Push to GHCR

on:
  push:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push User Service
        uses: docker/build-push-action@v5
        with:
          context: ./backend/user-service
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/ecommerce-user-service:latest
```

**Avantages** :
- Pas de secrets à configurer
- Authentification automatique
- Privé par défaut (sécurité)
- Gratuit
- Aucune configuration supplémentaire

Voulez-vous que je configure le workflow pour GHCR ?

## 🔗 Ressources

- [Docker Hub](https://hub.docker.com)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [GitLab CI/CD](https://docs.gitlab.com/ee/ci/)
- [AWS ECR](https://aws.amazon.com/ecr/)
- [Azure Container Registry](https://azure.microsoft.com/en-us/services/container-registry/)

