package com.mindbridge.ai.agent.orchestrator.orchestrator.tools;

import com.mindbridge.ai.agent.orchestrator.orchestrator.advisor.MessageAugmentationAdviser;
import com.mindbridge.ai.agent.orchestrator.orchestrator.component.SearchEngineDocumentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

@Slf4j
public class SearchEngineTool {

    private final ChatClient.Builder chatClientBuilder;
    private final RestClient.Builder restClientBuilder;
    private final MessageAugmentationAdviser.Builder messageAugmentationAdviserBuilder;
    private final VectorStore vectorStore;
    private final Resource queryRewriteTransformerPrompt;

    public SearchEngineTool(ChatClient.Builder chatClientBuilder, RestClient.Builder restClientBuilder, 
                            ChatMemory chatMemory, VectorStore vectorStore, 
                            Resource queryAugmenterPrompt, Resource queryRewriteTransformerPrompt) {

        this.chatClientBuilder = chatClientBuilder;
        this.restClientBuilder = restClientBuilder;
        this.vectorStore = vectorStore;
        this.queryRewriteTransformerPrompt = queryRewriteTransformerPrompt;

        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(PromptTemplate.builder()
                        .resource(queryAugmenterPrompt).build()).build();

        var queryCompression = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .build();

        this.messageAugmentationAdviserBuilder = MessageAugmentationAdviser.builder()
                .queryExpander(queryExpander)
                .queryTransformers(queryCompression)
                .queryAugmenter(queryAugmenter)
                .chatMemory(chatMemory);
    }


    @Tool(name = "TavilySearchTool", description = "Searches trusted mental health websites using a Google Custom Search Engine to find reliable educational content.")
    public String tavilySearch(String userInput) {
        var documentRetriever = SearchEngineDocumentRetriever.builder()
                .restClientBuilder(restClientBuilder)
                .maxResults(10)
                .build();
        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .targetSearchSystem("search engine")
                .build();
        String content = chatClientBuilder.clone().build().prompt()
                .advisors(messageAugmentationAdviserBuilder
                        .documentRetriever(documentRetriever)
                        .addQueryTransformer(queryTransformer)
                        .build())
                .user(userInput)
                .call()
                .content();

        System.out.println(content);
        return content;

    }

    @Tool(name = "VectorDatabaseSearchTool", description = "Searches vector database for user's previous memories, chat history's, mood histories, journal histories.")
    public String vectorDatabaseSearch(String userInput, String conversationId) {
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .topK(RandomUtils.secure().randomInt(3, 5))
                .build();

        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .targetSearchSystem("vector store")
                .build();

        ChatResponse chatResponse = chatClientBuilder.clone().build().prompt()
                .advisors(messageAugmentationAdviserBuilder
                        .documentRetriever(documentRetriever)
                        .addQueryTransformer(queryTransformer)
                        .build())
                .user(userInput)
                .call()
                .chatResponse();
        Generation result = chatResponse.getResult();
        String content = "";
        System.out.println(content);
        return content;

    }


}
