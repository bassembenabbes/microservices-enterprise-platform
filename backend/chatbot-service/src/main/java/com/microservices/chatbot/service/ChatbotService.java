package com.microservices.chatbot.service;

import com.microservices.chatbot.client.GeminiClient;
import com.microservices.chatbot.client.OrderServiceClient;
import com.microservices.chatbot.client.ProductServiceClient;
import com.microservices.chatbot.client.UserServiceClient;
import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import com.microservices.chatbot.dto.OrderItemRequest;
import com.microservices.chatbot.dto.OrderRequest;
import com.microservices.chatbot.util.InputSanitizer;
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
    private final InputSanitizer inputSanitizer;
    
    @Value("${chatbot.use-gemini:true}")
    private boolean useGemini;
    
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("#?(\\d{5,})");
    
    public ChatResponse processMessage(ChatRequest request) {
        log.info("📨 Message reçu: {}", request.getMessage());
        
        // Sanitize and validate input
        String sanitizedMessage = inputSanitizer.sanitize(request.getMessage());
        if (!inputSanitizer.isValidMessage(sanitizedMessage)) {
            return ChatResponse.builder()
                .response("Message invalide ou trop long.")
                .sessionId(request.getSessionId())
                .intent("INVALID")
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        String message = sanitizedMessage.toLowerCase();
        
        // Détection d'intention
        if (message.contains("iphone") || message.contains("produit") || 
            message.contains("cherche") || message.contains("recherche")) {
            return handleProductSearch(request);
        }
        else if ((message.contains("créer") || message.contains("create")) && 
                 (message.contains("commande") || message.contains("order"))) {
            return handleCreateOrder(request);
        }
        else if ((message.contains("lister") || message.contains("liste") || message.contains("mes")) && 
                 (message.contains("commande") || message.contains("order"))) {
            return handleListOrders(request);
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
                try {
                    double price = Double.parseDouble(p.get("price").toString());
                    int stock = Integer.parseInt(p.get("stock").toString());
                    context.append(String.format("- %s: %.2f€ (Stock: %d)\n", 
                        p.get("name"), price, stock));
                } catch (Exception e) {
                    log.warn("Produit mal formé: {}", p);
                }
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
                try {
                    double price = Double.parseDouble(p.get("price").toString());
                    response.append(String.format("• **%s** - %.2f€\n", p.get("name"), price));
                } catch (Exception e) {
                    log.warn("Produit mal formé: {}", p);
                }
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
    
    private ChatResponse handleCreateOrder(ChatRequest request) {
        log.info("🛒 Création de commande");
        
        try {
            // Extraire les informations de l'article et quantité
            String productId = extractProductId(request.getMessage());
            Integer quantity = extractQuantity(request.getMessage());
            
            if (productId == null || quantity == null) {
                return ChatResponse.builder()
                    .response("📝 Pour créer une commande, veuillez préciser l'article et la quantité.\nExemple: créer une commande avec l'article 'iPhone' quantité 1")
                    .sessionId(request.getSessionId())
                    .intent("CREATE_ORDER")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Récupérer les détails du produit
            List<Map<String, Object>> products = productClient.searchProducts(productId, null);
            if (products == null || products.isEmpty()) {
                return ChatResponse.builder()
                    .response("❌ Article '" + productId + "' non trouvé.")
                    .sessionId(request.getSessionId())
                    .intent("CREATE_ORDER")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            Map<String, Object> product = products.get(0);
            String productName = product.get("name").toString();
            double unitPrice = Double.parseDouble(product.get("price").toString());
            
            // Créer la commande
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setUserId(request.getUserId());
            orderRequest.setEmail("user@example.com"); // TODO: récupérer depuis user-service
            
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(productId);
            item.setProductName(productName);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            
            orderRequest.setItems(List.of(item));
            orderRequest.setPaymentMethod("chatbot");
            
            Map<String, Object> orderResponse = orderClient.createOrder(orderRequest);
            
            if (orderResponse.containsKey("error")) {
                return ChatResponse.builder()
                    .response("❌ Erreur lors de la création de la commande: " + orderResponse.get("error"))
                    .sessionId(request.getSessionId())
                    .intent("CREATE_ORDER")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            String orderId = orderResponse.getOrDefault("orderId", "N/A").toString();
            double total = quantity * unitPrice;
            
            String context = String.format("Commande créée: #%s - %s x%d = %.2f€", orderId, productName, quantity, total);
            String geminiResponse = geminiClient.generateResponse(
                "L'utilisateur a créé une commande. Confirme la création de manière positive.",
                context);
            
            if (geminiResponse != null && useGemini) {
                return ChatResponse.builder()
                    .response(geminiResponse)
                    .sessionId(request.getSessionId())
                    .intent("CREATE_ORDER")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            return ChatResponse.builder()
                .response(String.format("✅ Commande #%s créée avec succès!\n• %s x%d = %.2f€", orderId, productName, quantity, total))
                .sessionId(request.getSessionId())
                .intent("CREATE_ORDER")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erreur création commande: {}", e.getMessage());
            return generateGeminiResponse(request, "Erreur lors de la création de la commande");
        }
    }
    
    private ChatResponse handleListOrders(ChatRequest request) {
        log.info("📋 Liste des commandes");
        
        try {
            List<Map<String, Object>> orders = orderClient.getUserOrders(request.getUserId());
            
            if (orders == null || orders.isEmpty()) {
                return ChatResponse.builder()
                    .response("📭 Vous n'avez aucune commande pour le moment.")
                    .sessionId(request.getSessionId())
                    .intent("LIST_ORDERS")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Construire le contexte pour Gemini
            StringBuilder context = new StringBuilder();
            context.append("Commandes de l'utilisateur:\n");
            for (Map<String, Object> o : orders) {
                context.append(String.format("- #%s: %s (%.2f€)\n", 
                    o.get("orderId"), o.get("status"), o.get("totalAmount")));
            }
            
            String geminiResponse = geminiClient.generateResponse(
                "L'utilisateur demande la liste de ses commandes. Présente-les de manière claire.",
                context.toString());
            
            if (geminiResponse != null && useGemini) {
                return ChatResponse.builder()
                    .response(geminiResponse)
                    .sessionId(request.getSessionId())
                    .intent("LIST_ORDERS")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Fallback template
            StringBuilder response = new StringBuilder();
            response.append(String.format("📋 **Vos %d commande(s):**\n\n", orders.size()));
            for (Map<String, Object> o : orders) {
                response.append(String.format("• **#%s** - %s - %.2f€\n", 
                    o.get("orderId"), o.get("status"), o.get("totalAmount")));
            }
            
            return ChatResponse.builder()
                .response(response.toString())
                .sessionId(request.getSessionId())
                .intent("LIST_ORDERS")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erreur liste commandes: {}", e.getMessage());
            return generateGeminiResponse(request, "Erreur lors de la récupération des commandes");
        }
    }
    
    private String extractProductId(String message) {
        // Chercher "article" suivi de guillemets ou d'un mot
        Pattern pattern = Pattern.compile("article\\s*[\"']?([^\"'\\s]+)[\"']?", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Chercher "identifiant exact" 
        pattern = Pattern.compile("identifiant\\s+exact\\s+de\\s+l'article\\s*=\\s*[\"']?([^\"'\\s]+)[\"']?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private Integer extractQuantity(String message) {
        // Chercher "quantité" suivi de "=" et d'un nombre
        Pattern pattern = Pattern.compile("quantité\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
