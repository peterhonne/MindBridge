package com.mindbridge.ai.agent.orchestrator.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    // customised chatClient
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {

        ChatClient build = chatClientBuilder.defaultAdvisors(

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

}
