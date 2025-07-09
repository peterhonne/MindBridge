package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.advisor.RedisChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.models.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
    private final RedisChatMemoryRepository redisChatMemoryRepository;
    public ChatHistoryService(RedisChatMemoryRepository redisChatMemoryRepository) {
        this.redisChatMemoryRepository = redisChatMemoryRepository;
    }


    public List<ChatMessage> getChatHistory(String userId) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        List<Message> messageList = redisChatMemoryRepository.findByConversationId(userId);
        if (ObjectUtils.isEmpty(messageList)) {
            redisChatMemoryRepository.saveMessage(userId, new AssistantMessage("Hello, I’m here.", Map.of("createAt", new Date())));
        }
        messageList = redisChatMemoryRepository.findByConversationId(userId);
        messageList.stream().filter(message -> message.getMessageType().equals(MessageType.ASSISTANT) || message.getMessageType().equals(MessageType.USER))
                .forEach(message -> {
                    chatMessages.add(new ChatMessage(message.getText(), Date.from(OffsetDateTime.parse((String) message.getMetadata().get("createAt")).toInstant()), message.getMessageType().toString().toLowerCase()));
                });
        return chatMessages;
    }
}
