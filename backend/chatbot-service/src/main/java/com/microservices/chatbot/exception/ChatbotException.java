package com.microservices.chatbot.exception;

/**
 * Exception personnalisée pour les erreurs du chatbot
 */
public class ChatbotException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;
    
    public ChatbotException(String errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public ChatbotException(String errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}

