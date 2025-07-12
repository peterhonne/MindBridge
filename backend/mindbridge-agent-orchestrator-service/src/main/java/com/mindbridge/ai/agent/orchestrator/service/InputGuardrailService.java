package com.mindbridge.ai.agent.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InputGuardrailService {

    private final ChatClient.Builder chatClientBuilder;

    private final ChatMemory chatMemory;

    public static final String GENERAL_BLOCKED_RESPONSE = """
            I'm sorry, but I can't continue with that message.
            
            This app is designed to support well-being in a safe and respectful way. Please avoid sharing personal details, medical concerns, or anything that could be harmful to you or others.
            
            If you're going through something difficult, I strongly encourage you to reach out to a mental health professional or a support line in your area. You're not alone.
            """;

    public boolean shouldProcessMessage(String input, String keycloakUserId) {
        List<Message> messageList = chatMemory.get(keycloakUserId);
        String memory = messageList.stream()
                .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
                .map(m -> m.getMessageType() + ":" + m.getText())
                .collect(Collectors.joining(System.lineSeparator()));
        InputGuardrailResult inputGuardrailResult = chatClientBuilder.clone().build().prompt()
                .system(memory + """
                        
                        Above is previous chat message history for your consideration.
                        --------
                        You are a safety and compliance advisor for a mental health support chatbot.
                        Your task is to evaluate a user's input, based on the full conversation.
                        
                        Evaluate the user message for the following risks:
                        1. Signs of self-harm, suicide, or crisis (urgent mental health concerns).
                        2. Requests for medical diagnosis, prescriptions, or treatment.
                        3. Disclosure of personally identifiable information (PII) like full name, address, phone number, etc.
                        4. Hate speech, abuse, or language that targets individuals or groups.
                        5. Inappropriate, sexual, or manipulative content.
                        6. Any content that may violate data privacy laws (e.g., GDPR, HIPAA).
                        
                        Mention which risk you concern.
                        """)
                .user(input)
                .call()
                .entity(InputGuardrailResult.class);

        if (inputGuardrailResult != null && Allowed.TRUE.equals(inputGuardrailResult.allowed())) {
            return true;
        } else {
            log.warn("Blocked input: {}; From user: {}; Issues: {}", input, keycloakUserId, inputGuardrailResult != null ? inputGuardrailResult.issues() : "Unknown");
            return false;
        }
    }

    public record InputGuardrailResult(Allowed allowed, String issues) {}

    public enum Allowed {
        TRUE, FALSE;
    }

}
