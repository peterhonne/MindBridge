package com.mindbridge.ai.agent.orchestrator.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateJournalEntryRequest {
    private String title;
    private String content;
    private Integer moodBefore;
    private Integer moodAfter;
    private List<String> tags;
}
