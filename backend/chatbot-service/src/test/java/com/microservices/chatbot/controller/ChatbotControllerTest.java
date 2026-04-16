package com.microservices.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.chatbot.dto.ChatRequest;
import com.microservices.chatbot.dto.ChatResponse;
import com.microservices.chatbot.service.ChatbotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour ChatbotController
 * 
 * À exécuter: mvn test -Dtest=ChatbotControllerTest
 */
@WebMvcTest(ChatbotController.class)
@DisplayName("ChatbotController Tests")
class ChatbotControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ChatbotService chatbotService;
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Succès avec message valide")
    void testChatSuccess() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setUserId("user-123");
        request.setSessionId("session-456");
        request.setMessage("Je cherche un iPhone");
        
        ChatResponse response = ChatResponse.builder()
            .response("Nous avons trouvé 3 iPhones disponibles")
            .sessionId("session-456")
            .intent("PRODUCT_SEARCH")
            .timestamp(LocalDateTime.now())
            .build();
        
        when(chatbotService.processMessage(any(ChatRequest.class)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Nous avons trouvé 3 iPhones disponibles"))
            .andExpect(jsonPath("$.intent").value("PRODUCT_SEARCH"))
            .andExpect(jsonPath("$.sessionId").value("session-456"));
    }
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Erreur validation message vide")
    void testChatWithEmptyMessage() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setUserId("user-123");
        request.setSessionId("session-456");
        request.setMessage("");
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Erreur userId manquant")
    void testChatWithoutUserId() throws Exception {
        // Arrange
        String requestBody = """
            {
                "sessionId": "session-456",
                "message": "Bonjour"
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Erreur message trop long")
    void testChatWithTooLongMessage() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setUserId("user-123");
        request.setSessionId("session-456");
        request.setMessage("x".repeat(1001)); // Dépasse la limite de 1000
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
    
    @Test
    @DisplayName("GET /api/chatbots/health - Vérifier la santé du service")
    void testHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/chatbots/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("chatbot-service"))
            .andExpect(jsonPath("$.version").value("1.0.0"));
    }
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Intent détection PRODUCT_SEARCH")
    void testProductSearchIntent() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setUserId("user-123");
        request.setSessionId("session-456");
        request.setMessage("Cherche un iPhone 15");
        
        ChatResponse response = ChatResponse.builder()
            .response("Nous avons trouvé les iPhones 15")
            .sessionId("session-456")
            .intent("PRODUCT_SEARCH")
            .action("search")
            .timestamp(LocalDateTime.now())
            .build();
        
        when(chatbotService.processMessage(any(ChatRequest.class)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intent").value("PRODUCT_SEARCH"))
            .andExpect(jsonPath("$.action").value("search"));
    }
    
    @Test
    @DisplayName("POST /api/chatbots/chat - Intent détection ORDER_STATUS")
    void testOrderStatusIntent() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setUserId("user-123");
        request.setSessionId("session-456");
        request.setMessage("Quel est le statut de ma commande #12345?");
        
        ChatResponse response = ChatResponse.builder()
            .response("Votre commande #12345 est en cours de livraison")
            .sessionId("session-456")
            .intent("ORDER_STATUS")
            .timestamp(LocalDateTime.now())
            .build();
        
        when(chatbotService.processMessage(any(ChatRequest.class)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/chatbots/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intent").value("ORDER_STATUS"));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        public com.microservices.chatbot.config.ChatbotConfig chatbotConfig() {
            com.microservices.chatbot.config.ChatbotConfig config = new com.microservices.chatbot.config.ChatbotConfig();
            // Set default values for testing
            return config;
        }
    }
}
