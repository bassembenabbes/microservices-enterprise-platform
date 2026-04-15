package com.microservices.chatbot.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatResponse {
    private String response;
    private String sessionId;
    private String intent;
    private String action;
    private String contractState;
    private List<String> suggestions;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
