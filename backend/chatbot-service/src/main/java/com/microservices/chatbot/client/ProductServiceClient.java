package com.microservices.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            
            // URL correcte: /api/products/search (pas de double /api)
            StringBuilder uri = new StringBuilder("/api/products/search");
            boolean hasParam = false;
            
            if (query != null && !query.isEmpty() && !query.equals("*")) {
                uri.append("?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                hasParam = true;
            }
            if (category != null && !category.isEmpty()) {
                uri.append(hasParam ? "&" : "?").append("category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8));
            }
            
            log.info("🔍 Appel Product Service: {}{}", productServiceUrl, uri.toString());
            
            Map<String, Object> response = webClient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            if (response != null && response.containsKey("products")) {
                Object products = response.get("products");
                if (products instanceof List) {
                    log.info("✅ {} produits trouvés", ((List<?>) products).size());
                    return (List<Map<String, Object>>) products;
                }
            }
            
            log.warn("⚠️ Aucun produit trouvé pour la recherche: {}", query);
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("❌ Erreur recherche produits: {}", e.getMessage());
            return getMockProducts(query);
        }
    }
    
    // Produits mockés pour le test (basés sur les vrais produits)
    private List<Map<String, Object>> getMockProducts(String query) {
        log.info("📦 Retourne produits mockés pour: {}", query);
        return List.of(
            Map.of("id", "2", "name", "iPhone 15", "price", 900.00, "stock", 9, "category", "Books"),
            Map.of("id", "1", "name", "Test Product", "price", 99.99, "stock", 11, "category", "Electronics"),
            Map.of("id", "3", "name", "a", "price", 1.00, "stock", 1, "category", "Sports")
        );
    }
}
