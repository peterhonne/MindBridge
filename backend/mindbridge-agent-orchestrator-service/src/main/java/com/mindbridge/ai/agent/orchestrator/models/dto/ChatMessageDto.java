package com.mindbridge.ai.agent.orchestrator.models.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(String content, LocalDateTime timestamp, String type) {
}
