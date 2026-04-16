package com.microservices.chatbot.client;

import com.microservices.chatbot.dto.OrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.order.url:http://order-service:8003}")
    private String orderServiceUrl;
    
    public OrderServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public Map<String, Object> getOrderStatus(String orderId, String userId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
            return webClient.get()
                    .uri("/api/orders/{orderId}?userId={userId}", orderId, userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error getting order status: {}", e.getMessage());
            return Map.of("orderId", orderId, "status", "UNKNOWN");
        }
    }
    
    public Map<String, Object> createOrder(OrderRequest request) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
            return webClient.post()
                    .uri("/api/orders")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage());
            return Map.of("error", "Failed to create order");
        }
    }
    
    public List<Map<String, Object>> getUserOrders(String userId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
            return webClient.get()
                    .uri("/api/orders/user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error getting user orders: {}", e.getMessage());
            return List.of();
        }
    }
}
