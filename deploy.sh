#!/bin/bash

# 🚀 Script de déploiement E-commerce PRO (Helm + ArgoCD)

set -e

# ================= COLORS =================
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ================= LOG =================
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ================= PREREQUISITES =================
check_prerequisites() {
    log_info "Vérification des prérequis..."

    command -v kubectl >/dev/null || { log_error "kubectl manquant"; exit 1; }
    command -v helm >/dev/null || { log_error "helm manquant"; exit 1; }

    kubectl cluster-info >/dev/null || {
        log_error "Cluster Kubernetes inaccessible"
        exit 1
    }

    log_success "Prérequis OK"
}

# ================= NAMESPACE =================
create_namespace() {
    log_info "Création namespace ecommerce..."
    kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -
    log_success "Namespace prêt"
}

# ================= SECRETS =================
create_secrets() {
    log_info "Création des secrets..."

    kubectl create secret generic app-secrets \
        --from-literal=DB_PASSWORD=password \
        --from-literal=GEMINI_API_KEY=your_api_key \
        -n ecommerce \
        --dry-run=client -o yaml | kubectl apply -f -

    log_success "Secrets créés"
}
cleanup_ingress() {
    kubectl delete validatingwebhookconfiguration ingress-nginx-admission || true
    kubectl delete mutatingwebhookconfiguration ingress-nginx-admission || true
}
# ================= INGRESS =================
install_ingress_controller() {
    log_info "Ensuring clean ingress state..."

    # check if real k8s resources exist
    if kubectl get deployment -n ingress-nginx ingress-nginx-controller >/dev/null 2>&1; then
        log_warning "K8s resources exist → forcing cleanup"

        helm uninstall ingress-nginx -n ingress-nginx || true
        kubectl delete namespace ingress-nginx --ignore-not-found
    fi

    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
    helm repo update

    helm install ingress-nginx ingress-nginx/ingress-nginx \
        -n ingress-nginx \
        --create-namespace \
        --timeout 10m \
        --wait \
        --atomic
}


# ================= HELM =================
deploy_with_helm() {
    local values_file=${1:-"values.yaml"}

    log_info "Déploiement Helm..."

    cd helm/ecommerce

    helm lint .

    if helm list -n ecommerce | grep -q ecommerce; then
        log_info "Upgrade..."
        helm upgrade ecommerce . -n ecommerce -f $values_file --wait || {
            log_error "Erreur Helm → rollback"
            helm rollback ecommerce
            exit 1
        }
    else
        log_info "Install..."
        helm install ecommerce . -n ecommerce -f $values_file --wait
    fi

    cd ../..
    log_success "Helm OK"
}

# ================= ARGOCD =================
deploy_with_argocd() {
    log_info "Déploiement ArgoCD..."

    if ! kubectl get namespace argocd >/dev/null 2>&1; then
        log_error "ArgoCD non installé"
        exit 1
    fi

    kubectl apply -f argocd/ecommerce-application.yaml

    kubectl patch application ecommerce \
      -n argocd \
      --type merge \
      -p '{"spec":{"syncPolicy":{"automated":{"prune":true,"selfHeal":true}}}}'

    log_success "ArgoCD configuré"
}

# ================= WAIT PODS =================
wait_for_pods() {
    log_info "Attente des pods..."

    sleep 5
    kubectl get pods -n ecommerce

    if kubectl get pods -n ecommerce | grep -Ei 'Crash|Error'; then
        log_error "Pods en erreur"
        debug_pods
        exit 1
    fi

    kubectl wait --for=condition=ready pod --all -n ecommerce --timeout=300s
    log_success "Pods prêts"
}

# ================= DEBUG =================
debug_pods() {
    log_warning "Debug pods..."

    for pod in $(kubectl get pods -n ecommerce -o name); do
        echo "---- Logs $pod ----"
        kubectl logs $pod -n ecommerce --tail=20 || true
    done
}

# ================= INGRESS WAIT =================
wait_for_ingress() {
    log_info "Attente Ingress..."

    for i in {1..30}; do
        IP=$(kubectl get ingress ecommerce-ingress -n ecommerce -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
        HOST=$(kubectl get ingress ecommerce-ingress -n ecommerce -o jsonpath='{.spec.rules[0].host}')

        if [ ! -z "$IP" ] || [ ! -z "$HOST" ]; then
            log_success "Ingress prêt"
            echo "IP: $IP"
            echo "HOST: $HOST"
            return
        fi

        echo "⏳ attente..."
        sleep 10
    done

    log_warning "Ingress non exposé (normal en local)"
}

# ================= VERIFY =================
verify_deployment() {
    log_info "Vérification..."

    kubectl get pods -n ecommerce
    kubectl get svc -n ecommerce
    kubectl get ingress -n ecommerce

    wait_for_ingress

    log_success "Déploiement validé"
}

# ================= MAIN =================
main() {
    echo "🚀 DEPLOIEMENT E-COMMERCE"
    echo "========================="

    MODE=${1:-helm}
    VALUES=${2:-values.yaml}

    check_prerequisites
    create_namespace
    create_secrets
    install_ingress_controller

    case $MODE in
        helm)
            deploy_with_helm $VALUES
            ;;
        argocd)
            deploy_with_argocd
            ;;
        both)
            deploy_with_helm $VALUES
            deploy_with_argocd
            ;;
        *)
            log_error "Mode invalide"
            exit 1
            ;;
    esac

    wait_for_pods
    verify_deployment

    echo ""
    log_success "DEPLOIEMENT TERMINÉ 🎉"
}

# ================= HELP =================
if [[ "$1" == "--help" ]]; then
    echo "Usage:"
    echo "$0 [helm|argocd|both] [values.yaml]"
    exit 0
fi

main "$@"