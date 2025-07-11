package com.mindbridge.ai.agent.orchestrator.orchestrator.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.ai.agent.orchestrator.models.entity.ChatMessage;
import com.mindbridge.ai.agent.orchestrator.repository.PgChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class CustomChatMemoryRepository implements ChatMemoryRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "chat:memory:";
    private final ObjectMapper objectMapper;
    private final PgChatMessageRepository pgChatMessageRepository;


    public CustomChatMemoryRepository(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, PgChatMessageRepository pgChatMessageRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pgChatMessageRepository = pgChatMessageRepository;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (ObjectUtils.isEmpty(keys)) return List.of();
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        String key = KEY_PREFIX + conversationId;
        List<String> jsonMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (ObjectUtils.isEmpty(jsonMessages)) return List.of();
        List<Message> messages = new ArrayList<>();
        for (String jsonMessage : jsonMessages) {
            try {
                Map<String, Object> messageMap = objectMapper.readValue(jsonMessage, Map.class);
                String messageTypeStr = (String) messageMap.get("messageType");
                MessageType messageType = MessageType.fromValue(messageTypeStr.toLowerCase());
                Message message = null;
                switch (messageType) {
                    case MessageType.USER:
                        message = UserMessage.builder()
                                .text((String) messageMap.get("text"))
                                .media((List<Media>) messageMap.get("media"))
                                .metadata((Map<String, Object>) messageMap.get("metadata"))
                                .build();
                        break;
                    case MessageType.ASSISTANT:
                        message = new AssistantMessage((String) messageMap.get("text"),
                                (Map<String, Object>) messageMap.get("metadata"),
                                (List<AssistantMessage.ToolCall>) messageMap.get("toolCalls"),
                                (List<Media>) messageMap.get("media"));
                        break;
                    case MessageType.SYSTEM:
                        message = SystemMessage.builder()
                                .text((String) messageMap.get("text"))
                                .metadata((Map<String, Object>) messageMap.get("metadata"))
                                .build();
                        break;
                    case MessageType.TOOL:
                        message = new ToolResponseMessage((List<ToolResponseMessage.ToolResponse>)  messageMap.get("responses")
                                , (Map<String, Object>) messageMap.get("metadata"));
                        break;
                }
                messages.add(message);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing message: " + e.getMessage(), e);
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        if (messages.isEmpty()) return;
        LocalDateTime localDateTime = LocalDateTime.now();
        // save to postgres db
        Message newMessage = messages.getLast();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(newMessage.getText());
        chatMessage.setMessageType(newMessage.getMessageType());
        chatMessage.setMetadata(newMessage.getMetadata());
        chatMessage.setCreatedAt(localDateTime);
        pgChatMessageRepository.save(chatMessage);

        String key = KEY_PREFIX + conversationId;

        List<String> jsonMessages = new ArrayList<>();
        for (Message message : messages) {
            try {
                Map<String, Object> metadata = message.getMetadata();
                if (!metadata.containsKey("createAt")) {
                    metadata.put("createAt", localDateTime);
                }
                String jsonMessage = objectMapper.writeValueAsString(message);
                jsonMessages.add(jsonMessage);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing message: " + e.getMessage(), e);
            }
        }

        // execute with transaction
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                try {
                    operations.multi();
                    redisTemplate.delete(key);
                    redisTemplate.opsForList().rightPushAll(key, jsonMessages);
                    operations.exec();
                    return null;
                } catch (RuntimeException e) {
                    operations.discard();
                    throw e;
                }
            }
        });

    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }


}
