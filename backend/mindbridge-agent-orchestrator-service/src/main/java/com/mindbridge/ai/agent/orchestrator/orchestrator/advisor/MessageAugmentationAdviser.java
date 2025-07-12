package com.mindbridge.ai.agent.orchestrator.orchestrator.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

public class MessageAugmentationAdviser implements BaseAdvisor {

    public static final String DOCUMENT_CONTEXT = "rag_document_context";

    public static final String CONTEXT_TEMPLATE = """
            This is the user current query:
            %s
            
            And previous conversation history for your reference:
            %s
            """;

    private final List<QueryTransformer> queryTransformers;

    private final QueryExpander queryExpander;

    private final DocumentRetriever documentRetriever;

    private final DocumentJoiner documentJoiner;

    private final List<DocumentPostProcessor> documentPostProcessors;

    private final QueryAugmenter queryAugmenter;

    private final TaskExecutor taskExecutor;

    private final Scheduler scheduler;

    private final int order;

    private final ChatMemory chatMemory;

    private MessageAugmentationAdviser(@Nullable List<QueryTransformer> queryTransformers,
                                       @Nullable QueryExpander queryExpander, DocumentRetriever documentRetriever,
                                       @Nullable DocumentJoiner documentJoiner, @Nullable List<DocumentPostProcessor> documentPostProcessors,
                                       @Nullable QueryAugmenter queryAugmenter, @Nullable TaskExecutor taskExecutor, @Nullable Scheduler scheduler,
                                       @Nullable Integer order, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        Assert.notNull(documentRetriever, "documentRetriever cannot be null");
        Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
        this.queryTransformers = queryTransformers != null ? queryTransformers : List.of();
        this.queryExpander = queryExpander;
        this.documentRetriever = documentRetriever;
        this.documentJoiner = documentJoiner != null ? documentJoiner : new ConcatenationDocumentJoiner();
        this.documentPostProcessors = documentPostProcessors != null ? documentPostProcessors : List.of();
        this.queryAugmenter = queryAugmenter != null ? queryAugmenter : ContextualQueryAugmenter.builder().build();
        this.taskExecutor = taskExecutor != null ? taskExecutor : buildDefaultTaskExecutor();
        this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
        this.order = order != null ? order : 0;
    }

    public static MessageAugmentationAdviser.Builder builder() {
        return new MessageAugmentationAdviser.Builder();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());

        Query originalQuery = Query.builder()
                .text(chatClientRequest.prompt().getUserMessage().getText())
                .history(chatClientRequest.prompt().getInstructions())
                .context(context)
                .build();

        List<Message> messageList = chatMemory.get((String) context.get(CONVERSATION_ID));
        String memory = messageList.stream()
                .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
                .map(m -> m.getMessageType() + ":" + m.getText())
                .collect(Collectors.joining(System.lineSeparator()));
        // 1. Transform original user query based on a chain of query transformers.
        Query transformedQuery = originalQuery;
        for (var queryTransformer : this.queryTransformers) {
            transformedQuery = queryTransformer.apply(transformedQuery.mutate()
                    .text(String.format(CONTEXT_TEMPLATE, chatClientRequest.prompt().getUserMessage().getText(), memory))
                    .build());
        }

        // 2. Expand query into one or multiple queries.
        List<Query> expandedQueries = this.queryExpander != null ? this.queryExpander.expand(transformedQuery)
                : List.of(transformedQuery);

        // 3. Get similar documents for each query.
        Map<Query, List<List<Document>>> documentsForQuery = expandedQueries.stream()
                .map(query -> CompletableFuture.supplyAsync(() -> getDocumentsForQuery(query), this.taskExecutor))
                .toList()
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));

        // 4. Combine documents retrieved based on multiple queries and from multiple data
        // sources.
        List<Document> documents = this.documentJoiner.join(documentsForQuery);

        // 5. Post-process the documents.
        for (var documentPostProcessor : this.documentPostProcessors) {
            documents = documentPostProcessor.process(transformedQuery, documents);
        }
        context.put(DOCUMENT_CONTEXT, documents);

        // 6. Augment user query with the document contextual data.
        Query augmentedQuery = this.queryAugmenter.augment(transformedQuery, documents);

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
                .context(context)
                .build();
    }


    private Map.Entry<Query, List<Document>> getDocumentsForQuery(Query query) {
        List<Document> documents = this.documentRetriever.retrieve(query);
        return Map.entry(query, documents);
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        }
        else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(DOCUMENT_CONTEXT, chatClientResponse.context().get(DOCUMENT_CONTEXT));
        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
    }


    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    private static TaskExecutor buildDefaultTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("ai-advisor-");
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(16);
        taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        taskExecutor.initialize();
        return taskExecutor;
    }


    public static final class Builder {

        private List<QueryTransformer> queryTransformers;

        private QueryExpander queryExpander;

        private DocumentRetriever documentRetriever;

        private DocumentJoiner documentJoiner;

        private List<DocumentPostProcessor> documentPostProcessors;

        private QueryAugmenter queryAugmenter;

        private TaskExecutor taskExecutor;

        private Scheduler scheduler;

        private Integer order;

        private ChatMemory chatMemory;

        private Builder() {
        }

        public MessageAugmentationAdviser.Builder queryTransformers(List<QueryTransformer> queryTransformers) {
            Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
            this.queryTransformers = queryTransformers;
            return this;
        }

        public MessageAugmentationAdviser.Builder queryTransformers(QueryTransformer... queryTransformers) {
            Assert.notNull(queryTransformers, "queryTransformers cannot be null");
            Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
            this.queryTransformers = Arrays.asList(queryTransformers);
            return this;
        }

        public MessageAugmentationAdviser.Builder queryExpander(QueryExpander queryExpander) {
            this.queryExpander = queryExpander;
            return this;
        }

        public MessageAugmentationAdviser.Builder documentRetriever(DocumentRetriever documentRetriever) {
            this.documentRetriever = documentRetriever;
            return this;
        }

        public MessageAugmentationAdviser.Builder documentJoiner(DocumentJoiner documentJoiner) {
            this.documentJoiner = documentJoiner;
            return this;
        }

        public MessageAugmentationAdviser.Builder documentPostProcessors(List<DocumentPostProcessor> documentPostProcessors) {
            Assert.noNullElements(documentPostProcessors, "documentPostProcessors cannot contain null elements");
            this.documentPostProcessors = documentPostProcessors;
            return this;
        }

        public MessageAugmentationAdviser.Builder documentPostProcessors(DocumentPostProcessor... documentPostProcessors) {
            Assert.notNull(documentPostProcessors, "documentPostProcessors cannot be null");
            Assert.noNullElements(documentPostProcessors, "documentPostProcessors cannot contain null elements");
            this.documentPostProcessors = Arrays.asList(documentPostProcessors);
            return this;
        }

        public MessageAugmentationAdviser.Builder queryAugmenter(QueryAugmenter queryAugmenter) {
            this.queryAugmenter = queryAugmenter;
            return this;
        }

        public MessageAugmentationAdviser.Builder taskExecutor(TaskExecutor taskExecutor) {
            this.taskExecutor = taskExecutor;
            return this;
        }

        public MessageAugmentationAdviser.Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public MessageAugmentationAdviser.Builder order(Integer order) {
            this.order = order;
            return this;
        }

        public MessageAugmentationAdviser.Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public MessageAugmentationAdviser build() {
            return new MessageAugmentationAdviser(this.queryTransformers, this.queryExpander, this.documentRetriever,
                    this.documentJoiner, this.documentPostProcessors, this.queryAugmenter, this.taskExecutor,
                    this.scheduler, this.order, this.chatMemory);
        }

    }
}
