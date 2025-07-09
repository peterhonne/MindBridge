package com.mindbridge.ai.agent.orchestrator.models.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MoodEntryDto {
    private String code;
    private Integer moodScore;
    private List<String> moodTags;
    private String notes;
    private LocalDateTime createdAt;
}
