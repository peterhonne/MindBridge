package com.mindbridge.ai.agent.orchestrator.models.dto;


import com.mindbridge.ai.agent.orchestrator.enums.TherapyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDto {
    private String username;
    private String email;
    private TherapyStatus therapyStatus;
}
