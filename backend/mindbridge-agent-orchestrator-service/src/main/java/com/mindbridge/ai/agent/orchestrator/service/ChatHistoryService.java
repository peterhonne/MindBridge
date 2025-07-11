package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.orchestrator.component.CustomChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.models.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
    private final CustomChatMemoryRepository customChatMemoryRepository;
    public ChatHistoryService(CustomChatMemoryRepository customChatMemoryRepository) {
        this.customChatMemoryRepository = customChatMemoryRepository;
    }


    public List<ChatMessageDto> getChatHistory(String userId) {
        List<ChatMessageDto> chatMessages = new ArrayList<>();
        List<Message> messageList = customChatMemoryRepository.findByConversationId(userId);
        if (ObjectUtils.isEmpty(messageList)) {
            customChatMemoryRepository.saveAll(userId, List.of(new AssistantMessage("Hello, I’m here.")));
        }
        messageList = customChatMemoryRepository.findByConversationId(userId);
        messageList.stream().filter(message -> message.getMessageType().equals(MessageType.ASSISTANT) || message.getMessageType().equals(MessageType.USER))
                .forEach(message -> {
                    chatMessages.add(new ChatMessageDto(message.getText(), LocalDateTime.parse((String) message.getMetadata().get("createAt")), message.getMessageType().toString().toLowerCase()));
                });
        return chatMessages;
    }
}
