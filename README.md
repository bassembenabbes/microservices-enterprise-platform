# E-commerce Microservices
minikube config set memory 8192 minikube config set cpus 4
minikube start --memory=8192 --cpus=4


A complete e-commerce platform built with microservices architecture, featuring modern DevOps practices and cloud-native deployment.

## Architecture

- **Backend**: Java Spring Boot microservices
- **Frontend**: React with Redux
- **Database**: PostgreSQL with Redis caching
- **Messaging**: Kafka for event streaming
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Monitoring**: Prometheus + Grafana
- **Deployment**: Kubernetes with Helm
- **GitOps**: ArgoCD

## Services

- **User Service**: User management and authentication
- **Product Service**: Product catalog and inventory
- **Order Service**: Order processing
- **Chatbot Service**: AI-powered customer support
- **API Gateway**: Request routing and authentication
- **Frontend**: React SPA
- **Notification Service**: Email/SMS notifications

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Node.js 18+
- Java 17+
- Kubernetes cluster (for production)
- Helm 3+

### Development Setup

```bash
# Clone repository
git clone <repository-url>
cd ecommerce-microservices

# Start development environment
make dev-up

# Or with Docker Compose
docker compose up -d
```

### Production Deployment

```bash
# Install Helm chart
make helm-install

# Or manually
helm install ecommerce ./helm/ecommerce
```

## CI/CD

### GitHub Actions

- **CI**: Automated testing and image building
- **CD**: Kubernetes deployment with ArgoCD

### Jenkins

- Declarative pipeline for complex deployments
- Parallel testing and multi-environment support

See [CI-CD-README.md](CI-CD-README.md) for detailed setup.

## API Documentation

### User Service
- `POST /api/users/register` - Register new user
- `POST /api/users/login` - User login
- `GET /api/users/me` - Get current user (requires auth)

### Product Service
- `GET /api/products` - List products
- `POST /api/products` - Create product
- `GET /api/products/search` - Search products

### Order Service
- `POST /api/orders` - Create order
- `GET /api/orders/user/{userId}` - Get user orders

### Chatbot Service
- `POST /api/chat` - Send message to chatbot

## Development

### Backend Services

```bash
# Build all services
make build

# Run tests
make test

# Clean artifacts
make clean
```

### Frontend

```bash
cd frontend/react-app
npm install
npm start
```

### Docker

```bash
# Build images
make docker-build

# Push images
make docker-push
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | localhost |
| `DB_NAME` | Database name | service_db |
| `REDIS_HOST` | Redis host | localhost |
| `KAFKA_BROKERS` | Kafka brokers | localhost:9092 |
| `GEMINI_API_KEY` | Google Gemini API key | - |

### Helm Values

See `helm/ecommerce/values.yaml` for all configuration options.

## Monitoring

- **Grafana**: http://localhost:3001 (dev) / http://grafana.ecommerce.local (prod)
- **Kibana**: http://localhost:5601 (dev) / http://kibana.ecommerce.local (prod)
- **Prometheus**: http://localhost:9090 (dev) / http://prometheus.ecommerce.local (prod)

## Workflow Automation

- **N8n**: Business process automation and integrations
- Webhook-based workflows for order processing, notifications, and alerts

See [n8n/README.md](n8n/README.md) for workflow setup and configuration.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and add tests
4. Submit a pull request

## License

MIT License - see LICENSE file for details.
