package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.orchestrator.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AgentOrchestrator agentOrchestrator;

    private final UserProfileService userProfileService;

    private final InputGuardrailService inputGuardrailService;

    public String chat(String keycloakUserId, String userMsg) {
        boolean shouldProcessMessage = inputGuardrailService.shouldProcessMessage(userMsg, keycloakUserId);
        if (!shouldProcessMessage) return InputGuardrailService.GENERAL_BLOCKED_RESPONSE;
        // routing workflow
        // TODO:load from db config
        // TODO if user doesn't have previous consultation history, rag knowledge base instead, or do not rag, just use base chat
        User user = userProfileService.getUser(keycloakUserId);

        return agentOrchestrator.routeUserInput(userMsg, user.getId(), keycloakUserId);

    }



}
