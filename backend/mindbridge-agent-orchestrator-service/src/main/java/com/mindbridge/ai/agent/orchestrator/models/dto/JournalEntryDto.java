package com.mindbridge.ai.agent.orchestrator.models.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JournalEntryDto {
    private String code;
    private String title;
    private String content;
    private Integer moodBefore;
    private Integer moodAfter;
    private List<String> tags;
    private LocalDateTime createdAt;
}
