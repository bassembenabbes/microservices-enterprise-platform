package com.microservices.chatbot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * DTO pour les réponses d'erreur
 */
@Getter
public class ErrorResponse {
    
    private final String errorCode;
    private final String message;
    private final String timestamp;
    private final String path;
    private final int status;
    
    public ErrorResponse(String errorCode, String message, String path, HttpStatus status) {
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.status = status.value();
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}

