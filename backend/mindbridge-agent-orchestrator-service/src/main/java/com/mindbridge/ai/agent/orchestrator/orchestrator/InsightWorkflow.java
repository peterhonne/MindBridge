package com.mindbridge.ai.agent.orchestrator.orchestrator;

import com.mindbridge.ai.agent.orchestrator.models.dto.AssessmentInsights;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightWorkflow {

    private final ChatClient.Builder chatClientBuilder;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            8,                             // core pool size
            32,                            // max pool size
            60L, TimeUnit.SECONDS,         // keep-alive time
            new LinkedBlockingQueue<>(100), // queue capacity
            new ThreadPoolExecutor.CallerRunsPolicy() // fallback policy
    );

    @Async
    public CompletableFuture<AssessmentInsights> analyzeUserMoodComprehensively(
            List<String> journalEntries,
            List<String> recentMoodLogs,
            Long userId) {

        log.debug("Starting parallel mood analysis for user: {}", userId);

        CompletableFuture<String> emotionalAnalysis = analyzeEmotionalState(journalEntries)
                .exceptionally(ex -> {
                    log.error("Emotional analysis failed", ex);
                    return "Emotional analysis could not be completed.";
                });

        CompletableFuture<String> trendAnalysis = analyzeMoodTrends(recentMoodLogs)
                .exceptionally(ex -> {
                    log.error("Trend analysis failed", ex);
                    return "Mood trend analysis could not be completed.";
                });

        return emotionalAnalysis.thenCombine(trendAnalysis, (emotion, trend) ->
                AssessmentInsights.builder()
                        .emotionalState(emotion)
                        .trendAnalysis(trend)
                        .analysisTimestamp(LocalDateTime.now())
                        .build()
        );
    }

    private CompletableFuture<String> analyzeEmotionalState(List<String> journalEntries) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Analyzing emotional state in parallel");

            if (journalEntries.isEmpty()) {
                return "No recent mood data available for trend analysis.";
            }

            String journalHistory = String.join("\n", journalEntries);
            return chatClientBuilder.clone().build().prompt()
                    .system("""
                            Analyze the emotional state in recent journal entries.
                            Identify primary emotions, emotional intensity (1-10), and emotional triggers.
                            Provide supportive insights in a caring, non-judgmental tone.
                            """)
                    .user(journalHistory)
                    .call()
                    .content();
        }, executorService);
    }

    private CompletableFuture<String> analyzeMoodTrends(List<String> recentMoodLogs) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Analyzing mood trends in parallel");

            if (recentMoodLogs.isEmpty()) {
                return "No recent mood data available for trend analysis.";
            }

            String moodHistory = String.join("\n", recentMoodLogs);
            return chatClientBuilder.clone().build().prompt()
                    .system("""
                            Analyze mood trends from these recent mood logs.
                            Identify patterns, improvements, concerning trends, and cyclical behaviors.
                            Focus on actionable insights and progress indicators.
                            """)
                    .user(moodHistory)
                    .call()
                    .content();
        }, executorService);
    }

}
