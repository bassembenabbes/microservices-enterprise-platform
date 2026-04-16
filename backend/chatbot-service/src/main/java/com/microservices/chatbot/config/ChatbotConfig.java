package com.microservices.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Chatbot service
 */
@Component
@ConfigurationProperties(prefix = "chatbot")
@Data
public class ChatbotConfig {
    
    private boolean useGemini = true;
    private int maxSessionHistory = 50;
    private int sessionTimeoutSeconds = 3600;
    private String apiKey;
}
