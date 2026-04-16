#!/bin/bash

# 🧪 Script de vérification du workflow CI/CD
# Ce script vérifie que le workflow est correctement configuré

echo "======================================"
echo "🔍 VÉRIFICATION DU WORKFLOW CI/CD"
echo "======================================"
echo ""

# Couleurs pour l'output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Compteurs
PASS=0
FAIL=0
WARN=0

# Fonction pour afficher les résultats
check_pass() {
    echo -e "${GREEN}✅ PASS${NC}: $1"
    ((PASS++))
}

check_fail() {
    echo -e "${RED}❌ FAIL${NC}: $1"
    ((FAIL++))
}

check_warn() {
    echo -e "${YELLOW}⚠️  WARN${NC}: $1"
    ((WARN++))
}

# 1. Vérifier que le fichier de workflow existe
echo "1️⃣  Vérification des fichiers..."
if [ -f ".github/workflows/ci.yml" ]; then
    check_pass "Fichier .github/workflows/ci.yml trouvé"
else
    check_fail "Fichier .github/workflows/ci.yml manquant"
fi

# 2. Vérifier que le fichier contient les bonnes sections
echo ""
echo "2️⃣  Vérification du contenu du workflow..."

if grep -q "name: CI" .github/workflows/ci.yml; then
    check_pass "Le workflow est nommé 'CI'"
else
    check_fail "Le workflow doit être nommé 'CI'"
fi

if grep -q "test-backend:" .github/workflows/ci.yml; then
    check_pass "Job 'test-backend' trouvé"
else
    check_fail "Job 'test-backend' manquant"
fi

if grep -q "test-frontend:" .github/workflows/ci.yml; then
    check_pass "Job 'test-frontend' trouvé"
else
    check_fail "Job 'test-frontend' manquant"
fi

if grep -q "build-images:" .github/workflows/ci.yml; then
    check_pass "Job 'build-images' trouvé"
else
    check_fail "Job 'build-images' manquant"
fi

# 3. Vérifier les conditions de secrets
echo ""
echo "3️⃣  Vérification des conditions Docker Hub..."

if grep -q "if: secrets.DOCKER_USERNAME != null && secrets.DOCKER_PASSWORD != null" .github/workflows/ci.yml; then
    check_pass "Conditions Docker optionnelles correctement configurées"
else
    check_fail "Conditions Docker optionnelles manquantes"
fi

if grep -q "Log in to Docker Hub (if credentials available)" .github/workflows/ci.yml; then
    check_pass "Étape de login Docker Hub conditionnelle"
else
    check_fail "Étape de login Docker Hub doit être conditionnelle"
fi

# 4. Vérifier les services testés
echo ""
echo "4️⃣  Vérification des services testés..."

services=("chatbot-service" "order-service")
for service in "${services[@]}"; do
    if grep -q "cd backend/$service" .github/workflows/ci.yml; then
        check_pass "Service $service testé"
    else
        check_fail "Service $service non testé"
    fi
done

# 5. Vérifier les services construits
echo ""
echo "5️⃣  Vérification des services construits..."

docker_services=(
    "User Service"
    "Product Service"
    "Order Service"
    "Chatbot Service"
    "API Gateway"
    "Frontend"
    "Notification Service"
)

for service in "${docker_services[@]}"; do
    if grep -q "Build $service" .github/workflows/ci.yml; then
        check_pass "Image Docker pour $service"
    else
        check_fail "Image Docker pour $service manquante"
    fi
done

# 6. Vérifier le cache Maven
echo ""
echo "6️⃣  Vérification de l'optimisation..."

if grep -q "Cache Maven packages" .github/workflows/ci.yml; then
    check_pass "Cache Maven configuré"
else
    check_warn "Cache Maven non configuré (optionnel)"
fi

if grep -q "setup-node" .github/workflows/ci.yml; then
    check_pass "Node.js 18 configuré"
else
    check_fail "Node.js non configuré"
fi

if grep -q "cache: 'npm'" .github/workflows/ci.yml; then
    check_pass "Cache npm configuré"
else
    check_warn "Cache npm non configuré (optionnel)"
fi

# 7. Vérifier les dépendances entre jobs
echo ""
echo "7️⃣  Vérification des dépendances..."

if grep -q "needs: \[test-backend, test-frontend\]" .github/workflows/ci.yml; then
    check_pass "Job 'build-images' dépend des tests"
else
    check_fail "Les dépendances entre jobs ne sont pas correctes"
fi

# 8. Résumé
echo ""
echo "======================================"
echo "📊 RÉSUMÉ"
echo "======================================"
echo -e "${GREEN}✅ Succès: $PASS${NC}"
echo -e "${RED}❌ Erreurs: $FAIL${NC}"
echo -e "${YELLOW}⚠️  Avertissements: $WARN${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 Workflow CI/CD correctement configuré!${NC}"
    echo ""
    echo "Prochaines étapes:"
    echo "1. Committez les changements:"
    echo "   git add .github/workflows/ci.yml"
    echo "   git add *.md"
    echo "   git commit -m 'fix: make Docker Hub push optional in CI/CD'"
    echo ""
    echo "2. (Optionnel) Configurez les secrets Docker Hub:"
    echo "   - Consultez GITHUB_SECRETS_SETUP.md"
    echo ""
    echo "3. (Ou) Utilisez GitHub Container Registry (GHCR):"
    echo "   - Consultez CI_CD_ALTERNATIVES.md"
    echo ""
    exit 0
else
    echo -e "${RED}⚠️  Des erreurs ont été détectées.${NC}"
    echo "Veuillez corriger les problèmes identifiés ci-dessus."
    exit 1
fi

