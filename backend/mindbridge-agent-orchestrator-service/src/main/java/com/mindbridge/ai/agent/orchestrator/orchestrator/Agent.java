package com.mindbridge.ai.agent.orchestrator.orchestrator;

import java.util.List;

public record Agent(
        String name,
        String systemPrompt,
        List<Object> tools,
        List<String> routableAgents
) {}
