# E-commerce Project Makefile

.PHONY: help build test deploy clean

# Default target
help:
	@echo "Available commands:"
	@echo "  build        - Build all services"
	@echo "  test         - Run all tests"
	@echo "  deploy       - Deploy to Kubernetes"
	@echo "  clean        - Clean build artifacts"
	@echo "  docker-build - Build Docker images"
	@echo "  docker-push  - Push Docker images"
	@echo "  helm-install - Install Helm chart"
	@echo "  helm-upgrade - Upgrade Helm release"

# Build all services
build:
	@echo "Building backend services..."
	cd backend/chatbot-service && mvn clean package -DskipTests
	cd backend/order-service && mvn clean package -DskipTests
	@echo "Building frontend..."
	cd frontend/react-app && npm run build

# Run tests
test:
	@echo "Running backend tests..."
	cd backend/chatbot-service && mvn test
	cd backend/order-service && mvn test
	@echo "Running frontend tests..."
	cd frontend/react-app && npm test -- --watchAll=false

# Docker operations
docker-build:
	@echo "Building Docker images..."
	docker build -t ecommerce-user-service backend/user-service
	docker build -t ecommerce-product-service backend/product-service
	docker build -t ecommerce-order-service backend/order-service
	docker build -t ecommerce-chatbot-service backend/chatbot-service
	docker build -t ecommerce-api-gateway backend/api-gateway
	docker build -t ecommerce-frontend frontend
	docker build -t ecommerce-notification-service backend/notification-service

docker-push:
	@echo "Pushing Docker images..."
	docker push ecommerce-user-service:latest
	docker push ecommerce-product-service:latest
	docker push ecommerce-order-service:latest
	docker push ecommerce-chatbot-service:latest
	docker push ecommerce-api-gateway:latest
	docker push ecommerce-frontend:latest
	docker push ecommerce-notification-service:latest

# Kubernetes/Helm operations
helm-install:
	@echo "Installing Helm chart..."
	helm dependency update helm/ecommerce
	helm install ecommerce helm/ecommerce --create-namespace --namespace ecommerce

helm-upgrade:
	@echo "Upgrading Helm release..."
	helm dependency update helm/ecommerce
	helm upgrade ecommerce helm/ecommerce --namespace ecommerce

deploy: helm-upgrade

# Clean up
clean:
	@echo "Cleaning build artifacts..."
	cd backend/chatbot-service && mvn clean
	cd backend/order-service && mvn clean
	cd frontend/react-app && rm -rf build node_modules
	docker system prune -f

# Development
dev-up:
	@echo "Starting development environment..."
	docker compose -f docker-compose.yml up -d

dev-down:
	@echo "Stopping development environment..."
	docker compose -f docker-compose.yml down

# Linting and formatting
lint:
	@echo "Running linters..."
	cd frontend/react-app && npm run lint
	cd backend/chatbot-service && mvn checkstyle:check

format:
	@echo "Formatting code..."
	cd frontend/react-app && npm run format
