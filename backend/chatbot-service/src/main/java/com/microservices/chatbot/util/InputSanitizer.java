package com.microservices.chatbot.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for input sanitization and security
 */
@Component
public class InputSanitizer {
    
    /**
     * Sanitize user input to prevent XSS and injection attacks
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("&", "&amp;")
                   .trim();
    }
    
    /**
     * Validate message length and content
     */
    public boolean isValidMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (message.length() > 1000) {
            return false;
        }
        
        // Check for suspicious patterns (basic)
        if (message.contains("<script") || message.contains("javascript:")) {
            return false;
        }
        
        return true;
    }
}
