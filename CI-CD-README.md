# CI/CD Setup

This document describes the CI/CD pipelines implemented for the e-commerce microservices project.

## GitHub Actions

### CI Pipeline (`.github/workflows/ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` branch

**Jobs:**
1. **Test Backend**: Runs unit tests for Java services (Chatbot, Order)
2. **Test Frontend**: Runs tests and coverage for React app
3. **Build Images**: Builds and pushes Docker images to registry

**Required Secrets:**
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password

### CD Pipeline (`.github/workflows/cd.yml`)

**Triggers:**
- Push to `main` branch
- Manual dispatch with environment selection

**Jobs:**
1. **Deploy**: Deploys to Kubernetes using Helm
2. **Notify**: Sends Slack notification

**Required Secrets:**
- `KUBE_CONFIG`: Kubernetes configuration
- `GEMINI_API_KEY`: API key for chatbot
- `SLACK_WEBHOOK_URL`: Slack webhook for notifications

## Jenkins Pipeline

### Jenkinsfile

**Features:**
- Parallel testing for backend services
- Docker image building and pushing
- Environment-based deployments (staging/production)
- Integration tests
- Slack notifications

**Setup Requirements:**
1. Install required plugins: Docker, Kubernetes, Helm
2. Configure credentials:
   - `kubeconfig`: Kubernetes configuration
   - Docker registry credentials
3. Set environment variables:
   - `DOCKER_REGISTRY`: Your Docker registry URL

## Local Development

### Makefile Commands

```bash
make build          # Build all services
make test           # Run all tests
make docker-build   # Build Docker images
make docker-push    # Push Docker images
make helm-install   # Install Helm chart
make helm-upgrade   # Upgrade Helm release
make deploy         # Deploy to Kubernetes
make clean          # Clean artifacts
```

### Docker Compose Override

The `docker-compose.override.yml` allows using local Docker images for development while keeping the base configuration in `docker-compose.yml`.

## Deployment Environments

### Staging
- Auto-deployed from `develop` branch
- Namespace: `ecommerce-staging`

### Production
- Manual approval required
- Deployed from `main` branch
- Namespace: `ecommerce-prod`

## Monitoring CI/CD

- **Test Results**: Published as JUnit reports
- **Coverage Reports**: HTML reports for frontend
- **Slack Notifications**: Deployment status updates
- **Docker Cleanup**: Automatic cleanup after builds

## Security Considerations

- Secrets stored in GitHub/Jenkins credentials
- No sensitive data in code
- Image scanning recommended (add to pipeline)
- RBAC configured for Kubernetes deployments

## Troubleshooting

### Common Issues

1. **Docker Build Failures**: Check Dockerfile syntax and base images
2. **Kubernetes Deployment**: Verify kubeconfig and cluster access
3. **Helm Chart Errors**: Run `helm template` for debugging
4. **Test Failures**: Check service dependencies and mocking

### Logs

- GitHub Actions: Available in Actions tab
- Jenkins: Console output and archived artifacts
- Kubernetes: `kubectl logs` and `kubectl describe`
