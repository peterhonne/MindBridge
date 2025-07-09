package com.mindbridge.ai.agent.orchestrator.repository;
import com.mindbridge.ai.agent.orchestrator.models.entity.DocumentEmbedding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    /**
     * Find all embeddings for a specific user and document type
     */
    List<DocumentEmbedding> findByUserIdAndDocumentType(Long userId, String documentType);

    /**
     * Find embedding for a specific document
     */
    List<DocumentEmbedding> findByUserIdAndDocumentTypeAndDocumentId(Long userId, String documentType, Long documentId);

    /**
     * Delete embeddings for a specific document
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentEmbedding de WHERE de.user.id = :userId AND de.documentType = :documentType AND de.documentId = :documentId")
    void deleteByUserIdAndDocumentTypeAndDocumentId(@Param("userId") Long userId,
                                                    @Param("documentType") String documentType,
                                                    @Param("documentId") Long documentId);

    /**
     * Find similar documents using vector similarity search
     * Note: This uses pgvector's cosine distance operator (<->)
     */
    @Query(value = """
        SELECT de.* FROM document_embeddings de 
        WHERE de.user_id = :userId 
        ORDER BY de.embedding <-> CAST(:queryEmbedding AS vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentEmbedding> findSimilarDocuments(@Param("userId") Long userId,
                                                 @Param("queryEmbedding") String queryEmbedding,
                                                 @Param("limit") int limit);

    /**
     * Find similar documents with a similarity threshold
     */
    @Query(value = """
        SELECT de.* FROM document_embeddings de 
        WHERE de.user_id = :userId 
        AND (de.embedding <-> CAST(:queryEmbedding AS vector)) < :threshold
        ORDER BY de.embedding <-> CAST(:queryEmbedding AS vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentEmbedding> findSimilarDocumentsWithThreshold(@Param("userId") Long userId,
                                                              @Param("queryEmbedding") String queryEmbedding,
                                                              @Param("threshold") double threshold,
                                                              @Param("limit") int limit);

    /**
     * Find similar documents of a specific type (e.g., only therapy sessions)
     */
    @Query(value = """
        SELECT de.* FROM document_embeddings de 
        WHERE de.user_id = :userId 
        AND de.document_type = :documentType
        ORDER BY de.embedding <-> CAST(:queryEmbedding AS vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentEmbedding> findSimilarDocumentsByType(@Param("userId") Long userId,
                                                       @Param("documentType") String documentType,
                                                       @Param("queryEmbedding") String queryEmbedding,
                                                       @Param("limit") int limit);

    /**
     * Get all embeddings for a user with pagination
     */
    Page<DocumentEmbedding> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Count embeddings by user and document type
     */
    long countByUserIdAndDocumentType(Long userId, String documentType);

    /**
     * Count total embeddings for a user
     */
    long countByUserId(Long userId);

    /**
     * Find recent embeddings for a user
     */
    @Query("SELECT de FROM DocumentEmbedding de WHERE de.user.id = :userId ORDER BY de.createdAt DESC")
    List<DocumentEmbedding> findRecentEmbeddings(@Param("userId") Long userId, Pageable pageable);

    /**
     * Delete all embeddings for a specific user (for cleanup/GDPR compliance)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentEmbedding de WHERE de.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Delete all embeddings of a specific type for a user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentEmbedding de WHERE de.user.id = :userId AND de.documentType = :documentType")
    void deleteByUserIdAndDocumentType(@Param("userId") Long userId, @Param("documentType") String documentType);

    /**
     * Find embeddings with specific metadata
     */
    @Query("SELECT de FROM DocumentEmbedding de WHERE de.user.id = :userId AND JSON_EXTRACT(de.metadata, '$.key') = :value")
    List<DocumentEmbedding> findByUserIdAndMetadata(@Param("userId") Long userId, @Param("value") String value);
}