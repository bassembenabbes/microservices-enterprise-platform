package com.microservices.chatbot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.chatbot.service.RAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GeminiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cache;
    
    @Value("${google.gemini.api-key:}")
    private String apiKey;
    
    @Value("${google.gemini.model:gemini-2.0-flash-exp}")
    private String model;
    
    @Value("${google.gemini.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
    @Autowired(required = false)
    private RAGService ragService;
    
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    
    public GeminiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }
    
    public String generateResponse(String prompt) {
        return generateResponse(prompt, null);
    }
    
    public String generateResponse(String prompt, String context) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️ Clé API Gemini non configurée");
            return null;
        }
        
        // Vérifier le cache
        String cacheKey = String.valueOf(prompt.hashCode()) + (context != null ? String.valueOf(context.hashCode()) : "");
        if (cacheEnabled && cache.containsKey(cacheKey)) {
            log.info("📦 Réponse depuis le cache");
            return cache.get(cacheKey);
        }
        
        try {
            String url = String.format(GEMINI_URL, model) + "?key=" + apiKey;
            
            String fullPrompt = buildPrompt(prompt, context);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", fullPrompt)))
            ));
            requestBody.put("generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 1024,
                "topP", 0.95,
                "topK", 40
            ));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("⏱️ Appel Gemini: {} ms", duration);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.path(0).path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String text = parts.path(0).path("text").asText();
                        if (text != null && !text.isEmpty()) {
                            if (cacheEnabled) {
                                cache.put(cacheKey, text);
                            }
                            return text;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur appel Gemini: {}", e.getMessage());
        }
        
        return null;
    }
    
    public String generateResponseWithRAG(String userMessage) {
        if (!ragEnabled || ragService == null) {
            return generateResponse(userMessage);
        }
        
        log.info("🧠 Génération avec RAG pour: {}", userMessage);
        
        String context = ragService.retrieveContext(userMessage, 3);
        List<String> suggestions = ragService.getSuggestions(userMessage);
        
        String enrichedPrompt = buildPromptWithRAG(userMessage, context, suggestions);
        String response = generateResponse(enrichedPrompt);
        
        if (response != null && !suggestions.isEmpty()) {
            StringBuilder sb = new StringBuilder(response);
            sb.append("\n\n💡 **Suggestions:**\n");
            for (String suggestion : suggestions) {
                sb.append("• ").append(suggestion).append("\n");
            }
            response = sb.toString();
        }
        
        return response;
    }
    
    private String buildPrompt(String userMessage, String context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Tu es un assistant professionnel pour une plateforme e-commerce.\n");
        prompt.append("Sois poli, courtois et concis.\n");
        prompt.append("Réponds en français.\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("Contexte:\n").append(context).append("\n\n");
        }
        
        prompt.append("Utilisateur: ").append(userMessage).append("\n");
        prompt.append("Assistant:");
        
        return prompt.toString();
    }
    
    private String buildPromptWithRAG(String userMessage, String context, List<String> suggestions) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Tu es un assistant e-commerce professionnel. Utilise le contexte fourni pour répondre.\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("CONTEXTE:\n").append(context).append("\n\n");
        }
        
        if (suggestions != null && !suggestions.isEmpty()) {
            prompt.append("SUGGESTIONS:\n");
            for (String suggestion : suggestions) {
                prompt.append("- ").append(suggestion).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("UTILISATEUR: ").append(userMessage).append("\n\n");
        prompt.append("ASSISTANT:");
        
        return prompt.toString();
    }
    
    public void clearCache() {
        cache.clear();
        log.info("🗑️ Cache vidé");
    }
}
