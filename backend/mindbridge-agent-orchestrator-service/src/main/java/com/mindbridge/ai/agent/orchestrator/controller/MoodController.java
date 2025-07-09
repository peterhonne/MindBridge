package com.mindbridge.ai.agent.orchestrator.controller;


import com.mindbridge.ai.agent.orchestrator.models.dto.CreateMoodEntryRequest;
import com.mindbridge.ai.agent.orchestrator.models.dto.MoodEntryDto;
import com.mindbridge.ai.agent.orchestrator.models.dto.MoodStatsDto;
import com.mindbridge.ai.agent.orchestrator.service.MoodService;
import com.mindbridge.ai.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/mood")
@RequiredArgsConstructor
public class MoodController {

    private final MoodService moodTrackingService;

    @PostMapping
    public ResponseEntity<MoodEntryDto> createMoodEntry(@Valid @RequestBody CreateMoodEntryRequest request) {
        MoodEntryDto moodEntry = moodTrackingService.createMoodEntry(request, SecurityUtils.getKeycloakUserId());
        return ResponseEntity.ok(moodEntry);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<MoodEntryDto>> getMoodHistory(@RequestParam(value = "page", defaultValue = "0", required = false) int pageNumber,
                                                             @RequestParam(value = "size", defaultValue = "20", required = false) int pageSize) {
        Page<MoodEntryDto> moodHistory = moodTrackingService.getMoodHistory(SecurityUtils.getKeycloakUserId(),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(moodHistory);
    }

    @GetMapping("/trends")
    public ResponseEntity<MoodStatsDto> getMoodTrends(@RequestParam(required = false) LocalDate startDate,
                                                      @RequestParam(required = false) LocalDate endDate) {
        MoodStatsDto stats = moodTrackingService.getMoodStats(SecurityUtils.getKeycloakUserId(), startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteJournalEntry(@PathVariable String code) {
        moodTrackingService.deleteMoodEntry(code, SecurityUtils.getKeycloakUserId());
        return ResponseEntity.noContent().build();
    }
}
