package com.mindbridge.ai.agent.orchestrator.orchestrator.tools;

import com.mindbridge.ai.agent.orchestrator.orchestrator.advisor.MessageAugmentationAdviser;
import com.mindbridge.ai.agent.orchestrator.orchestrator.component.SearchEngineDocumentRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class SearchEngineTool {

    private final ChatClient.Builder chatClientBuilder;
    private final RestClient.Builder restClientBuilder;

    @Tool(name = "TavilySearchTool", description = "Searches trusted mental health websites using a Google Custom Search Engine to find reliable educational content.")
    public String tavilySearch(String userInput) {
        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(PromptTemplate.builder().resource(queryRewriteTransformerPrompt).build())
                .build();

        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(PromptTemplate.builder()
                        .resource(queryAugmenterPrompt).build()).build();
        
        return chatClientBuilder.clone().build().prompt()
                .advisors(MessageAugmentationAdviser.builder()
                        .documentRetriever()
                        .build())
                .user(userInput)
                .call()
                .content();

    }


}
