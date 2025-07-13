package com.mindbridge.ai.agent.orchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.ai.agent.orchestrator.orchestrator.component.CustomChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.repository.PgChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        return chatClientBuilder.clone().defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .defaultSystem(systemPromptResource)
                // TODO Function calls, tool call
                .build();
    }


    @Bean
    public CustomChatMemoryRepository customChatMemoryRepository(RedisTemplate<String, String> redisAgentMessageTemplate, ObjectMapper objectMapper, PgChatMessageRepository pgChatMessageRepository) {
        return new CustomChatMemoryRepository(redisAgentMessageTemplate, objectMapper, pgChatMessageRepository);
    }
}
