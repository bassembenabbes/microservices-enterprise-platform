package com.microservices.chatbot.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ChatRequest {
    private String userId;
    private String sessionId;
    private String message;
    private Map<String, Object> context;
}
