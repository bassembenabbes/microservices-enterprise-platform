package com.microservices.chatbot.service;

import com.microservices.chatbot.client.GeminiClient;
import com.microservices.chatbot.client.OrderServiceClient;
import com.microservices.chatbot.client.ProductServiceClient;
import com.microservices.chatbot.client.UserServiceClient;
import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {
    
    private final UserServiceClient userClient;
    private final ProductServiceClient productClient;
    private final OrderServiceClient orderClient;
    private final GeminiClient geminiClient;
    
    @Value("${chatbot.use-gemini:true}")
    private boolean useGemini;
    
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("#?(\\d{5,})");
    
    public ChatResponse processMessage(ChatRequest request) {
        log.info("📨 Message reçu: {}", request.getMessage());
        
        String message = request.getMessage().toLowerCase();
        
        // Détection d'intention
        if (message.contains("iphone") || message.contains("produit") || 
            message.contains("cherche") || message.contains("recherche")) {
            return handleProductSearch(request);
        }
        else if (message.contains("commande") && (message.contains("statut") || message.contains("status"))) {
            return handleOrderStatus(request);
        }
        else if (message.contains("mon compte") || message.contains("profil")) {
            return handleUserInfo(request);
        }
        else {
            return handleGeneralChat(request);
        }
    }
    
    private ChatResponse handleProductSearch(ChatRequest request) {
        log.info("🔍 Recherche de produits");
        
        try {
            String query = extractSearchQuery(request.getMessage());
            List<Map<String, Object>> products = productClient.searchProducts(query, null);
            
            if (products == null || products.isEmpty()) {
                return generateGeminiResponse(request, "Aucun produit trouvé pour: " + query);
            }
            
            // Construire le contexte pour Gemini
            StringBuilder context = new StringBuilder();
            context.append("Produits disponibles:\n");
            for (Map<String, Object> p : products) {
                context.append(String.format("- %s: %.2f€ (Stock: %d)\n", 
                    p.get("name"), p.get("price"), p.get("stock")));
            }
            
            String userPrompt = String.format("L'utilisateur cherche: %s. Présente-lui les produits trouvés de manière naturelle.", query);
            String geminiResponse = geminiClient.generateResponse(userPrompt, context.toString());
            
            if (geminiResponse != null && useGemini) {
                return ChatResponse.builder()
                    .response(geminiResponse)
                    .sessionId(request.getSessionId())
                    .intent("PRODUCT_SEARCH")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Fallback template
            StringBuilder response = new StringBuilder();
            response.append(String.format("🔍 **%d produit(s) trouvé(s):**\n\n", products.size()));
            for (Map<String, Object> p : products) {
                response.append(String.format("• **%s** - %.2f€\n", p.get("name"), p.get("price")));
            }
            
            return ChatResponse.builder()
                .response(response.toString())
                .sessionId(request.getSessionId())
                .intent("PRODUCT_SEARCH")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erreur recherche: {}", e.getMessage());
            return generateGeminiResponse(request, "Erreur technique, veuillez réessayer");
        }
    }
    
    private ChatResponse handleOrderStatus(ChatRequest request) {
        log.info("📋 Recherche statut commande");
        
        String orderId = extractOrderId(request.getMessage());
        if (orderId == null) {
            return ChatResponse.builder()
                .response("📋 Pour connaître le statut, donnez-moi votre numéro de commande.\nExemple: #12345")
                .sessionId(request.getSessionId())
                .intent("ORDER_STATUS")
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        try {
            Map<String, Object> order = orderClient.getOrderStatus(orderId, request.getUserId());
            String status = order.getOrDefault("status", "Inconnu").toString();
            
            String context = String.format("Commande #%s: statut %s", orderId, status);
            String geminiResponse = geminiClient.generateResponse(
                "L'utilisateur demande le statut de sa commande. Réponds de manière rassurante.",
                context);
            
            if (geminiResponse != null && useGemini) {
                return ChatResponse.builder()
                    .response(geminiResponse)
                    .sessionId(request.getSessionId())
                    .intent("ORDER_STATUS")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            return ChatResponse.builder()
                .response(String.format("📋 Commande #%s: %s", orderId, status))
                .sessionId(request.getSessionId())
                .intent("ORDER_STATUS")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return generateGeminiResponse(request, "Service momentanément indisponible");
        }
    }
    
    private ChatResponse handleUserInfo(ChatRequest request) {
        log.info("👤 Recherche infos utilisateur");
        
        try {
            Map<String, Object> user = userClient.getUserInfo(request.getUserId());
            String username = user.getOrDefault("username", "Utilisateur").toString();
            String email = user.getOrDefault("email", "non renseigné").toString();
            
            String context = String.format("Utilisateur: %s, Email: %s", username, email);
            String geminiResponse = geminiClient.generateResponse(
                "L'utilisateur demande ses informations de profil. Présente-les de manière claire.",
                context);
            
            if (geminiResponse != null && useGemini) {
                return ChatResponse.builder()
                    .response(geminiResponse)
                    .sessionId(request.getSessionId())
                    .intent("USER_INFO")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            return ChatResponse.builder()
                .response(String.format("👤 %s (%s)", username, email))
                .sessionId(request.getSessionId())
                .intent("USER_INFO")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return generateGeminiResponse(request, "Service indisponible");
        }
    }
    
    private ChatResponse handleGeneralChat(ChatRequest request) {
        log.info("💬 Chat général");
        
        String geminiResponse = geminiClient.generateResponse(request.getMessage());
        
        if (geminiResponse != null && useGemini) {
            return ChatResponse.builder()
                .response(geminiResponse)
                .sessionId(request.getSessionId())
                .intent("GENERAL")
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        return ChatResponse.builder()
            .response("👋 Bonjour! Je suis votre assistant. Comment puis-je vous aider?\n\n" +
                "• 🔍 Rechercher des produits\n" +
                "• 📋 Suivre mes commandes\n" +
                "• 👤 Mon compte")
            .sessionId(request.getSessionId())
            .intent("GENERAL")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private ChatResponse generateGeminiResponse(ChatRequest request, String fallbackMessage) {
        String geminiResponse = geminiClient.generateResponse(request.getMessage());
        if (geminiResponse != null && useGemini) {
            return ChatResponse.builder()
                .response(geminiResponse)
                .sessionId(request.getSessionId())
                .intent("GEMINI")
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        return ChatResponse.builder()
            .response(fallbackMessage)
            .sessionId(request.getSessionId())
            .intent("FALLBACK")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private String extractSearchQuery(String message) {
        return message.toLowerCase()
            .replaceAll("(cherche|recherche|trouve|produit|un|une|des|je|veux)", "")
            .replaceAll("[?!.,;]", "")
            .trim();
    }
    
    private String extractOrderId(String message) {
        var matcher = ORDER_ID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
}
