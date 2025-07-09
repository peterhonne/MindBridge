package com.mindbridge.ai.agent.orchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.ai.agent.orchestrator.advisor.RedisChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisAgentMessageTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(RedisTemplate<String, String> redisAgentMessageTemplate, ObjectMapper objectMapper) {
        return new RedisChatMemoryRepository(redisAgentMessageTemplate, objectMapper);
    }
}
