package com.mindbridge.ai.agent.orchestrator.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.client.RestClient;

@Slf4j
public class RagTools {

    private final ChatClient.Builder chatClientBuilder;
    private final RestClient.Builder restClientBuilder;
    private final VectorStore vectorStore;



    public RagTools(ChatClient.Builder chatClientBuilder, RestClient.Builder restClientBuilder, VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder;
        this.restClientBuilder = restClientBuilder;
        this.vectorStore = vectorStore;
    }



    @Tool(description = "recall memory, retrieve insights from the user's past moods, journal entries, and session logs to help explain their current emotional state or stress If the message clearly implies context retrieval (emotionally reflective, memory-seeking).")
    public String recallMemory(String query) {
        log.info("retrieving user's previous session log");

        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .topK(3)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(PromptTemplate.builder()
                        .template("""
                                Context information is below.

                                ---------------------
                                {context}
                                ---------------------

                                Given the context information and no prior knowledge, answer the query as directly as possible.

                                Follow these rules strictly:

                                1. If the answer is not in the context, return nothing"
                                2. Do NOT include any phrases such as "Based on the context," "The provided information," "According to the data," or similar introductory explanations.
                                3. Your answer should be concise and direct, without referencing the source of information.

                                Query: {query}

                                Answer:
                        """).build()).build();

        return chatClientBuilder
                .clone()
                .build()
                .prompt()
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .queryExpander(queryExpander)
                        .queryTransformers(queryTransformer)
                        .queryAugmenter(queryAugmenter)
                        .documentRetriever(documentRetriever)
                        .build())
                .user(query)
                .call()
                .content();

    }
    @Tool(description = "Retrieve information by searching the google scholar")
    public String googleScholarSearchRetriever(String query) {
        log.info("searching google scholar");
        return "";
    }


}
