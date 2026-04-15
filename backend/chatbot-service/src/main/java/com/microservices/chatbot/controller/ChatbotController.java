package com.microservices.chatbot.controller;

import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import com.microservices.chatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API Controller pour les interactions avec le chatbot
 * Version 1.0.0 - API stable et recommandée
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbots")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Chatbot API", description = "Gestion des conversations avec le chatbot IA")
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    
    /**
     * Traiter un message utilisateur
     * 
     * @param request Requête de chat contenant userId, sessionId et message
     * @return Réponse générée par le chatbot
     */
    @PostMapping("/chat")
    @Operation(
        summary = "Envoyer un message au chatbot",
        description = "Traite un message utilisateur et retourne une réponse intelligente",
        tags = {"Chatbot API"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Réponse du chatbot générée avec succès",
        content = @Content(schema = @Schema(implementation = ChatResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Données de requête invalides"
    )
    @ApiResponse(
        responseCode = "503",
        description = "Service indisponible ou erreur lors du traitement"
    )
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("📨 Traitement du message - UserId: {}, SessionId: {}", 
                request.getUserId(), request.getSessionId());
        
        ChatResponse response = chatbotService.processMessage(request);
        
        log.info("✅ Réponse générée - Intent: {}", response.getIntent());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Vérifier la santé du service
     * 
     * @return État du service
     */
    @GetMapping("/health")
    @Operation(
        summary = "Vérifier la santé du service",
        description = "Endpoint de health check pour monitoring",
        tags = {"Health Check"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service en bonne santé"
    )
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "chatbot-service");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        log.debug("Health check OK");
        return ResponseEntity.ok(response);
    }
}
