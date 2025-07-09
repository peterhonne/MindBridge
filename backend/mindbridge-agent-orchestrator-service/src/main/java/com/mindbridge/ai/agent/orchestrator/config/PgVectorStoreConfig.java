package com.mindbridge.ai.agent.orchestrator.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

//@Configuration
public class PgVectorStoreConfig {


//    @Bean
//    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
//        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
////                .schemaName(schemaName)
//                .vectorTableName("mindbridge_vector")
////                .dimensions(embeddingDimensions)
//                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
//                .removeExistingVectorStoreTable(false)
//                .indexType(PgVectorStore.PgIndexType.HNSW)
//                .initializeSchema(true)
//                .build();
//    }

}
