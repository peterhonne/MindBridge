package com.mindbridge.ai.agent.orchestrator.orchestrator;

import com.mindbridge.ai.agent.orchestrator.orchestrator.advisor.MessageAugmentationAdviser;
import com.mindbridge.ai.agent.orchestrator.orchestrator.component.CustomChatMemoryRepository;
import com.mindbridge.ai.agent.orchestrator.orchestrator.component.SearchEngineDocumentRetriever;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
import static org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever.FILTER_EXPRESSION;

@Component
@Slf4j
public class AgentOrchestrator {

    private final ChatClient chatClient;

    private final ChatClient.Builder clientBuilder;

    private final RestClient.Builder restClientBuilder;

    private final VectorStore vectorStore;

    private final ChatMemory chatMemory;

    private MessageAugmentationAdviser.Builder messageAugmentationAdviserBuilder;

    @Value("classpath:/prompts/query_augmenter_prompt.st")
    private Resource queryAugmenterPrompt;
    @Value("classpath:/prompts/query_rewrite_transformer_prompt.st")
    private Resource queryRewriteTransformerPrompt;

    private final Map<String, String> supportRoutes = Map.of(
            "therapeutic", """
                    You are a therapeutic AI companion. Provide evidence-based insights,
                    reference therapy techniques, and offer structured support.
                    """,
            "general", """
                    You are a supportive mental health companion. Provide empathetic listening,
                    gentle guidance, and encouragement for continued care.
                    """,
            "educational", """
                    You are a mental health educator. Provide informative content about
                    mental health concepts, techniques, and resources. Call GoogleSearchTool if needed.
                    """
    );

    @PostConstruct
    public void init() {

        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(clientBuilder)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(PromptTemplate.builder()
                        .resource(queryAugmenterPrompt).build()).build();
        this.messageAugmentationAdviserBuilder = MessageAugmentationAdviser.builder()
                .queryExpander(queryExpander)
                .queryAugmenter(queryAugmenter)
                .chatMemory(chatMemory);
    }

    public AgentOrchestrator(ChatClient chatClient, ChatClient.Builder clientBuilder, VectorStore vectorStore, CustomChatMemoryRepository customChatMemoryRepository, RestClient.Builder restClientBuilder) {
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(customChatMemoryRepository)
                .build();
        this.chatClient = chatClient;
        this.clientBuilder = clientBuilder;
        this.vectorStore = vectorStore;
        this.restClientBuilder = restClientBuilder;
    }


    public String routeUserInput(String userInput, Long userId, String keycloakUserId) {
        log.debug("Routing user input for appropriate support level - user: {}", userId);

        String classification = classifyInput(userInput, keycloakUserId);
        classification = classification.replace("\n", "");
        log.info("Classification: {}", classification);
        return switch (classification.toLowerCase()) {
            case "therapeutic" -> handleTherapeuticSupport(userInput, userId, keycloakUserId);
            case "educational" -> handleEducationalSupport(userInput, keycloakUserId);
            default -> handleGeneralSupport(userInput, keycloakUserId);
        };
    }

    private String handleTherapeuticSupport(String userInput, Long userId, String keycloakUserId) {

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .topK(RandomUtils.secure().randomInt(3, 5))
                .build();

        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(clientBuilder)
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .targetSearchSystem("vector store")
                .build();

        return chatClient.prompt(supportRoutes.get("therapeutic"))
                .user(userInput)
                .advisors(messageAugmentationAdviserBuilder
                        .documentRetriever(documentRetriever)
                        .queryTransformers(queryTransformer)
                        .build())
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId).param(FILTER_EXPRESSION, "user_id == " + userId))
                .call()
                .content();
    }

    private String handleGeneralSupport(String userInput, String keycloakUserId) {
        return chatClient.prompt(supportRoutes.get("general"))
                .user(userInput)
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId))
                .call()
                .content();
    }

    private String handleEducationalSupport(String userInput, String keycloakUserId) {
        var documentRetriever = SearchEngineDocumentRetriever.builder()
                .restClientBuilder(restClientBuilder)
                .maxResults(10)
                .build();
        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(clientBuilder)
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .targetSearchSystem("search engine")
                .build();

        return chatClient.prompt(supportRoutes.get("educational"))
                .user(userInput)
                .advisors(messageAugmentationAdviserBuilder
                        .documentRetriever(documentRetriever)
                        .queryTransformers(queryTransformer)
                        .build())
                .advisors(advisor -> advisor.param(CONVERSATION_ID, keycloakUserId))
                .call()
                .content();
    }


    private String classifyInput(String userInput, String keycloakUserId) {
        List<Message> messageList = chatMemory.get(keycloakUserId);
        String memory = messageList.stream()
                .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
                .map(m -> m.getMessageType() + ":" + m.getText())
                .collect(Collectors.joining(System.lineSeparator()));
        return clientBuilder.clone().build()
                .prompt(memory + """
                        
                        Above is previous chat message history for your reference.
                        --------------
                        Classify the main intent of the user's messages based on the full conversation. Choose the most relevant category:
                        - THERAPEUTIC: Deep emotional issues, therapy-related discussions, complex problems
                        - EDUCATIONAL: Questions about mental health concepts, techniques, general information
                        - GENERAL: General support, daily check-ins, mild concerns
                        
                        Respond with only the category name.
                        """)
                .user(userInput)
                .call()
                .content();
    }

}
