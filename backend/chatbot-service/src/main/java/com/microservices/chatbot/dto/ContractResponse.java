package com.microservices.chatbot.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ContractResponse {
    private boolean validated;
    private String message;
    private Map<String, Object> data;
    private String nextStep;
    private Map<String, String> requiredFields;
}
