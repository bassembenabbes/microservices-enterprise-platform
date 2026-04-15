package com.microservices.chatbot.exception;

/**
 * Exception levée lors de la détection d'intention
 */
public class IntentDetectionException extends ChatbotException {
    
    public IntentDetectionException(String message) {
        super("INTENT_DETECTION_ERROR", message);
    }
    
    public IntentDetectionException(String message, Throwable cause) {
        super("INTENT_DETECTION_ERROR", message, cause);
    }
}

