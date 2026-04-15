package com.microservices.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * DTO pour les requêtes de chat
 */
@Data
public class ChatRequest {
    
    @NotBlank(message = "L'ID utilisateur est requis")
    private String userId;
    
    @NotBlank(message = "L'ID de session est requis")
    private String sessionId;
    
    @NotNull(message = "Le message est requis")
    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(min = 1, max = 1000, message = "Le message doit contenir entre 1 et 1000 caractères")
    private String message;
    
    private Map<String, Object> context;
}
