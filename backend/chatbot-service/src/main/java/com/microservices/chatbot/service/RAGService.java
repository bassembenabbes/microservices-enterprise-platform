package com.microservices.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RAGService {
    
    private final Map<String, String> knowledgeBase = new ConcurrentHashMap<>();
    
    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
    @PostConstruct
    public void init() {
        if (!ragEnabled) return;
        
        log.info("📚 Initialisation de la base de connaissances RAG...");
        
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge/*.txt");
            
            for (Resource resource : resources) {
                loadDocument(resource);
            }
            
            log.info("✅ Base de connaissances initialisée avec {} documents", knowledgeBase.size());
            
        } catch (Exception e) {
            log.warn("⚠️ Aucun document trouvé: {}", e.getMessage());
        }
    }
    
    private void loadDocument(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            knowledgeBase.put(resource.getFilename(), content.toString());
            log.info("📄 Chargé: {}", resource.getFilename());
            
        } catch (Exception e) {
            log.error("Erreur chargement {}: {}", resource.getFilename(), e.getMessage());
        }
    }
    
    public String retrieveContext(String query, int topK) {
        if (!ragEnabled || knowledgeBase.isEmpty()) {
            return "";
        }
        
        log.info("🔍 Recherche RAG pour: {}", query);
        
        // Recherche simple par mots-clés
        String lowerQuery = query.toLowerCase();
        List<Map.Entry<String, String>> matches = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            String content = entry.getValue().toLowerCase();
            if (content.contains(lowerQuery)) {
                matches.add(entry);
            }
        }
        
        if (matches.isEmpty()) {
            log.info("Aucun document pertinent trouvé");
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("📚 **Informations contextuelles:**\n\n");
        
        int count = 0;
        for (Map.Entry<String, String> match : matches) {
            if (count >= topK) break;
            
            String content = match.getValue();
            // Extraire les passages pertinents
            String relevantPassage = extractRelevantPassage(content, lowerQuery, 500);
            context.append("📖 **").append(match.getKey()).append(":**\n");
            context.append(relevantPassage).append("\n\n");
            count++;
        }
        
        log.info("✅ {} documents pertinents trouvés", count);
        return context.toString();
    }
    
    private String extractRelevantPassage(String content, String query, int maxLength) {
        String lowerContent = content.toLowerCase();
        int index = lowerContent.indexOf(query);
        
        if (index >= 0) {
            int start = Math.max(0, index - 100);
            int end = Math.min(content.length(), index + 200);
            String passage = content.substring(start, end);
            if (start > 0) passage = "..." + passage;
            if (end < content.length()) passage = passage + "...";
            return passage;
        }
        
        // Retourner le début du document
        if (content.length() > maxLength) {
            return content.substring(0, maxLength) + "...";
        }
        return content;
    }
    
    public List<String> getSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("produit") || lowerQuery.contains("iphone") || lowerQuery.contains("samsung")) {
            suggestions.add("📱 iPhone 15 Pro - 1299.99€");
            suggestions.add("📱 Samsung Galaxy S24 - 1199.99€");
            suggestions.add("📱 Google Pixel 8 - 999.99€");
        } else if (lowerQuery.contains("livraison") || lowerQuery.contains("delivery")) {
            suggestions.add("🚚 Livraison standard: 3-5 jours");
            suggestions.add("⚡ Livraison express: 24h");
            suggestions.add("🎁 Livraison gratuite dès 50€");
        } else if (lowerQuery.contains("retour") || lowerQuery.contains("return")) {
            suggestions.add("🔄 Retour sous 14 jours");
            suggestions.add("💰 Remboursement sous 5-7 jours");
        } else if (lowerQuery.contains("paiement") || lowerQuery.contains("payment")) {
            suggestions.add("💳 Carte bancaire");
            suggestions.add("📱 PayPal");
            suggestions.add("🏦 Virement");
        } else {
            suggestions.add("🔍 Rechercher des produits");
            suggestions.add("📋 Suivre ma commande");
            suggestions.add("👤 Mon compte");
        }
        
        return suggestions;
    }
}
