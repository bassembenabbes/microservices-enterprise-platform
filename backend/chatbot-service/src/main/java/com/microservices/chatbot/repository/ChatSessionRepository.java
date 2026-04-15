package com.microservices.chatbot.repository;

import com.microservices.chatbot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);
    void deleteByExpiresAtBefore(java.time.LocalDateTime expiresAt);
}
