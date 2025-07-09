package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.models.entity.JournalEntry;
import com.mindbridge.ai.agent.orchestrator.models.entity.MoodEntry;
import com.mindbridge.ai.agent.orchestrator.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {


    private final VectorStore vectorStore;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;

    @Async
    @Transactional
    public void generateEmbeddingForJournalEntry(JournalEntry journalEntry) {
        log.debug("Generating embedding for journal entry: {}", journalEntry.getId());

        try {
            String content = prepareJournalContent(journalEntry);
            Map<String, Object> metadata = createJournalMetadata(journalEntry);

            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));

            log.debug("Successfully generated embedding for journal entry: {}", journalEntry.getId());
        } catch (Exception e) {
            log.error("Error generating embedding for journal entry {}: {}",
                    journalEntry.getId(), e.getMessage());
        }
    }

    @Async
    @Transactional
    public void generateEmbeddingForMoodEntry(MoodEntry moodEntry) {
        log.debug("Generating embedding for mood entry: {}", moodEntry.getId());

        try {
            if (moodEntry.getNotes() != null && !moodEntry.getNotes().trim().isEmpty()) {
                String content = prepareMoodEntryContent(moodEntry);
                Map<String, Object> metadata = createMoodEntryMetadata(moodEntry);

                Document document = new Document(content, metadata);
                vectorStore.add(List.of(document));

                log.debug("Successfully generated embedding for mood entry: {}", moodEntry.getId());
            }
        } catch (Exception e) {
            log.error("Error generating embedding for mood entry {}: {}",
                    moodEntry.getId(), e.getMessage());
        }
    }


    private String prepareJournalContent(JournalEntry journalEntry) {
        StringBuilder content = new StringBuilder();

        if (journalEntry.getTitle() != null) {
            content.append("Title: ").append(journalEntry.getTitle()).append("\n");
        }

        content.append("Content: ").append(journalEntry.getContent());

        if (journalEntry.getTags() != null && !journalEntry.getTags().isEmpty()) {
            content.append("\nTags: ").append(String.join(", ", journalEntry.getTags()));
        }

        if (journalEntry.getMoodBefore() != null) {
            content.append("\nMood before: ").append(journalEntry.getMoodBefore());
        }

        if (journalEntry.getMoodAfter() != null) {
            content.append("\nMood after: ").append(journalEntry.getMoodAfter());
        }

        return content.toString();
    }


    private String prepareMoodEntryContent(MoodEntry moodEntry) {
        StringBuilder content = new StringBuilder();

        content.append("Mood Score: ").append(moodEntry.getMoodScore()).append("/10");

        if (moodEntry.getMoodTags() != null && !moodEntry.getMoodTags().isEmpty()) {
            content.append("\nMood Tags: ").append(String.join(", ", moodEntry.getMoodTags()));
        }

        if (moodEntry.getNotes() != null) {
            content.append("\nNotes: ").append(moodEntry.getNotes());
        }

        return content.toString();
    }


    private Map<String, Object> createJournalMetadata(JournalEntry journalEntry) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_type", "journal_entry");
        metadata.put("document_id", journalEntry.getId());
        metadata.put("user_id", journalEntry.getUser().getId());
        metadata.put("created_at", journalEntry.getCreatedAt().toString());
        metadata.put("date", journalEntry.getCreatedAt().toLocalDate().toString());

        if (journalEntry.getTags() != null) {
            metadata.put("tags", journalEntry.getTags());
        }

        return metadata;
    }

    private Map<String, Object> createMoodEntryMetadata(MoodEntry moodEntry) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_type", "mood_entry");
        metadata.put("document_id", moodEntry.getId());
        metadata.put("user_id", moodEntry.getUser().getId());
        metadata.put("created_at", moodEntry.getCreatedAt().toString());
        metadata.put("date", moodEntry.getCreatedAt().toLocalDate().toString());
        metadata.put("mood_score", moodEntry.getMoodScore());

        if (moodEntry.getMoodTags() != null) {
            metadata.put("mood_tags", moodEntry.getMoodTags());
        }

        return metadata;
    }

    public void deleteEmbeddingsForDocument(String documentType, Long documentId, Long userId) {
        log.debug("Deleting embeddings for {} with id: {}", documentType, documentId);

        try {
            // Delete from our manual tracking table
            documentEmbeddingRepository.deleteByUserIdAndDocumentTypeAndDocumentId(userId, documentType, documentId);

            // Note: For Spring AI VectorStore, you would need to implement custom deletion
            // based on metadata filtering. This is a limitation of the current VectorStore API
            log.debug("Deleted embeddings for {} {}", documentType, documentId);
        } catch (Exception e) {
            log.error("Error deleting embeddings for {} {}: {}",
                    documentType, documentId, e.getMessage());
        }
    }

}
