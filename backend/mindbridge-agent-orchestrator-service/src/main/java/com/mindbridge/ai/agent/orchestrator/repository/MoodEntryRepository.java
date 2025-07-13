package com.mindbridge.ai.agent.orchestrator.repository;


import com.mindbridge.ai.agent.orchestrator.models.entity.MoodEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {


    Page<MoodEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<MoodEntry> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(m.moodScore) FROM MoodEntry m WHERE m.user.id = :userId " +
            "AND m.createdAt BETWEEN :start AND :end")
    Double getAverageMoodScore(@Param("userId") Long userId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT tag
            FROM mood_entries, unnest(mood_tags) AS tag
            WHERE user_id = :userId
            GROUP BY tag
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<String> findMostCommonMoodTags(@Param("userId") Long userId);

    Optional<MoodEntry> findByCode(String code);
}
