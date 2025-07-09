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

    private final ChatClient chatClient;

    private final AgentOrchestrator agentOrchestrator;

    private final ChatClient.Builder clientBuilder;

    private final UserProfileService userProfileService;

    public String chat(String keycloakUserId, String userMsg) {
        // routing workflow
        // TODO:load from db config
        // TODO if user doesn't have previous consultation history, rag knowledge base instead, or do not rag, just use base chat
        User user = userProfileService.getUser(keycloakUserId);

        return agentOrchestrator.routeUserInput(userMsg, user.getId(), keycloakUserId);

    }



}
