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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {
    
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;
    
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("#?(\\d{5,})");
    
    public ChatResponse processMessage(ChatRequest request) {
        log.info("Processing message: {}", request.getMessage());
        
        String message = request.getMessage().toLowerCase();
        ChatResponse.ChatResponseBuilder responseBuilder = ChatResponse.builder()
            .sessionId(request.getSessionId())
            .timestamp(LocalDateTime.now());
        
        // Détection d'intention simple
        if (message.matches(".*\\b(bonjour|salut|coucou|hello|hi)\\b.*")) {
            return handleGreeting(responseBuilder);
        }
        else if (message.matches(".*\\b(commande|order|status|suivi)\\b.*") && 
                 (message.contains("statut") || message.contains("status") || message.contains("suivi"))) {
            return handleOrderStatus(message, responseBuilder, request);
        }
        else if (message.matches(".*\\b(produit|recherche|cherche|catalogue|article)\\b.*")) {
            return handleProductSearch(message, responseBuilder, request);
        }
        else if (message.matches(".*\\b(commander|acheter|passer commande|nouvelle commande)\\b.*")) {
            return handleCreateOrder(responseBuilder, request);
        }
        else if (message.matches(".*\\b(mon compte|profil|mes infos|user)\\b.*")) {
            return handleUserInfo(responseBuilder, request);
        }
        else if (message.matches(".*\\b(aide|help|assistance|que faire|comment)\\b.*")) {
            return handleHelp(responseBuilder);
        }
        else {
            return handleUnknown(responseBuilder);
        }
    }
    
    private ChatResponse handleGreeting(ChatResponse.ChatResponseBuilder builder) {
        return builder
            .response("👋 Bonjour! Je suis votre assistant virtuel. Comment puis-je vous aider aujourd'hui?\n\n" +
                "Voici ce que je peux faire:\n" +
                "• 📦 Rechercher des produits\n" +
                "• 📋 Suivre vos commandes\n" +
                "• 👤 Consulter votre profil\n" +
                "• 🛒 Passer une commande")
            .intent("GREETING")
            .suggestions(List.of("📦 Rechercher un produit", "📋 Mes commandes", "👤 Mon profil", "❓ Aide"))
            .build();
    }
    
    private ChatResponse handleOrderStatus(String message, ChatResponse.ChatResponseBuilder builder, ChatRequest request) {
        var matcher = ORDER_ID_PATTERN.matcher(message);
        
        if (matcher.find()) {
            String orderId = matcher.group(1);
            try {
                Map<String, Object> order = orderServiceClient.getOrderStatus(orderId, request.getUserId());
                String status = (String) order.getOrDefault("status", "Inconnu");
                Double total = (Double) order.getOrDefault("totalAmount", 0.0);
                
                return builder
                    .response(String.format("📋 **Statut de votre commande #%s**\n\n" +
                        "Statut: %s\n" +
                        "Montant total: %.2f €\n\n" +
                        "Souhaitez-vous plus de détails?", orderId, status, total))
                    .intent("ORDER_STATUS")
                    .data(Map.of("orderId", orderId, "status", status, "total", total))
                    .suggestions(List.of("📋 Voir toutes mes commandes", "❌ Annuler cette commande", "🔄 Autre demande"))
                    .build();
            } catch (Exception e) {
                log.error("Error getting order status: {}", e.getMessage());
                return builder
                    .response("❌ Je n'ai pas pu récupérer le statut de votre commande. Vérifiez que le numéro est correct.")
                    .intent("ORDER_STATUS")
                    .build();
            }
        } else {
            return builder
                .response("📋 Pour connaître le statut de votre commande, veuillez me donner son numéro.\n\n" +
                    "Exemple: \"Quel est le statut de ma commande #12345?\"")
                .intent("ORDER_STATUS")
                .contractState("PENDING")
                .suggestions(List.of("📋 Voir toutes mes commandes", "❓ Aide"))
                .build();
        }
    }
    
    private ChatResponse handleProductSearch(String message, ChatResponse.ChatResponseBuilder builder, ChatRequest request) {
        // Extraire le terme de recherche
        String searchTerm = message
            .replaceAll(".*\\b(recherche|cherche|trouve|produit|article)\\b", "")
            .replaceAll("(\\?|!|\\.|,|je|veux|un|une|des|le|la|les|du|de|des)", "")
            .trim();
        
        if (searchTerm.isEmpty()) {
            return builder
                .response("🔍 Que souhaitez-vous rechercher? Donnez-moi un nom de produit ou une catégorie.\n\n" +
                    "Exemples:\n" +
                    "• \"Je cherche un iPhone\"\n" +
                    "• \"Des écouteurs sans fil\"\n" +
                    "• \"Ordinateur portable\"")
                .intent("PRODUCT_SEARCH")
                .suggestions(List.of("📱 iPhone", "💻 Ordinateur", "🎧 Audio", "⌚ Montre connectée"))
                .build();
        }
        
        try {
            List<Map<String, Object>> products = productServiceClient.searchProducts(searchTerm, null);
            
            if (products.isEmpty()) {
                return builder
                    .response(String.format("❌ Je n'ai pas trouvé de produit correspondant à \"%s\".\n\n" +
                        "Essayez avec d'autres mots-clés ou consultez notre catalogue.", searchTerm))
                    .intent("PRODUCT_SEARCH")
                    .suggestions(List.of("🔍 Nouvelle recherche", "📋 Voir catalogue", "❓ Aide"))
                    .build();
            }
            
            StringBuilder response = new StringBuilder();
            response.append(String.format("🔍 **Résultats pour \"%s\"**\n\n", searchTerm));
            
            for (int i = 0; i < Math.min(3, products.size()); i++) {
                Map<String, Object> product = products.get(i);
                response.append(String.format("• **%s** - %.2f € (Stock: %d)\n",
                    product.get("name"),
                    product.get("price"),
                    product.get("stock")));
            }
            
            if (products.size() > 3) {
                response.append(String.format("\n... et %d autres résultats.", products.size() - 3));
            }
            
            response.append("\n\nSouhaitez-vous plus de détails sur un produit?");
            
            return builder
                .response(response.toString())
                .intent("PRODUCT_SEARCH")
                .data(Map.of("products", products, "count", products.size()))
                .suggestions(List.of("📦 Voir détails", "🛒 Ajouter au panier", "🔍 Nouvelle recherche"))
                .build();
                
        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage());
            return builder
                .response("❌ Le service de recherche est momentanément indisponible. Veuillez réessayer plus tard.")
                .intent("PRODUCT_SEARCH")
                .build();
        }
    }
    
    private ChatResponse handleCreateOrder(ChatResponse.ChatResponseBuilder builder, ChatRequest request) {
        return builder
            .response("🛒 Pour passer une commande, veuillez:\n\n" +
                "1️⃣ Ajouter des produits à votre panier\n" +
                "2️⃣ Aller dans l'onglet 'Cart'\n" +
                "3️⃣ Remplir l'adresse de livraison\n" +
                "4️⃣ Valider la commande\n\n" +
                "Souhaitez-vous que je vous guide?")
            .intent("CREATE_ORDER")
            .suggestions(List.of("➕ Ajouter au panier", "📦 Voir mon panier", "🏠 Adresse de livraison"))
            .build();
    }
    
    private ChatResponse handleUserInfo(ChatResponse.ChatResponseBuilder builder, ChatRequest request) {
        try {
            Map<String, Object> userInfo = userServiceClient.getUserInfo(request.getUserId());
            
            return builder
                .response(String.format("👤 **Votre profil**\n\n" +
                    "• Nom d'utilisateur: %s\n" +
                    "• Email: %s\n" +
                    "• ID: %s\n\n" +
                    "Souhaitez-vous modifier vos informations?",
                    userInfo.getOrDefault("username", "Non défini"),
                    userInfo.getOrDefault("email", "Non défini"),
                    request.getUserId()))
                .intent("USER_INFO")
                .data(userInfo)
                .suggestions(List.of("✏️ Modifier mon profil", "🔐 Changer mot de passe", "📋 Mes commandes"))
                .build();
        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            return builder
                .response("❌ Je n'ai pas pu récupérer vos informations. Veuillez vous reconnecter.")
                .intent("USER_INFO")
                .build();
        }
    }
    
    private ChatResponse handleHelp(ChatResponse.ChatResponseBuilder builder) {
        return builder
            .response("📚 **Aide et Assistance**\n\n" +
                "Voici ce que je peux faire pour vous:\n\n" +
                "🔍 **Rechercher des produits**\n" +
                "   → \"Je cherche un iPhone\"\n\n" +
                "📋 **Suivre une commande**\n" +
                "   → \"Quel est le statut de ma commande #12345?\"\n\n" +
                "👤 **Consulter mon profil**\n" +
                "   → \"Mon compte\" ou \"Mes informations\"\n\n" +
                "🛒 **Passer une commande**\n" +
                "   → \"Je veux commander\"\n\n" +
                "❓ **Aide**\n" +
                "   → \"Aide\" ou \"Que pouvez-vous faire?\"\n\n" +
                "Comment puis-je vous aider?")
            .intent("HELP")
            .suggestions(List.of("📦 Rechercher", "📋 Mes commandes", "👤 Mon profil", "🛒 Commander"))
            .build();
    }
    
    private ChatResponse handleUnknown(ChatResponse.ChatResponseBuilder builder) {
        return builder
            .response("❓ Je n'ai pas bien compris votre demande.\n\n" +
                "Pour voir ce que je peux faire, dites **'aide'**.\n\n" +
                "Ou essayez l'une des suggestions ci-dessous.")
            .intent("UNKNOWN")
            .suggestions(List.of("❓ Aide", "📦 Rechercher des produits", "📋 Mes commandes", "👤 Mon compte"))
            .build();
    }
}
