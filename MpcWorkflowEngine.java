package com.microservices.chatbot.service;

import com.microservices.chatbot.client.OrderServiceClient;
import com.microservices.chatbot.client.ProductServiceClient;
import com.microservices.chatbot.client.UserServiceClient;
import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import com.microservices.chatbot.model.ChatSession;
import com.microservices.chatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpcWorkflowEngine {
    
    private final ChatClient chatClient;
    private final RAGService ragService;
    private final UserServiceClient userClient;
    private final ProductServiceClient productClient;
    private final OrderServiceClient orderClient;
    private final ChatSessionRepository sessionRepository;
    
    private static final Map<String, ContractDefinition> CONTRACTS = new HashMap<>();
    
    static {
        CONTRACTS.put("PRODUCT_SEARCH", new ContractDefinition(
            List.of("query"),
            List.of("category", "minPrice", "maxPrice")
        ));
        CONTRACTS.put("ORDER_STATUS", new ContractDefinition(
            List.of("orderId"),
            List.of("userId")
        ));
        CONTRACTS.put("USER_INFO", new ContractDefinition(
            List.of("userId"),
            List.of()
        ));
    }
    
    public ChatResponse processMessage(ChatRequest request) {
        log.info("🔄 MPC Processing: {}", request.getMessage());
        
        // 1. Récupération session
        ChatSession session = getOrCreateSession(request);
        
        // 2. Détection d'intention avec Gemini
        String intent = detectIntent(request.getMessage());
        log.info("🎯 Intent detected: {}", intent);
        
        // 3. Validation du contrat MPC
        Map<String, Object> extractedParams = extractParameters(request.getMessage());
        var validation = validateContract(intent, extractedParams);
        
        if (!validation.valid) {
            return buildMissingFieldsResponse(validation.missingFields, session.getSessionId());
        }
        
        // 4. Exécution de l'action
        Map<String, Object> result = executeAction(intent, extractedParams, request.getUserId());
        
        // 5. Génération de la réponse avec Gemini + RAG
        String context = ragService.buildContext(request.getMessage());
        String response = generateResponse(intent, result, context);
        
        // 6. Mise à jour session
        updateSession(session, intent, result);
        
        return ChatResponse.builder()
            .response(response)
            .sessionId(session.getSessionId())
            .intent(intent)
            .action(intent)
            .contractState("COMPLETED")
            .suggestions(generateSuggestions(intent))
            .data(result)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private String detectIntent(String message) {
        String prompt = """
            Analyse le message suivant et retourne UNIQUEMENT le nom de l'intention.
            
            Intentions possibles:
            - PRODUCT_SEARCH: recherche de produit, "je cherche", "quel produit"
            - ORDER_STATUS: statut de commande, "où est ma commande"
            - USER_INFO: informations utilisateur, "mon compte"
            - HELP: aide, assistance
            - GREETING: bonjour, salut
            
            Message: %s
            Intention:""".formatted(message);
        
        return chatClient.prompt().user(prompt).call().content().trim().toUpperCase();
    }
    
    private Map<String, Object> extractParameters(String message) {
        Map<String, Object> params = new HashMap<>();
        
        // Extraction simple (peut être améliorée avec Gemini)
        var orderIdPattern = java.util.regex.Pattern.compile("#?(\\d{5,})");
        var matcher = orderIdPattern.matcher(message);
        if (matcher.find()) {
            params.put("orderId", matcher.group(1));
        }
        
        // Extraction de query pour recherche
        if (message.contains("cherche") || message.contains("recherche")) {
            String query = message.replaceAll(".*(cherche|recherche)\\s+", "").trim();
            if (!query.isEmpty()) params.put("query", query);
        }
        
        return params;
    }
    
    private ValidationResult validateContract(String intent, Map<String, Object> params) {
        var contract = CONTRACTS.get(intent);
        if (contract == null) {
            return new ValidationResult(false, List.of("intention_non_reconnue"));
        }
        
        List<String> missing = new ArrayList<>();
        for (String required : contract.requiredFields) {
            if (!params.containsKey(required)) {
                missing.add(required);
            }
        }
        
        return new ValidationResult(missing.isEmpty(), missing);
    }
    
    private Map<String, Object> executeAction(String intent, Map<String, Object> params, String userId) {
        return switch (intent) {
            case "PRODUCT_SEARCH" -> {
                String query = (String) params.getOrDefault("query", "");
                String category = (String) params.get("category");
                var products = productClient.searchProducts(query, category);
                yield Map.of("products", products, "count", products.size());
            }
            case "ORDER_STATUS" -> {
                String orderId = (String) params.get("orderId");
                yield orderClient.getOrderStatus(orderId, userId);
            }
            case "USER_INFO" -> userClient.getUserInfo(userId);
            default -> Map.of();
        };
    }
    
    private String generateResponse(String intent, Map<String, Object> result, String context) {
        String prompt = """
            Contexte: %s
            
            Intention: %s
            Résultat: %s
            
            Génère une réponse naturelle et professionnelle pour l'utilisateur.
            Sois concis, poli et utile.
            """.formatted(context, intent, result);
        
        return chatClient.prompt().user(prompt).call().content();
    }
    
    private ChatResponse buildMissingFieldsResponse(List<String> missingFields, String sessionId) {
        String message = "Pour compléter votre demande, j'ai besoin des informations suivantes: " + 
            String.join(", ", missingFields);
        
        return ChatResponse.builder()
            .response(message)
            .sessionId(sessionId)
            .contractState("PENDING")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private List<String> generateSuggestions(String intent) {
        return switch (intent) {
            case "PRODUCT_SEARCH" -> List.of("🔍 iPhone", "📱 Samsung", "💻 Ordinateur");
            case "ORDER_STATUS" -> List.of("📋 Voir mes commandes", "❌ Annuler");
            case "USER_INFO" -> List.of("✏️ Modifier profil", "🔐 Changer mot de passe");
            default -> List.of("📦 Rechercher", "📋 Commandes", "👤 Profil", "❓ Aide");
        };
    }
    
    private ChatSession getOrCreateSession(ChatRequest request) {
        String sessionId = request.getSessionId() != null ? 
            request.getSessionId() : "session_" + request.getUserId() + "_" + System.currentTimeMillis();
        
        return sessionRepository.findBySessionId(sessionId)
            .orElseGet(() -> sessionRepository.save(ChatSession.builder()
                .sessionId(sessionId)
                .userId(request.getUserId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .variables(new HashMap<>())
                .build()));
    }
    
    private void updateSession(ChatSession session, String intent, Map<String, Object> result) {
        session.setCurrentIntent(intent);
        session.setUpdatedAt(LocalDateTime.now());
        result.forEach((k, v) -> session.getVariables().put(k, v != null ? v.toString() : ""));
        sessionRepository.save(session);
    }
    
    record ContractDefinition(List<String> requiredFields, List<String> optionalFields) {}
    record ValidationResult(boolean valid, List<String> missingFields) {}
}
