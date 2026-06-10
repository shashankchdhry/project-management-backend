package org.example.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.example.adapter.out.events.OutboxRelay;
import org.example.application.command.CreateIssueCommand;
import org.example.application.command.CreateIssueService;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The notification service is down. Asserts the circuit breaker opens, board
 * operations keep succeeding, and notifications are queued as PENDING for later delivery.
 */
@SpringBootTest
@Testcontainers
class NotificationCircuitBreakerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("outbox.relay.delay-ms", () -> "3600000");
        registry.add("notification.simulate-failure", () -> "true"); // pretend the service is down
    }

    static final UUID WS = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID WF = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID TODO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID PROJECT = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Autowired CreateIssueService createIssue;
    @Autowired OutboxRelay relay;
    @Autowired JdbcTemplate jdbc;
    @Autowired CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO workspaces(id,key,name) VALUES (?,?,?) ON CONFLICT DO NOTHING", WS, "ACME", "Acme");
        jdbc.update("INSERT INTO users(id,workspace_id,email,display_name,password_hash) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                USER, WS, "a@acme.test", "Alice", "x");
        jdbc.update("INSERT INTO workflows(id,name) VALUES (?,?) ON CONFLICT DO NOTHING", WF, "Default");
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", TODO, WF, "To Do", "TODO", 0);
        jdbc.update("INSERT INTO workflow_transitions(id,workflow_id,name,from_status_id,to_status_id) VALUES (?,?,?,NULL,?) ON CONFLICT DO NOTHING",
                UUID.fromString("00000000-0000-0000-0000-0000000000b1"), WF, "Create", TODO);
        jdbc.update("INSERT INTO projects(id,workspace_id,key,name,workflow_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", PROJECT, WS, "PROJ", "Project", WF);
    }

    @Test
    void breakerOpensBoardKeepsWorkingNotificationsQueued() {
        for (int i = 1; i <= 8; i++) {
            createIssue.create(new CreateIssueCommand("PROJ", IssueType.TASK, "Issue " + i, null,
                    Priority.MEDIUM, USER, null, null), "corr-s4");
        }
        relay.poll(); // dispatch all events -> notification sends fail -> breaker trips

        Integer issues = jdbc.queryForObject("SELECT count(*) FROM issues WHERE project_id = ?", Integer.class, PROJECT);
        assertThat(issues).isEqualTo(8); // board operations unaffected by notification outage

        Integer pending = jdbc.queryForObject("SELECT count(*) FROM notifications WHERE status = 'PENDING'", Integer.class);
        assertThat(pending).isGreaterThanOrEqualTo(5); // notifications queued for later delivery

        CircuitBreaker breaker = circuitBreakers.circuitBreaker("notification");
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // breaker tripped
    }
}
