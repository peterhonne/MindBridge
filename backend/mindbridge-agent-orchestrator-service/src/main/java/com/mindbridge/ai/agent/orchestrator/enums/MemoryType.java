package com.mindbridge.ai.agent.orchestrator.enums;

import lombok.Getter;

@Getter
public enum MemoryType {

    EPISODIC("episodic"), SEMANTIC("semantic");

    private final String value;

    MemoryType(String value) {
        this.value = value;
    }

    public static MemoryType fromValue(String value) {
        for (MemoryType type : MemoryType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown memory type: " + value);
    }
}
