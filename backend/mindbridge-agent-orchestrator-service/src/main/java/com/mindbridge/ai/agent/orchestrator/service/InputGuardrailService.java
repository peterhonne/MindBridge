package com.mindbridge.ai.agent.orchestrator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InputGuardrailService {

    public final ChatClient.Builder chatClientBuilder;

    public InputGuardrailService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public static final String GENERAL_BLOCKED_RESPONSE = """
            I'm sorry, but I can't continue with that message.
            
            This app is designed to support well-being in a safe and respectful way. Please avoid sharing personal details, medical concerns, or anything that could be harmful to you or others.
            
            If you're going through something difficult, I strongly encourage you to reach out to a mental health professional or a support line in your area. You're not alone.
            """;

    public boolean shouldProcessMessage(String input, String keycloakUserId) {
        InputGuardrailResult inputGuardrailResult = chatClientBuilder.clone().build().prompt()
                .system("""
                        You are a safety and compliance advisor for a mental health support chatbot.
                        Your task is to evaluate a user's input and determine whether it is appropriate to be processed by an AI assistant.
                        
                        Evaluate the message for the following risks:
                        1. Signs of self-harm, suicide, or crisis (urgent mental health concerns).
                        2. Requests for medical diagnosis, prescriptions, or treatment.
                        3. Disclosure of personally identifiable information (PII) like full name, address, phone number, etc.
                        4. Hate speech, abuse, or language that targets individuals or groups.
                        5. Inappropriate, sexual, or manipulative content.
                        6. Any content that may violate data privacy laws (e.g., GDPR, HIPAA).
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
