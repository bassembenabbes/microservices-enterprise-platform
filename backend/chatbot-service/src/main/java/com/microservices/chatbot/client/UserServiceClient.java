package com.microservices.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class UserServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.user.url:http://user-service:8001}")
    private String userServiceUrl;
    
    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    public Map<String, Object> getUserInfo(String userId) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(userServiceUrl).build();
            return webClient.get()
                    .uri("/api/users/{userId}", "me")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            return Map.of("id", userId, "username", "unknown", "email", "unknown@example.com");
        }
    }
}
