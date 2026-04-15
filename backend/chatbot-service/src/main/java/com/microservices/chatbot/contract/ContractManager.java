package com.microservices.chatbot.contract;

import com.microservices.chatbot.dto.ContractRequest;
import com.microservices.chatbot.dto.ContractResponse;
import com.microservices.chatbot.model.ChatSession;
import com.microservices.chatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractManager {
    
    private final ChatSessionRepository sessionRepository;
    private final Map<String, ContractDefinition> contracts = new HashMap<>();
    
    @jakarta.annotation.PostConstruct
    public void init() {
        // Définition des contrats pour chaque action
        registerContract("CREATE_ORDER", ContractDefinition.builder()
            .name("CREATE_ORDER")
            .description("Création d'une nouvelle commande")
            .requiredFields(List.of("userId", "items", "shippingAddress"))
            .optionalFields(List.of("couponCode", "paymentMethod", "notes"))
            .validationRules(Map.of(
                "items", "nonEmpty",
                "shippingAddress", "nonEmpty"
            ))
            .nextStep("PAYMENT")
            .build());
        
        registerContract("GET_ORDER_STATUS", ContractDefinition.builder()
            .name("GET_ORDER_STATUS")
            .description("Consultation du statut d'une commande")
            .requiredFields(List.of("orderId", "userId"))
            .optionalFields(List.of())
            .build());
        
        registerContract("PRODUCT_SEARCH", ContractDefinition.builder()
            .name("PRODUCT_SEARCH")
            .description("Recherche de produits")
            .requiredFields(List.of("query"))
            .optionalFields(List.of("category", "minPrice", "maxPrice"))
            .build());
        
        registerContract("USER_INFO", ContractDefinition.builder()
            .name("USER_INFO")
            .description("Consultation des informations utilisateur")
            .requiredFields(List.of("userId"))
            .optionalFields(List.of())
            .build());
    }
    
    public void registerContract(String name, ContractDefinition contract) {
        contracts.put(name, contract);
    }
    
    public ContractResponse validateContract(ContractRequest request) {
        ContractDefinition contract = contracts.get(request.getContractType());
        
        if (contract == null) {
            return ContractResponse.builder()
                .validated(false)
                .message("Contrat non trouvé: " + request.getContractType())
                .build();
        }
        
        List<String> missingFields = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();
        
        // Vérification des champs requis
        for (String requiredField : contract.getRequiredFields()) {
            if (!request.getParameters().containsKey(requiredField)) {
                missingFields.add(requiredField);
            }
        }
        
        if (!missingFields.isEmpty()) {
            return ContractResponse.builder()
                .validated(false)
                .message("Champs requis manquants: " + String.join(", ", missingFields))
                .requiredFields(contract.getRequiredFields().stream()
                    .collect(HashMap::new, (m, f) -> m.put(f, "Requis"), HashMap::putAll))
                .nextStep(contract.getNextStep())
                .build();
        }
        
        // Validation des règles
        for (Map.Entry<String, String> rule : contract.getValidationRules().entrySet()) {
            Object value = request.getParameters().get(rule.getKey());
            if (!validateRule(rule.getKey(), value, rule.getValue())) {
                errors.put(rule.getKey(), "Validation échouée: " + rule.getValue());
            }
        }
        
        if (!errors.isEmpty()) {
            return ContractResponse.builder()
                .validated(false)
                .message("Erreurs de validation: " + String.join(", ", errors.values()))
                .build();
        }
        
        // Mise à jour de la session
        updateSessionContract(request);
        
        return ContractResponse.builder()
            .validated(true)
            .message("Contrat validé avec succès")
            .data(request.getParameters())
            .nextStep(contract.getNextStep())
            .build();
    }
    
    private boolean validateRule(String field, Object value, String rule) {
        switch (rule) {
            case "nonEmpty":
                return value != null && !value.toString().trim().isEmpty();
            case "positive":
                if (value instanceof Number) {
                    return ((Number) value).doubleValue() > 0;
                }
                return false;
            default:
                return true;
        }
    }
    
    private void updateSessionContract(ContractRequest request) {
        Optional<ChatSession> sessionOpt = sessionRepository.findBySessionId(request.getSessionId());
        
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setCurrentIntent(request.getContractType());
            session.setContractData(request.getParameters().toString());
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ContractDefinition {
        private String name;
        private String description;
        private List<String> requiredFields;
        private List<String> optionalFields;
        private Map<String, String> validationRules;
        private String nextStep;
    }
}
