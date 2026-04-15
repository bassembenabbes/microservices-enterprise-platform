package com.microservices.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ProductServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.product.url:http://product-service:8002}")
    private String productServiceUrl;
    
    public ProductServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public List<Map<String, Object>> searchProducts(String query, String category) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(productServiceUrl).build();
            String uri = "/api/products/search?q=" + (query != null ? query : "");
            
            log.info("🔍 Appel Product Service: {}{}", productServiceUrl, uri);
            
            Map<String, Object> response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            if (response != null && response.containsKey("products")) {
                Object productsObj = response.get("products");
                if (productsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> products = (List<Map<String, Object>>) productsObj;
                    log.info("✅ {} produits trouvés", products.size());
                    return products;
                }
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("❌ Erreur recherche produits: {}", e.getMessage());
            return getMockProducts();
        }
    }
    
    private List<Map<String, Object>> getMockProducts() {
        log.info("📦 Retourne produits mockés");
        return List.of(
            Map.of("id", "2", "name", "iPhone 15", "price", 900.00, "stock", 9),
            Map.of("id", "1", "name", "Test Product", "price", 99.99, "stock", 11),
            Map.of("id", "3", "name", "Samsung Galaxy", "price", 800.00, "stock", 5)
        );
    }
}
