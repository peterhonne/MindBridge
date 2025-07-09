package com.mindbridge.ai.agent.orchestrator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/mindbridge").setAllowedOriginPatterns("*");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT token from WebSocket connection
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            // Decode and validate JWT
                            Jwt jwt = jwtDecoder.decode(token);

                            // Extract user information
                            String userId = jwt.getClaimAsString("sub");
                            String username = jwt.getClaimAsString("preferred_username");

                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                    userId, null, List.of());

                            // Set user in WebSocket session
                            accessor.setUser(auth);
//                            accessor.setSessionId(userId);

                            // Add user info to session attributes
                            accessor.getSessionAttributes().put("userId", userId);
                            accessor.getSessionAttributes().put("username", username);
                            accessor.getSessionAttributes().put("roles", List.of());

                            log.info("WebSocket connection authenticated for user: {}", userId);

                        } catch (Exception e) {
                            log.error("WebSocket authentication failed", e);
                            throw new SecurityException("Invalid authentication token");
                        }
                    } else {
                        log.warn("WebSocket connection attempt without authentication");
                        throw new SecurityException("Authentication required");
                    }
                }

                return message;
            }
        });
    }


}
