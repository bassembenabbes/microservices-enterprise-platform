# E-commerce Microservices Helm Chart

This Helm chart deploys the complete e-commerce microservices application on Kubernetes.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- Persistent storage class

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
helm install my-release ./helm/ecommerce
```

## Configuration

The following table lists the configurable parameters of the e-commerce chart and their default values.

| Parameter | Description | Default |
|-----------|-------------|---------|
| `postgresql.enabled` | Enable PostgreSQL databases | `true` |
| `redis.enabled` | Enable Redis | `true` |
| `kafka.enabled` | Enable Kafka | `true` |
| `elasticsearch.enabled` | Enable Elasticsearch | `true` |
| `prometheus.enabled` | Enable Prometheus | `true` |
| `grafana.enabled` | Enable Grafana | `true` |
| `ingress.enabled` | Enable Ingress | `true` |
| `ingress.hosts[0].host` | Ingress host | `ecommerce.local` |

## Services

- **User Service**: Manages user accounts
- **Product Service**: Handles product catalog
- **Order Service**: Processes orders
- **Chatbot Service**: AI-powered customer support
- **API Gateway**: Routes requests
- **Frontend**: React application
- **Notification Service**: Sends notifications

## Monitoring

- **Prometheus**: Metrics collection
- **Grafana**: Dashboards
- **ELK Stack**: Centralized logging

## Event Streaming

- **Kafka**: Message broker for event-driven architecture

## GitOps with ArgoCD

To enable ArgoCD integration, set `argocd.enabled=true` and configure your ArgoCD instance to sync this chart.
