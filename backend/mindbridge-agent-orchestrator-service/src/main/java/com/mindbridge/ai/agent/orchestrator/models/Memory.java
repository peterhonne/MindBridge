package com.mindbridge.ai.agent.orchestrator.models;

import com.mindbridge.ai.agent.orchestrator.enums.MemoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Memory {

    private String content;
    private MemoryType memoryType;
    private String metadata;

}
