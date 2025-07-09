package com.mindbridge.ai.agent.orchestrator.repository;

import com.mindbridge.ai.agent.orchestrator.models.entity.JournalEntry;
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
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Page<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<JournalEntry> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT j FROM JournalEntry j WHERE j.user.id = :userId " +
            "AND LOWER(j.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<JournalEntry> searchByContent(@Param("userId") Long userId,
                                       @Param("searchTerm") String searchTerm);

    @Query(value = "SELECT * FROM journal_entries j WHERE j.user_id = :userId AND :tag = ANY(j.tags)", nativeQuery = true)
    List<JournalEntry> findByUserIdAndTag(@Param("userId") Long userId,
                                          @Param("tag") String tag);

    Optional<JournalEntry> findByCode(String code);
}
