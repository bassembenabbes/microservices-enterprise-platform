package com.microservices.chatbot.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.product.url}")
    private String productServiceUrl;
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "getProductInfoFallback")
    public Map<String, Object> getProductInfo(String productId) {
        log.info("📞 Appel Product Service pour productId: {}", productId);
        
        WebClient webClient = webClientBuilder.baseUrl(productServiceUrl).build();
        
        return webClient.get()
                .uri("/api/products/{productId}", productId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "searchProductsFallback")
    public List<Map<String, Object>> searchProducts(String query, String category) {
        WebClient webClient = webClientBuilder.baseUrl(productServiceUrl).build();
        
        String uri = "/api/products/search";
        if (query != null) uri += "?q=" + query;
        if (category != null) uri += (query != null ? "&" : "?") + "category=" + category;
        
        Map<String, Object> response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        return response != null ? (List<Map<String, Object>>) response.get("products") : List.of();
    }
    
    @CircuitBreaker(name = "product-service", fallbackMethod = "checkStockFallback")
    public int checkStock(String productId) {
        Map<String, Object> product = getProductInfo(productId);
        return product != null ? (int) product.getOrDefault("stock", 0) : 0;
    }
    
    private Map<String, Object> getProductInfoFallback(String productId, Throwable t) {
        log.warn("Fallback Product Service: produit par défaut");
        return Map.of(
            "id", productId,
            "name", "Produit " + productId,
            "price", 0.0,
            "stock", 0
        );
    }
    
    private List<Map<String, Object>> searchProductsFallback(String query, String category, Throwable t) {
        log.warn("Fallback Product Service: recherche vide");
        return List.of();
    }
    
    private int checkStockFallback(String productId, Throwable t) {
        return 0;
    }
}
