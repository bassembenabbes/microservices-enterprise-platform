package com.microservices.chatbot.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.user.url}")
    private String userServiceUrl;
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserInfoFallback")
    public Map<String, Object> getUserInfo(String userId) {
        log.info("📞 Appel User Service pour userId: {}", userId);
        
        WebClient webClient = webClientBuilder.baseUrl(userServiceUrl).build();
        
        return webClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "checkUserExistsFallback")
    public boolean userExists(String userId) {
        WebClient webClient = webClientBuilder.baseUrl(userServiceUrl).build();
        
        Map<String, Object> response = webClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), 
                    responseMono -> Mono.empty())
                .bodyToMono(Map.class)
                .block();
        
        return response != null && !response.containsKey("error");
    }
    
    private Map<String, Object> getUserInfoFallback(String userId, Throwable t) {
        log.warn("Fallback User Service: utilisateur par défaut");
        return Map.of(
            "id", userId,
            "username", "utilisateur_" + userId,
            "email", "default@example.com"
        );
    }
    
    private boolean checkUserExistsFallback(String userId, Throwable t) {
        log.warn("Fallback User Service: utilisateur supposé existant");
        return true;
    }
}
