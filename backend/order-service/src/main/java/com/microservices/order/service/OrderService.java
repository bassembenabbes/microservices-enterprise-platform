package com.microservices.order.service;

import com.microservices.order.client.UserClient;
import com.microservices.order.dto.*;
import com.microservices.order.model.*;
import com.microservices.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final WebClient webClient;
    
    @Value("${n8n.webhook.url:http://n8n:5678/webhook}")
    private String n8nWebhookUrl;
    
    public OrderService(OrderRepository orderRepository, UserClient userClient, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.webClient = webClientBuilder.build();
    }
    
    private static final String ORDER_NUMBER_PREFIX = "ORD-";
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        
        // Vérifier l'utilisateur via User Service (avec Resilience4j)
        boolean userExists = userClient.userExists(request.getUserId());
        if (!userExists) {
            log.warn("User not found in User Service: {}, but continuing", request.getUserId());
        }
        
        // Récupérer les infos utilisateur
        Map<String, Object> userInfo = userClient.getUserInfo(request.getUserId());
        String userEmail = userInfo != null ? (String) userInfo.get("email") : request.getEmail();
        
        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
        String orderNumber = ORDER_NUMBER_PREFIX + System.currentTimeMillis();
        
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(request.getUserId());
        order.setUserEmail(userEmail);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryStatus(DeliveryStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setEmail(request.getEmail());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setSubtotal(itemReq.getQuantity() * itemReq.getUnitPrice());
            item.setOrder(order);
            order.getItems().add(item);
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());
        
        // Trigger n8n workflow for order processing
        triggerOrderCreatedWebhook(savedOrder, request);
        
        return mapToResponse(savedOrder);
    }
    
    public OrderResponse getOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }
        return mapToResponse(order);
    }
    
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setUserEmail(order.getUserEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setDeliveryStatus(order.getDeliveryStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setEmail(order.getEmail());
        response.setCreatedAt(order.getCreatedAt());
        
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            OrderItemResponse itemRes = new OrderItemResponse();
            itemRes.setId(item.getId());
            itemRes.setProductId(item.getProductId());
            itemRes.setProductName(item.getProductName());
            itemRes.setQuantity(item.getQuantity());
            itemRes.setUnitPrice(item.getUnitPrice());
            itemRes.setSubtotal(item.getSubtotal());
            return itemRes;
        }).collect(Collectors.toList());
        response.setItems(items);
        
        return response;
    }
    
    private void triggerOrderCreatedWebhook(Order order, OrderRequest request) {
        // Prepare the payload for the webhook
        WebhookPayload payload = new WebhookPayload();
        payload.setOrderId(order.getId());
        payload.setOrderNumber(order.getOrderNumber());
        payload.setUserId(order.getUserId());
        payload.setUserEmail(order.getUserEmail());
        payload.setTotalAmount(order.getTotalAmount());
        payload.setStatus(order.getStatus());
        payload.setPaymentStatus(order.getPaymentStatus());
        payload.setDeliveryStatus(order.getDeliveryStatus());
        payload.setShippingAddress(order.getShippingAddress());
        payload.setPhoneNumber(order.getPhoneNumber());
        payload.setEmail(order.getEmail());
        payload.setCreatedAt(order.getCreatedAt());
        payload.setItems(order.getItems().stream().map(item -> {
            WebhookItem webhookItem = new WebhookItem();
            webhookItem.setProductId(item.getProductId());
            webhookItem.setProductName(item.getProductName());
            webhookItem.setQuantity(item.getQuantity());
            webhookItem.setUnitPrice(item.getUnitPrice());
            webhookItem.setSubtotal(item.getSubtotal());
            return webhookItem;
        }).collect(Collectors.toList()));
        payload.setToken(request.getToken());
        
        // Call the n8n webhook URL
        webClient.post()
                .uri(n8nWebhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(aVoid -> log.info("Triggered n8n webhook successfully"))
                .doOnError(throwable -> log.error("Error triggering n8n webhook", throwable))
                .subscribe();
    }
    
    private static class WebhookPayload {
        private String orderId;
        private String orderNumber;
        private String userId;
        private String userEmail;
        private double totalAmount;
        private OrderStatus status;
        private PaymentStatus paymentStatus;
        private DeliveryStatus deliveryStatus;
        private String shippingAddress;
        private String phoneNumber;
        private String email;
        private LocalDateTime createdAt;
        private List<WebhookItem> items;
        private String token;
        
        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public PaymentStatus getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
        public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<WebhookItem> getItems() { return items; }
        public void setItems(List<WebhookItem> items) { this.items = items; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    
    private static class WebhookItem {
        private String productId;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double subtotal;
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    }
}
