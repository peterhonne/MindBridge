package com.mindbridge.ai.agent.orchestrator.orchestrator.tools;

import com.mindbridge.ai.agent.orchestrator.orchestrator.advisor.MessageAugmentationAdviser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
import static org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever.FILTER_EXPRESSION;

public class SupportAgents {

    private static final String GENERAL_PROMPT = "You are a supportive mental health companion. Provide empathetic listening, " +
            "gentle guidance, and encouragement for continued care.";

    private static final String EDUCATIONAL_PROMPT = "You are a therapeutic AI companion. Provide evidence-based insights, " +
            "reference therapy techniques, and offer structured support.";

    private static final String THERAPEUTIC_PROMPT = "You are a mental health educator. Provide informative content about " +
            "mental health concepts, techniques, and resources.";

    @Value("classpath:/prompts/query_augmenter_prompt.st")
    private Resource queryAugmenterPrompt;
    @Value("classpath:/prompts/query_rewrite_transformer_prompt.st")
    private Resource queryRewriteTransformerPrompt;

    private final ChatClient.Builder chatClientBuilder;

    private final VectorStore vectorStore;

    private final ChatMemory chatMemory;

    public SupportAgents(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    @Tool(description = "")
    public String handleGeneralSupport(String userInput, String keycloakUserId) {
        return chatClientBuilder.clone().build()
                .prompt(GENERAL_PROMPT)
                .user(userInput)
                .advisors()
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId))
                .call()
                .content();
    }

    @Tool(description = "")
    public String handleEducationalSupport(String userInput, String keycloakUserId) {
        return chatClientBuilder.clone().build()
                .prompt(EDUCATIONAL_PROMPT)
                .user(userInput)
                .advisors()
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId))
                .call()
                .content();
    }

    @Tool(description = "")
    public String handleTherapeuticSupport(String userInput, Long userId, String keycloakUserId) {

        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .build();

        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .emptyContextPromptTemplate(PromptTemplate.builder()
                        .resource(queryAugmenterPrompt).build()).build();

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .topK(3)
                .build();

        return chatClientBuilder.clone().build()
                .prompt(THERAPEUTIC_PROMPT)
                .user(userInput)
                .advisors(
                        MessageAugmentationAdviser.builder()
                                .queryTransformers(queryTransformer)
                                .queryExpander(queryExpander)
                                .queryAugmenter(queryAugmenter)
                                .documentRetriever(documentRetriever)
                                .chatMemory(chatMemory)
                                .build())
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId).param(FILTER_EXPRESSION, "user_id == " + userId))
                .call()
                .content();
    }

}
