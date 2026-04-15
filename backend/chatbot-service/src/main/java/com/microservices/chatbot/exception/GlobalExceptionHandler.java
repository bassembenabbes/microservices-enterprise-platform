package com.microservices.chatbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handler global pour les exceptions
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<ErrorResponse> handleChatbotException(
            ChatbotException ex,
            WebRequest request) {
        
        log.error("ChatbotException: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", ""),
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex,
            WebRequest request) {
        
        log.error("ExternalServiceException: Service={}, Message={}", 
                ex.getServiceName(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            "Service indisponible. Veuillez réessayer plus tard.",
            request.getDescription(false).replace("uri=", ""),
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    @ExceptionHandler(IntentDetectionException.class)
    public ResponseEntity<ErrorResponse> handleIntentDetectionException(
            IntentDetectionException ex,
            WebRequest request) {
        
        log.warn("IntentDetectionException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            "Impossible de comprendre votre demande. Pouvez-vous reformuler ?",
            request.getDescription(false).replace("uri=", ""),
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a + ", " + b);
        
        log.warn("Validation error: {}", message);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Données invalides: " + message,
            request.getDescription(false).replace("uri=", ""),
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Une erreur inattendue s'est produite. Veuillez contacter le support.",
            request.getDescription(false).replace("uri=", ""),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
