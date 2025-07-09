package com.mindbridge.ai.agent.orchestrator.controller;

import com.mindbridge.ai.agent.orchestrator.models.dto.ChatMessage;
import com.mindbridge.ai.agent.orchestrator.service.ChatHistoryService;
import com.mindbridge.ai.common.annotation.SysLog;
import com.mindbridge.ai.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/chat/history")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @SysLog("Get chat message histories")
    @GetMapping
    public ResponseEntity<List<ChatMessage>> getChatHistory() {
        return ResponseEntity.ok(chatHistoryService.getChatHistory(SecurityUtils.getUserId()));
    }

}
