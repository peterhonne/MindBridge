package com.mindbridge.ai.agent.orchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.ai.agent.orchestrator.component.CustomChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.repository.PgChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    // customised chatClient
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {

        ChatClient build = chatClientBuilder.defaultAdvisors(
                PromptChatMemoryAdvisor.builder(chatMemory).build(),
//                OrchestratorAdviser.builder().queryTransformers(queryTransformer).queryExpander(queryExpander).build(),
//                VectorStoreChatMemoryAdvisor.builder(vectorStore).defaultTopK(3).build(),
                        // TODO RelevancyEvaluator
                        // TODO SensitiveDataInputGuardrailAdvisor
//                QuestionAnswerAdvisor.builder(vectorStore).searchRequest().build(),
                        new SimpleLoggerAdvisor())
                // TODO Function calls, tool call
                .defaultSystem(systemPromptResource)
//                .defaultTools(new RagTools(chatClientBuilder.clone(), restClientBuilder, vectorStore))
                .build();


        return build;
    }


    @Bean
    public CustomChatMemoryRepository customChatMemoryRepository(RedisTemplate<String, String> redisAgentMessageTemplate, ObjectMapper objectMapper, PgChatMessageRepository pgChatMessageRepository) {
        return new CustomChatMemoryRepository(redisAgentMessageTemplate, objectMapper, pgChatMessageRepository);
    }
}
