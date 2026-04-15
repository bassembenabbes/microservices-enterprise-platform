package com.microservices.chatbot.service;

import com.microservices.chatbot.client.OrderServiceClient;
import com.microservices.chatbot.client.ProductServiceClient;
import com.microservices.chatbot.client.UserServiceClient;
import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("#?(\\d{5,})");
    
    public ChatResponse processMessage(ChatRequest request) {
        log.info("📨 Message reçu: {}", request.getMessage());
        
        String message = request.getMessage().toLowerCase();
        
        if (message.contains("iphone") || message.contains("produit") || message.contains("cherche") || message.contains("recherche")) {
            return handleProductSearch(request);
        }
        else if (message.contains("commande") && (message.contains("statut") || message.contains("status") || message.contains("suivi"))) {
            return handleOrderStatus(request);
        }
        else if (message.contains("mon compte") || message.contains("profil") || message.contains("mes infos")) {
            return handleUserInfo(request);
        }
        else if (message.contains("bonjour") || message.contains("salut") || message.contains("coucou")) {
            return handleGreeting(request);
        }
        else {
            return handleHelp(request);
        }
    }
    
    private ChatResponse handleProductSearch(ChatRequest request) {
        log.info("🔍 Recherche de produits");
        
        try {
            String query = extractSearchQuery(request.getMessage());
            log.info("🔎 Terme recherché: {}", query);
            
            List<Map<String, Object>> products = productClient.searchProducts(query, null);
            
            if (products == null || products.isEmpty()) {
                log.warn("⚠️ Aucun produit trouvé pour: {}", query);
                return ChatResponse.builder()
                    .response(String.format("❌ Aucun produit trouvé pour \"%s\".\n\nProduits disponibles:\n• iPhone 15\n• Test Product\n• a\n\nEssayez \"iPhone 15\" ou \"Test Product\"", query))
                    .sessionId(request.getSessionId())
                    .intent("PRODUCT_SEARCH")
                    .suggestions(List.of("📱 iPhone 15", "📦 Test Product", "🔍 Autre recherche"))
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            StringBuilder response = new StringBuilder();
            response.append(String.format("🔍 **%d produit(s) trouvé(s) pour \"%s\":**\n\n", products.size(), query));
            
            for (int i = 0; i < Math.min(5, products.size()); i++) {
                Map<String, Object> p = products.get(i);
                String name = p.get("name").toString();
                double price = ((Number) p.get("price")).doubleValue();
                int stock = ((Number) p.get("stock")).intValue();
                String category = p.get("category").toString();
                String stockStatus = stock > 0 ? "✅ En stock" : "❌ Rupture";
                
                response.append(String.format("• **%s**\n  💰 %.2f € | 📦 %s | 🏷️ %s\n\n", name, price, stockStatus, category));
            }
            
            return ChatResponse.builder()
                .response(response.toString())
                .sessionId(request.getSessionId())
                .intent("PRODUCT_SEARCH")
                .suggestions(List.of("📱 Voir détails", "🛒 Ajouter au panier", "🔍 Nouvelle recherche"))
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erreur recherche: {}", e.getMessage(), e);
            return ChatResponse.builder()
                .response("🔍 Voici les produits disponibles:\n\n• iPhone 15 - 900.00€\n• Test Product - 99.99€\n• a - 1.00€\n\nQue souhaitez-vous savoir ?")
                .sessionId(request.getSessionId())
                .intent("PRODUCT_SEARCH")
                .suggestions(List.of("📱 iPhone 15", "📦 Test Product", "🔍 Autre recherche"))
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    private ChatResponse handleOrderStatus(ChatRequest request) {
        log.info("📋 Recherche statut commande");
        
        String orderId = extractOrderId(request.getMessage());
        if (orderId == null) {
            return ChatResponse.builder()
                .response("📋 Pour connaître le statut de votre commande, veuillez me donner son numéro.\n\nExemple: \"Quel est le statut de ma commande #12345 ?\"")
                .sessionId(request.getSessionId())
                .intent("ORDER_STATUS")
                .suggestions(List.of("📋 Voir toutes mes commandes", "❓ Aide"))
                .timestamp(LocalDateTime.now())
                .build();
        }
        
        try {
            Map<String, Object> order = orderClient.getOrderStatus(orderId, request.getUserId());
            String status = order.getOrDefault("status", "En traitement").toString();
            
            String statusEmoji;
            switch (status.toUpperCase()) {
                case "DELIVERED": statusEmoji = "✅"; break;
                case "SHIPPED": statusEmoji = "📦"; break;
                case "PROCESSING": statusEmoji = "⚙️"; break;
                case "CANCELLED": statusEmoji = "❌"; break;
                default: statusEmoji = "⏳";
            }
            
            return ChatResponse.builder()
                .response(String.format("📋 **Commande #%s**\n\n%s Statut: %s\n\nSouhaitez-vous plus de détails ?", orderId, statusEmoji, status))
                .sessionId(request.getSessionId())
                .intent("ORDER_STATUS")
                .suggestions(List.of("📋 Voir toutes mes commandes", "❌ Annuler", "🔄 Actualiser"))
                .timestamp(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("❌ Erreur statut commande: {}", e.getMessage());
            return ChatResponse.builder()
                .response("📋 Je n'ai pas pu récupérer le statut de votre commande. Vérifiez que le numéro est correct.\n\nExemple: #12345")
                .sessionId(request.getSessionId())
                .intent("ORDER_STATUS")
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    private ChatResponse handleUserInfo(ChatRequest request) {
        log.info("👤 Recherche infos utilisateur: {}", request.getUserId());
        
        try {
            Map<String, Object> user = userClient.getUserInfo(request.getUserId());
            String username = user.getOrDefault("username", "Utilisateur").toString();
            String email = user.getOrDefault("email", "non renseigné").toString();
            
            return ChatResponse.builder()
                .response(String.format("👤 **Votre profil**\n\n• Nom d'utilisateur: %s\n• Email: %s\n• ID: %s\n\nSouhaitez-vous modifier vos informations ?", 
                    username, email, request.getUserId()))
                .sessionId(request.getSessionId())
                .intent("USER_INFO")
                .suggestions(List.of("✏️ Modifier profil", "📋 Mes commandes", "🔐 Changer mot de passe"))
                .timestamp(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            log.error("❌ Erreur infos utilisateur: {}", e.getMessage());
            return ChatResponse.builder()
                .response("👤 Je n'ai pas pu récupérer vos informations. Veuillez vous reconnecter.")
                .sessionId(request.getSessionId())
                .intent("USER_INFO")
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    private ChatResponse handleGreeting(ChatRequest request) {
        return ChatResponse.builder()
            .response("👋 Bonjour! Je suis votre assistant virtuel. Comment puis-je vous aider aujourd'hui ?\n\n" +
                "🔍 **Rechercher des produits**\n" +
                "   → \"Je cherche un iPhone\"\n\n" +
                "📋 **Suivre une commande**\n" +
                "   → \"Quel est le statut de ma commande #12345 ?\"\n\n" +
                "👤 **Mon compte**\n" +
                "   → \"Mon profil\"\n\n" +
                "Que souhaitez-vous faire ?")
            .sessionId(request.getSessionId())
            .intent("GREETING")
            .suggestions(List.of("📱 Chercher un iPhone", "📋 Mes commandes", "👤 Mon profil", "❓ Aide"))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private ChatResponse handleHelp(ChatRequest request) {
        return ChatResponse.builder()
            .response("📚 **Aide - Ce que je peux faire**\n\n" +
                "🔍 **Rechercher des produits**\n" +
                "   • \"Je cherche un iPhone\"\n" +
                "   • \"Montre-moi les produits\"\n\n" +
                "📋 **Suivre une commande**\n" +
                "   • \"Quel est le statut de ma commande #12345 ?\"\n\n" +
                "👤 **Gérer mon compte**\n" +
                "   • \"Mon profil\"\n" +
                "   • \"Mes informations\"\n\n" +
                "Comment puis-je vous aider ?")
            .sessionId(request.getSessionId())
            .intent("HELP")
            .suggestions(List.of("🔍 Rechercher", "📋 Mes commandes", "👤 Mon profil"))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private String extractSearchQuery(String message) {
        String query = message.toLowerCase()
            .replaceAll("(cherche|recherche|trouve|produit|article|un|une|des|le|la|les|je|veux)", "")
            .replaceAll("[?!.,;]", "")
            .trim();
        
        if (query.isEmpty()) {
            return "produit";
        }
        return query;
    }
    
    private String extractOrderId(String message) {
        var matcher = ORDER_ID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
}
