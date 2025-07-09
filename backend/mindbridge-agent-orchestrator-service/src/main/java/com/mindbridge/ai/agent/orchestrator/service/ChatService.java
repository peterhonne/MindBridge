package com.mindbridge.ai.agent.orchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
import static org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever.FILTER_EXPRESSION;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    public String chat(String userId, String userMsg) {
        return chatClient.prompt()
                .user(userMsg)
                // TODO if user doesn't have previous consultation history, rag knowledge base instead, or do not rag, just use base chat
                .advisors(advisor -> advisor.param(CONVERSATION_ID, userId).param(FILTER_EXPRESSION, "user_id == '" + userId + "'"))
                .call()
                .content();
    }

}
