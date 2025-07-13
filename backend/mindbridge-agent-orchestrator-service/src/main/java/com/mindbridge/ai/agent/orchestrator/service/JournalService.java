package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.models.dto.CreateJournalEntryRequest;
import com.mindbridge.ai.agent.orchestrator.models.dto.JournalEntryDto;
import com.mindbridge.ai.agent.orchestrator.models.dto.UpdateJournalEntryRequest;
import com.mindbridge.ai.agent.orchestrator.models.entity.JournalEntry;
import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.repository.JournalEntryRepository;
import com.mindbridge.ai.agent.orchestrator.repository.UserRepository;
import com.mindbridge.ai.common.exception.UserProfileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class JournalService {
    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    public JournalEntryDto createJournalEntry(CreateJournalEntryRequest request, String userId) {
        log.debug("Creating journal entry for user: {}", userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setCode(RandomStringUtils.secure().nextAlphanumeric(8));
        journalEntry.setUser(user);
        journalEntry.setTitle(request.getTitle());
        journalEntry.setContent(request.getContent());
        journalEntry.setMoodBefore(request.getMoodBefore());
        journalEntry.setMoodAfter(request.getMoodAfter());
        journalEntry.setTags(request.getTags());

        journalEntry = journalEntryRepository.save(journalEntry);

        // Generate embeddings for the journal entry
        embeddingService.generateEmbeddingForJournalEntry(journalEntry);

        log.debug("Created journal entry with ID: {}", journalEntry.getId());
        return mapToDto(journalEntry);
    }

    @Transactional(readOnly = true)
    public Page<JournalEntryDto> getJournalEntries(String userId, Pageable pageable) {
        log.debug("Getting journal entries for user: {}", userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        Page<JournalEntry> journalEntries = journalEntryRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return journalEntries.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public JournalEntryDto getJournalEntry(String code, String userId) {
        log.debug("Getting journal entry: {} for user: {}", code, userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        JournalEntry journalEntry = journalEntryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        if (!journalEntry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapToDto(journalEntry);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        // Default to last 7 days if no dates provided
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return journalEntryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
    }


    public JournalEntryDto updateJournalEntry(String code, UpdateJournalEntryRequest request, String userId) {
        log.debug("Updating journal entry: {} for user: {}", code, userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        JournalEntry journalEntry = journalEntryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        if (!journalEntry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            journalEntry.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            journalEntry.setContent(request.getContent());
        }
        if (request.getMoodBefore() != null) {
            journalEntry.setMoodBefore(request.getMoodBefore());
        }
        if (request.getMoodAfter() != null) {
            journalEntry.setMoodAfter(request.getMoodAfter());
        }
        if (request.getTags() != null) {
            journalEntry.setTags(request.getTags());
        }

        journalEntry = journalEntryRepository.save(journalEntry);

        // Regenerate embeddings since content changed
        // TODO update the embedding
        embeddingService.generateEmbeddingForJournalEntry(journalEntry);

        log.debug("Updated journal entry: {}", code);
        return mapToDto(journalEntry);
    }

    public void deleteJournalEntry(String code, String userId) {
        log.debug("Deleting journal entry: {} for user: {}", code, userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        JournalEntry journalEntry = journalEntryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        if (!journalEntry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // TODO Delete associated embeddings


        journalEntryRepository.delete(journalEntry);
        log.debug("Deleted journal entry: {}", code);
    }

    @Transactional(readOnly = true)
    public List<JournalEntryDto> searchJournalEntries(String searchTerm, String userId) {
        log.debug("Searching journal entries for user: {} with term: {}", userId, searchTerm);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        List<JournalEntry> journalEntries = journalEntryRepository.searchByContent(user.getId(), searchTerm);

        return journalEntries.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private JournalEntryDto mapToDto(JournalEntry journalEntry) {
        return JournalEntryDto.builder()
                .code(journalEntry.getCode())
                .title(journalEntry.getTitle())
                .content(journalEntry.getContent())
                .moodBefore(journalEntry.getMoodBefore())
                .moodAfter(journalEntry.getMoodAfter())
                .tags(journalEntry.getTags())
                .createdAt(journalEntry.getCreatedAt())
                .build();
    }

}
