package com.mindbridge.ai.agent.orchestrator.models.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class MoodStatsDto {
    private Double averageMoodScore;
    private Integer highestMoodScore;
    private Integer lowestMoodScore;
    private List<String> commonMoodTags;
    private Map<String, Integer> moodTrendData; // Date -> Mood Score
}
