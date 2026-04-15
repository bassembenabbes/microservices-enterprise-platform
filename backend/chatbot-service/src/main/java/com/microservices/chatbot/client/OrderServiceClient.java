package com.microservices.chatbot.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.order.url}")
    private String orderServiceUrl;
    
    @CircuitBreaker(name = "order-service", fallbackMethod = "createOrderFallback")
    public Map<String, Object> createOrder(Map<String, Object> orderData) {
        log.info("📞 Appel Order Service pour créer commande");
        
        WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
        
        return webClient.post()
                .uri("/api/orders")
                .bodyValue(orderData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    
    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrderStatusFallback")
    public Map<String, Object> getOrderStatus(String orderId, String userId) {
        WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
        
        return webClient.get()
                .uri("/api/orders/{orderId}?userId={userId}", orderId, userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    
    @CircuitBreaker(name = "order-service", fallbackMethod = "getUserOrdersFallback")
    public List<Map<String, Object>> getUserOrders(String userId) {
        WebClient webClient = webClientBuilder.baseUrl(orderServiceUrl).build();
        
        List<Map<String, Object>> response = webClient.get()
                .uri("/api/orders/user/{userId}", userId)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        
        return response != null ? response : List.of();
    }
    
    private Map<String, Object> createOrderFallback(Map<String, Object> orderData, Throwable t) {
        log.warn("Fallback Order Service: commande simulée");
        return Map.of(
            "id", "fallback-" + System.currentTimeMillis(),
            "status", "PENDING",
            "totalAmount", orderData.getOrDefault("totalAmount", 0)
        );
    }
    
    private Map<String, Object> getOrderStatusFallback(String orderId, String userId, Throwable t) {
        return Map.of("status", "UNKNOWN", "message", "Service indisponible");
    }
    
    private List<Map<String, Object>> getUserOrdersFallback(String userId, Throwable t) {
        return List.of();
    }
}
