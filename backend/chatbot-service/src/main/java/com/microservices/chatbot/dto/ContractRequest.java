package com.microservices.chatbot.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ContractRequest {
    private String contractType;
    private Map<String, Object> parameters;
    private String sessionId;
    private String userId;
}
