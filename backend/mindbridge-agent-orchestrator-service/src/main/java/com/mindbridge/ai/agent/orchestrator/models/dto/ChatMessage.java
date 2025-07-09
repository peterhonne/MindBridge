package com.mindbridge.ai.agent.orchestrator.models.dto;

import java.util.Date;

public record ChatMessage(String content, Date timestamp, String type) {
}
