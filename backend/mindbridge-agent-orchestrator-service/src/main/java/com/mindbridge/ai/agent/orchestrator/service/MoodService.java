package com.mindbridge.ai.agent.orchestrator.service;


import com.mindbridge.ai.agent.orchestrator.models.dto.CreateMoodEntryRequest;
import com.mindbridge.ai.agent.orchestrator.models.dto.MoodEntryDto;
import com.mindbridge.ai.agent.orchestrator.models.dto.MoodStatsDto;
import com.mindbridge.ai.agent.orchestrator.models.entity.MoodEntry;
import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.repository.MoodEntryRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MoodService {

    private final MoodEntryRepository moodEntryRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    public MoodEntryDto createMoodEntry(CreateMoodEntryRequest request, String userId) {
        log.debug("Creating mood entry for user: {}", userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        MoodEntry moodEntry = new MoodEntry();
        moodEntry.setCode(RandomStringUtils.secure().nextAlphanumeric(8));
        moodEntry.setUser(user);
        moodEntry.setMoodScore(request.getMoodScore());
        moodEntry.setMoodTags(request.getMoodTags());
        moodEntry.setNotes(request.getNotes());

        moodEntry = moodEntryRepository.save(moodEntry);

        // Generate embeddings for the mood entry if it has notes
        if (moodEntry.getNotes() != null && !moodEntry.getNotes().trim().isEmpty()) {
            embeddingService.generateEmbeddingForMoodEntry(moodEntry);
        }

        log.debug("Created mood entry with ID: {}", moodEntry.getId());
        return mapToDto(moodEntry);
    }

    @Transactional(readOnly = true)
    public Page<MoodEntryDto> getMoodHistory(String userId, Pageable pageable) {
        log.debug("Getting mood history for user: {}", userId);

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        Page<MoodEntry> moodEntries = moodEntryRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return moodEntries.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public MoodStatsDto getMoodStats(String keycloakUserId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting mood stats for user: {} from {} to {}", keycloakUserId, startDate, endDate);
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(keycloakUserId));
        List<MoodEntry> moodEntries = getRecentMoodsByPeriod(user.getId(), startDate, endDate);

        if (moodEntries.isEmpty()) {
            return MoodStatsDto.builder()
                    .averageMoodScore(0.0)
                    .highestMoodScore(0)
                    .lowestMoodScore(0)
                    .commonMoodTags(List.of())
                    .moodTrendData(Map.of())
                    .build();
        }

        // Calculate statistics
        Double averageMoodScore = moodEntries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .average()
                .orElse(0.0);

        Integer highestMoodScore = moodEntries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .max()
                .orElse(0);

        Integer lowestMoodScore = moodEntries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .min()
                .orElse(0);

        // Get common mood tags
        List<String> commonMoodTags = moodEntryRepository.findMostCommonMoodTags(user.getId());

        // Create mood trend data (grouped by day)
        Map<String, Integer> moodTrendData = moodEntries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        Collectors.collectingAndThen(
                                Collectors.averagingInt(MoodEntry::getMoodScore),
                                avg -> avg.intValue()
                        )
                ));

        return MoodStatsDto.builder()
                .averageMoodScore(Math.round(averageMoodScore * 100.0) / 100.0)
                .highestMoodScore(highestMoodScore)
                .lowestMoodScore(lowestMoodScore)
                .commonMoodTags(commonMoodTags)
                .moodTrendData(moodTrendData)
                .build();
    }

    public void deleteMoodEntry(String code, String userId) {
        log.debug("Deleting mood entry: {} for user: {}", code, userId);

        MoodEntry moodEntry = moodEntryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mood entry not found"));

        User user = userRepository.findByKeycloakUserId(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        if (!moodEntry.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Delete associated embeddings
        embeddingService.deleteEmbeddingsForDocument("mood_entry", moodEntry.getId(), user.getId());

        moodEntryRepository.delete(moodEntry);
        log.debug("Deleted mood entry: {}", code);
    }

    @Transactional(readOnly = true)
    public List<MoodEntry> getRecentMoodsByPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        // Default to last 7 days if no dates provided

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Get mood entries in the date range
        return moodEntryRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
    }

    private MoodEntryDto mapToDto(MoodEntry moodEntry) {
        return MoodEntryDto.builder()
                .code(moodEntry.getCode())
                .moodScore(moodEntry.getMoodScore())
                .moodTags(moodEntry.getMoodTags())
                .notes(moodEntry.getNotes())
                .createdAt(moodEntry.getCreatedAt())
                .build();
    }


}
