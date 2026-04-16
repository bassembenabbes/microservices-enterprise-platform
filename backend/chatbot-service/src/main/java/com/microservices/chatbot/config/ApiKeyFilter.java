package com.microservices.chatbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to validate API key for chatbot endpoints
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {
    
    private final ChatbotConfig chatbotConfig;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply to chatbot API endpoints
        if (path.startsWith("/api/chatbots/")) {
            String apiKey = request.getHeader("X-API-Key");
            
            if (chatbotConfig.getApiKey() != null && !chatbotConfig.getApiKey().isEmpty()) {
                if (apiKey == null || !apiKey.equals(chatbotConfig.getApiKey())) {
                    log.warn("Invalid or missing API key for request: {}", path);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Invalid API key\"}");
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
