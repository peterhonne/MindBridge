package com.mindbridge.ai.agent.orchestrator.repository;

import com.mindbridge.ai.agent.orchestrator.models.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PgChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
