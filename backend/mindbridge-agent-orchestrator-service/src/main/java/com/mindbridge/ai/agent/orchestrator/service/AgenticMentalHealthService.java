package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.models.dto.AssessmentInsights;
import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.orchestrator.AgentOrchestrator;
import com.mindbridge.ai.agent.orchestrator.orchestrator.InsightWorkflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticMentalHealthService {

    private final AgentOrchestrator agentOrchestrator;

    private final UserProfileService userProfileService;

    private final InputGuardrailService inputGuardrailService;

    private final MoodService moodService;

    private final JournalService journalService;

    private final InsightWorkflow insightWorkflow;

    public String chat(String keycloakUserId, String userMsg) {
        boolean shouldProcessMessage = inputGuardrailService.shouldProcessMessage(userMsg, keycloakUserId);
        if (!shouldProcessMessage) return InputGuardrailService.GENERAL_BLOCKED_RESPONSE;
        User user = userProfileService.getUser(keycloakUserId);
        return agentOrchestrator.routeUserInputAndRespond(userMsg, user.getId(), keycloakUserId);
    }

    public AssessmentInsights performAssessment(String keycloakUserId, LocalDate startDate, LocalDate endDate) {
        User user = userProfileService.getUser(keycloakUserId);
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<String> recentMoodLogs = moodService.getRecentMoodsByPeriod(user.getId(), startDate, endDate)
                .stream()
                .map(mood -> String.format("Mood: %d/10, Tags: %s, Notes: %s",
                        mood.getMoodScore(), mood.getMoodTags(), mood.getNotes()))
                .collect(Collectors.toList());

        List<String> recentJournalLogs = journalService.getJournalEntriesByPeriod(user.getId(), startDate, endDate)
                .stream()
                .map(journal -> String.format("Title: %s, Content: %s, Mood before: %d/10, Mood after: %d/10, Tags: %s",
                        journal.getTitle(), journal.getContent(), journal.getMoodBefore(), journal.getMoodAfter(), journal.getTags()))
                .collect(Collectors.toList());

        try {
            return insightWorkflow.analyzeUserMoodComprehensively(
                    recentJournalLogs, recentMoodLogs, user.getId()
            ).get();

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error in parallel mood analysis for user {}: {}", keycloakUserId, e.getMessage());
            throw new RuntimeException("Mood analysis temporarily unavailable", e);
        }
    }


}
