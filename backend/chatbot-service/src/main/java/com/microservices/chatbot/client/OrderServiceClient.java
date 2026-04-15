package com.microservices.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("Error getting order status: {}", e.getMessage());
            return Map.of("orderId", orderId, "status", "UNKNOWN");
        }
    }
}
