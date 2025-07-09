package com.mindbridge.ai.agent.orchestrator.models.entity;

import com.mindbridge.ai.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "document_type")
    private String documentType; // therapy_session, journal, mood_note

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "content_chunk", columnDefinition = "TEXT")
    private String contentChunk;

    // Note: pgvector extension handles this as vector type
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding; // Store as text, pgvector handles conversion

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

}
