package com.mindbridge.ai.agent.orchestrator.controller;

import com.mindbridge.ai.agent.orchestrator.models.dto.AssessmentInsights;
import com.mindbridge.ai.agent.orchestrator.models.dto.MoodStatsDto;
import com.mindbridge.ai.agent.orchestrator.orchestrator.AgentOrchestrator;
import com.mindbridge.ai.agent.orchestrator.service.AgenticMentalHealthService;
import com.mindbridge.ai.agent.orchestrator.service.MoodService;
import com.mindbridge.ai.common.annotation.SysLog;
import com.mindbridge.ai.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MoodService moodTrackingService;

    private final AgenticMentalHealthService agenticMentalHealthService;

    @SysLog("get mood trends for dashboard")
    @GetMapping("/mood/trends")
    public ResponseEntity<MoodStatsDto> getMoodTrends(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        MoodStatsDto stats = moodTrackingService.getMoodStats(SecurityUtils.getKeycloakUserId(), startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/mood/insight")
    public ResponseEntity<AssessmentInsights> getMoodInsight(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        AssessmentInsights assessmentInsights = agenticMentalHealthService.performAssessment(SecurityUtils.getKeycloakUserId(), startDate, endDate);
        return ResponseEntity.ok(assessmentInsights);
    }


}
