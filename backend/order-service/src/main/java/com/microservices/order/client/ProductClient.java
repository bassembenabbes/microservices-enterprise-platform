package com.microservices.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ProductClient {
    
    private final WebClient.Builder webClientBuilder;
    
    public ProductClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductInfoFallback")
    @Retry(name = "product-service")
    public Map<String, Object> getProductInfo(String productId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://product-service:8002").build();
            return webClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error getting product info: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private Map<String, Object> getProductInfoFallback(String productId, Throwable t) {
        log.warn("Fallback: product-service unavailable for productId: {}", productId);
        Map<String, Object> defaultProduct = new HashMap<>();
        defaultProduct.put("name", "Unknown Product");
        defaultProduct.put("price", 0.0);
        defaultProduct.put("stock", 0);
        return defaultProduct;
    }
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "updateStockFallback")
    @Retry(name = "product-service")
    public boolean updateStock(String productId, int quantity, String operation) {
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://product-service:8002").build();
            Map<String, Object> request = Map.of("quantity", quantity, "operation", operation);
            
            webClient.patch()
                    .uri("/api/products/{productId}/stock", productId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.error("Error updating stock: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private boolean updateStockFallback(String productId, int quantity, String operation, Throwable t) {
        log.warn("Fallback: product-service unavailable for stock update - productId: {}, quantity: {}", productId, quantity);
        return false;
    }
}
