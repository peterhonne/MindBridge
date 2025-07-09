package com.mindbridge.ai.agent.orchestrator.config;

import com.mindbridge.ai.agent.orchestrator.advisor.RedisChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.tools.RagTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    // customised chatClient
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, RedisChatMemoryRepository redisChatMemoryRepository, RestClient.Builder restClientBuilder, VectorStore vectorStore) {

        var chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .build();

        return chatClientBuilder.defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory)
                                .build(),
                VectorStoreChatMemoryAdvisor.builder(vectorStore).defaultTopK(3).build(),
                // TODO RelevancyEvaluator
                // TODO SensitiveDataInputGuardrailAdvisor
//                QuestionAnswerAdvisor.builder(vectorStore).searchRequest().build(),
                        new SimpleLoggerAdvisor())
                // TODO Function calls, tool call
                .defaultSystem(systemPromptResource)
                .defaultTools(new RagTools(chatClientBuilder.clone(), restClientBuilder, vectorStore))
                .build();
    }

}
