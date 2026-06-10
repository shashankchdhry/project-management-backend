package org.example.application.config;

import org.example.domain.sprint.SprintCompletion;
import org.example.domain.workflow.WorkflowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the stateless domain services as beans. They live here (not as {@code @Component}) so
 * the domain layer stays framework-free.
 */
@Configuration
public class DomainConfig {

    @Bean
    public WorkflowEngine workflowEngine() {
        return new WorkflowEngine();
    }

    @Bean
    public SprintCompletion sprintCompletion() {
        return new SprintCompletion();
    }
}
