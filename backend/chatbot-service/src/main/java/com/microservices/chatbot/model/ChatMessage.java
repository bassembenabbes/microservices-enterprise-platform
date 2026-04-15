package com.microservices.chatbot.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String sessionId;
    private String userId;
    private String role; // USER, ASSISTANT, SYSTEM
    private String content;
    private String intent;
    private String action;
    private String contractState;
    
    @Column(length = 2000)
    private String metadata;
    
    private LocalDateTime timestamp;
}
