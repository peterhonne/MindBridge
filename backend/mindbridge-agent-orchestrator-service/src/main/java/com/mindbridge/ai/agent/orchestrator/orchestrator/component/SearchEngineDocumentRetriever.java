package com.mindbridge.ai.agent.orchestrator.orchestrator.component;

import com.mindbridge.ai.agent.orchestrator.config.GoogleSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
public class GoogleSearchDocumentRetriever implements DocumentRetriever {
    private static final String BASE_URL = "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=";

    private static final int DEFAULT_MAX_RESULTS = 10;

    private final RestClient.Builder restClientBuilder;

    private final int maxResults;

    private final GoogleSearchProperties googleSearchProperties;


    public GoogleSearchDocumentRetriever(RestClient.Builder restClientBuilder, int maxResults, GoogleSearchProperties googleSearchProperties) {
        this.googleSearchProperties = googleSearchProperties;
        Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
        this.restClientBuilder = restClientBuilder;
        this.maxResults = maxResults;
    }

    @Override
    public List<Document> retrieve(Query query) {
        Assert.notNull(query, "query cannot be null");
        String baseUrl = "https://www.googleapis.com";

        String uriPath = UriComponentsBuilder
                .fromPath("/customsearch/v1")
                .queryParam("key", googleSearchProperties.getApiKey())
                .queryParam("cx", googleSearchProperties.getSearchId())
                .queryParam("num", maxResults)
                .queryParam("q", query.text())
                .build()
                .toUriString();

        var response = restClientBuilder.clone().baseUrl(baseUrl).build()
                .get()
                .uri(uriPath)
                .retrieve()
                .body(Map.class);
        System.out.println(response);

        return List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder restClientBuilder;

        private int maxResults = DEFAULT_MAX_RESULTS;

        private GoogleSearchProperties googleSearchProperties;

        private Builder() {}

        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public Builder googleSearchProperties(GoogleSearchProperties googleSearchProperties) {
            this.googleSearchProperties = googleSearchProperties;
            return this;
        }

        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.maxResults = maxResults;
            return this;
        }

        public GoogleSearchDocumentRetriever build() {
            return new GoogleSearchDocumentRetriever(restClientBuilder, maxResults, googleSearchProperties);
        }
    }
}
