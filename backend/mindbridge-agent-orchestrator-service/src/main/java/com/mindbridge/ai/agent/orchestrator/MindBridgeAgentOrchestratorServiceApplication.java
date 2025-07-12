package com.mindbridge.ai.agent.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.mindbridge.ai.agent.orchestrator", "com.mindbridge.ai.common"})
@EntityScan(basePackages = {"com.mindbridge.ai.agent.orchestrator", "com.mindbridge.ai.common"})
@EnableJpaRepositories(basePackages = {"com.mindbridge.ai.agent.orchestrator", "com.mindbridge.ai.common"})
public class MindBridgeAgentOrchestratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindBridgeAgentOrchestratorServiceApplication.class, args);
	}

}
