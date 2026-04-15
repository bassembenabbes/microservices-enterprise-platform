# N8n Workflows

This directory contains n8n workflow definitions for automating e-commerce processes.

## Available Workflows

### 1. Order Processing Workflow (`order-processing-workflow.json`)

**Purpose**: Handles order confirmation and notifications when a new order is created.

**Trigger**: Webhook at `/webhook/order-created`

**Flow**:
1. Receive order data from order-service
2. Fetch user information from user-service
3. Fetch order details from order-service
4. Send confirmation email to customer
5. Send push notification via notification-service

**Required Services**:
- user-service (port 8001)
- order-service (port 8003)
- notification-service (port 8004)
- Email service (configured in n8n)

### 2. Product Inventory Workflow (`product-inventory-workflow.json`)

**Purpose**: Monitors product stock levels and sends alerts for low or out-of-stock items.

**Trigger**: Webhook at `/webhook/product-updated`

**Flow**:
1. Receive product update data
2. Check if stock is low (< 10 items)
3. Send low stock alert email to admin
4. Check if product is out of stock (= 0)
5. Send out of stock alert email to admin

**Required Services**:
- Email service (configured in n8n)

### 3. User Registration Workflow (`user-registration-workflow.json`)

**Purpose**: Sends welcome emails and notifications to new users.

**Trigger**: Webhook at `/webhook/user-registered`

**Flow**:
1. Receive user registration data
2. Send welcome email to user
3. Send welcome notification via notification-service

**Required Services**:
- notification-service (port 8004)
- Email service (configured in n8n)

## Setup Instructions

### 1. Start N8n

```bash
docker compose up -d n8n
```

Access n8n at: http://localhost:5678

### 2. Import Workflows

1. Open n8n web interface
2. Click "Import" button
3. Upload the JSON workflow files from this directory
4. Activate the workflows

### 3. Configure Email

In each workflow that sends emails:
1. Edit the "Send Email" nodes
2. Configure your SMTP settings:
   - Host: your-smtp-server.com
   - Port: 587 (or 465 for SSL)
   - Username: your-email@domain.com
   - Password: your-email-password
   - From Email: noreply@yourdomain.com

### 4. Test Workflows

#### Test Order Processing:
```bash
curl -X POST http://localhost:5678/webhook/order-created \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "test-order-123",
    "userId": "user-456",
    "totalAmount": 99.99,
    "userEmail": "user@example.com",
    "token": "user-jwt-token"
  }'
```

#### Test Product Inventory:
```bash
curl -X POST http://localhost:5678/webhook/product-updated \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-123",
    "name": "Test Product",
    "stock": 5,
    "is_available": true
  }'
```

#### Test User Registration:
```bash
curl -X POST http://localhost:5678/webhook/user-registered \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "username": "testuser",
    "email": "user@example.com",
    "full_name": "Test User"
  }'
```

## Integration with Services

### Order Service

The order-service automatically triggers the order processing workflow when a new order is created. The webhook URL is configured via the `N8N_WEBHOOK_URL` environment variable.

### Product Service

The product service can trigger inventory alerts by calling the product update webhook when stock levels change.

### User Service

The user service can trigger welcome workflows by calling the user registration webhook after successful registration.

## Monitoring

- Check n8n logs: `docker logs n8n`
- View workflow executions in n8n web interface
- Monitor webhook calls and email delivery

## Customization

### Adding New Workflows

1. Create a new JSON workflow file
2. Define webhook triggers
3. Add nodes for your business logic
4. Test and activate the workflow
5. Update services to call the new webhook endpoints

### Environment Variables

Configure n8n with environment variables in `docker-compose.yml`:

```yaml
environment:
  - N8N_EMAIL_FROM=noreply@yourdomain.com
  - N8N_SMTP_HOST=your-smtp-server.com
  - N8N_SMTP_USER=your-email@domain.com
  - N8N_SMTP_PASS=your-email-password
```

## Troubleshooting

### Common Issues

1. **Webhook not triggering**: Check n8n logs and webhook URL configuration
2. **Email not sending**: Verify SMTP settings and credentials
3. **Service communication**: Ensure all services are running and accessible
4. **Workflow errors**: Check node configurations and data flow

### Logs

- N8n application logs: `docker logs n8n`
- Workflow execution logs: Available in n8n web interface
- Service integration logs: Check individual service logs

## Security Considerations

- Use HTTPS for webhook URLs in production
- Store email credentials securely
- Implement authentication for webhook endpoints if needed
- Monitor workflow executions for anomalies
