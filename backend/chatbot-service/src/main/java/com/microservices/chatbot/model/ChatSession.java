package com.microservices.chatbot.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String sessionId;
    private String userId;
    private String context;
    
    @ElementCollection
    @CollectionTable(name = "session_variables")
    @MapKeyColumn(name = "var_key")
    @Column(name = "var_value", length = 2000)
    private Map<String, String> variables = new HashMap<>();
    
    private String currentIntent;
    private String currentAction;
    private String contractData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}
