package com.mindbridge.ai.agent.orchestrator.models.dto;


import com.mindbridge.ai.agent.orchestrator.enums.TherapyStatus;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateUserProfileRequest {

    private TherapyStatus therapyStatus;
}