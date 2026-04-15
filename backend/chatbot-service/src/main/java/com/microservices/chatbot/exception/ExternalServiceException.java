package com.microservices.chatbot.exception;

/**
 * Exception levée lors d'une erreur de communication avec un service externe
 */
public class ExternalServiceException extends ChatbotException {
    
    private final String serviceName;
    
    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", "Erreur lors de l'appel au service: " + serviceName);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", "Erreur lors de l'appel au service: " + serviceName, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}

