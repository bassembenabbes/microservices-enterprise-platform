package com.microservices.chatbot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${google.gemini.api-key:}")
    private String apiKey;
    
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
    
    public GeminiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateResponse(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️ Clé API Gemini non configurée");
            return getFallbackResponse(prompt);
        }
        
        try {
            String url = GEMINI_URL + "?key=" + apiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ));
            requestBody.put("generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 1024,
                "topP", 0.95
            ));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                return text != null && !text.isEmpty() ? text : getFallbackResponse(prompt);
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur appel Gemini: {}", e.getMessage());
        }
        
        return getFallbackResponse(prompt);
    }
    
    private String getFallbackResponse(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("iphone") || lowerPrompt.contains("produit")) {
            return "🔍 **Produits disponibles:**\n\n• iPhone 15 Pro - 1299.99€\n• iPhone 15 - 1099.99€\n• Samsung Galaxy S24 - 1199.99€\n\nQue souhaitez-vous savoir ?";
        }
        if (lowerPrompt.contains("commande") || lowerPrompt.contains("order")) {
            return "📋 Pour connaître le statut de votre commande, veuillez me donner son numéro.\n\nExemple: \"Quel est le statut de ma commande #12345?\"";
        }
        if (lowerPrompt.contains("bonjour") || lowerPrompt.contains("salut")) {
            return "👋 Bonjour! Je suis votre assistant virtuel. Comment puis-je vous aider aujourd'hui?\n\n• 📦 Rechercher des produits\n• 📋 Suivre mes commandes\n• 👤 Mon compte";
        }
        return "Je suis votre assistant. Comment puis-je vous aider?";
    }
}
