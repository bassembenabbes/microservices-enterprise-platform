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
public class UserClient {
    
    private final WebClient.Builder webClientBuilder;
    
    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "userExistsFallback")
    @Retry(name = "user-service")
    public boolean userExists(String userId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://user-service:8001").build();
            Map<String, Object> response = webClient.get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            return response != null && !response.containsKey("error");
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private boolean userExistsFallback(String userId, Throwable t) {
        log.warn("Circuit breaker opened for user-service, fallback: assuming user exists: {}", userId);
        return true;
    }
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserInfoFallback")
    @Retry(name = "user-service")
    public Map<String, Object> getUserInfo(String userId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl("http://user-service:8001").build();
            return webClient.get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private Map<String, Object> getUserInfoFallback(String userId, Throwable t) {
        log.warn("Fallback: returning default user info for userId: {}", userId);
        Map<String, Object> defaultUser = new HashMap<>();
        defaultUser.put("email", "default@example.com");
        defaultUser.put("username", "default_user");
        return defaultUser;
    }
}
