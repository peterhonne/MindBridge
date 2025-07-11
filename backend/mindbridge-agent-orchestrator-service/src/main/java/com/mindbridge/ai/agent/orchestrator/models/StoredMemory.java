package com.mindbridge.ai.agent.orchestrator.models;

import com.mindbridge.ai.agent.orchestrator.enums.MemoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoredMemory extends Memory {

    private String id;
    private String memoryId = UUID.randomUUID().toString();
    private LocalDateTime createdAt;
    private String userId;
    private String threadId;
    private MemoryType memoryType; // Optional override

    @Override
    public MemoryType getMemoryType() {
        return memoryType != null ? memoryType : super.getMemoryType();
    }

    @Override
    public void setMemoryType(MemoryType memoryType) {
        this.memoryType = memoryType;
    }

}
