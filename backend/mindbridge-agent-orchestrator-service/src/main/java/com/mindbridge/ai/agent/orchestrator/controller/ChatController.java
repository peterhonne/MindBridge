package com.mindbridge.ai.agent.orchestrator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mindbridge.ai.agent.orchestrator.models.dto.ChatMessageDto;
import com.mindbridge.ai.agent.orchestrator.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Date;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatService chatService;

    @MessageMapping("/agent/conversation")
    public void chat(@Payload ChatMessageDto userMsg, SimpMessageHeaderAccessor headerAccessor) throws JsonProcessingException {
        String userId = headerAccessor.getUser().getName();
        messagingTemplate.convertAndSendToUser( userId, "/queue/chat", new ChatMessageDto(chatService.chat(userId, userMsg.content()), LocalDateTime.now(), "agent"));
    }

}
