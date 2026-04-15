package com.microservices.chatbot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
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
    private String currentIntent;
    private String currentAction;
    
    @ElementCollection
    @CollectionTable(name = "session_variables")
    @MapKeyColumn(name = "var_key")
    @Column(name = "var_value", length = 2000)
    private Map<String, String> variables = new HashMap<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}
