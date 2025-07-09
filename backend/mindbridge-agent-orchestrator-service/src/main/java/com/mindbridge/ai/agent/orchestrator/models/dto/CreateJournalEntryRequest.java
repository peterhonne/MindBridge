package com.mindbridge.ai.agent.orchestrator.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateJournalEntryRequest {
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private Integer moodBefore;
    private Integer moodAfter;
    private List<String> tags;
}
