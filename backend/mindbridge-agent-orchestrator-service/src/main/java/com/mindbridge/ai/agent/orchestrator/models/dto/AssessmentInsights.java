package com.mindbridge.ai.agent.orchestrator.models.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AssessmentInsights {
    private String emotionalState;
    private String trendAnalysis;
    private LocalDateTime analysisTimestamp;
}
