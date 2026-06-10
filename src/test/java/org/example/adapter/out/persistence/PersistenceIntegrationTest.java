package org.example.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.application.port.out.AdvisoryLockPort;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.domain.issue.Issue;
import org.example.domain.issue.IssueCreated;
import org.example.domain.issue.IssueDetailsUpdate;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.example.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the persistence adapter against a real PostgreSQL: Flyway migrations apply, Hibernate
 * validates the entities against the migrated schema (ddl-auto=validate), and the jsonb/array/
 * generated-column mappings, optimistic-lock version, advisory lock, sequence allocation, and
 * transactional outbox all behave as designed.
 */
@SpringBootTest
@Testcontainers
class PersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    static final UUID WORKSPACE = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID WORKFLOW = UUID.randomUUID();
    static final UUID TODO = UUID.randomUUID();
    static final UUID DONE = UUID.randomUUID();
    static final UUID PROJECT = UUID.randomUUID();

    @Autowired IssueRepositoryPort issues;
    @Autowired ProjectRepositoryPort projects;
    @Autowired EventOutboxPort outbox;
    @Autowired AdvisoryLockPort locks;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    TransactionTemplate tx;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);
        jdbc.update("INSERT INTO workspaces(id,key,name) VALUES (?,?,?) ON CONFLICT DO NOTHING", WORKSPACE, "ACME", "Acme");
        jdbc.update("INSERT INTO users(id,workspace_id,email,display_name,password_hash) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                USER, WORKSPACE, "a@acme.test", "Alice", "x");
        jdbc.update("INSERT INTO workflows(id,name) VALUES (?,?) ON CONFLICT DO NOTHING", WORKFLOW, "Default");
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                TODO, WORKFLOW, "To Do", "TODO", 0);
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                DONE, WORKFLOW, "Done", "DONE", 1);
        jdbc.update("INSERT INTO projects(id,workspace_id,key,name,workflow_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                PROJECT, WORKSPACE, "PROJ", "Project", WORKFLOW);
    }

    @Test
    void roundTripsIssueWithJsonbArrayAndGeneratedSearchVector() {
        long seq = tx.execute(s -> projects.nextIssueSeq(PROJECT));
        UUID issueId = UUID.randomUUID();
        Issue created = Issue.create(issueId, "PROJ-" + seq, seq, PROJECT, IssueType.STORY,
                "OAuth login", "Implement OAuth 2.0 flow", TODO, Priority.HIGH, USER, null, 5);
        created.updateDetails(new IssueDetailsUpdate(null, null, null, null, null, List.of("auth", "backend")));
        tx.executeWithoutResult(s -> issues.save(created));

        Issue loaded = tx.execute(s -> issues.findByKey("PROJ-" + seq).orElseThrow());
        assertThat(loaded.title()).isEqualTo("OAuth login");
        assertThat(loaded.priority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.storyPoints()).isEqualTo(5);
        assertThat(loaded.labels()).containsExactly("auth", "backend");

        String tsv = jdbc.queryForObject("SELECT search_vector::text FROM issues WHERE id = ?", String.class, issueId);
        assertThat(tsv).contains("oauth"); // generated full-text column is populated
    }

    @Test
    void incrementsVersionOnUpdate() {
        long seq = tx.execute(s -> projects.nextIssueSeq(PROJECT));
        UUID id = UUID.randomUUID();
        Issue issue = Issue.create(id, "PROJ-" + seq, seq, PROJECT, IssueType.TASK,
                "t", "d", TODO, Priority.LOW, USER, null, null);
        tx.executeWithoutResult(s -> issues.save(issue));

        Issue v0 = tx.execute(s -> issues.findById(id).orElseThrow());
        assertThat(v0.version()).isZero();

        v0.updateDetails(new IssueDetailsUpdate("changed", null, null, null, null, null));
        tx.executeWithoutResult(s -> issues.save(v0));

        Issue v1 = tx.execute(s -> issues.findById(id).orElseThrow());
        assertThat(v1.version()).isEqualTo(1L);
    }

    @Test
    void advisoryLockAndSequenceAllocation() {
        long first = tx.execute(s -> {
            locks.acquireXact(AdvisoryLockPort.Namespace.SPRINT, PROJECT);
            return projects.nextEventSeq(PROJECT);
        });
        long second = tx.execute(s -> projects.nextEventSeq(PROJECT));
        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void appendsOutboxEventAsJsonb() {
        DomainEvent event = new IssueCreated(UUID.randomUUID(), "PROJ-99", PROJECT, IssueType.BUG,
                "boom", TODO, USER, Instant.now());
        tx.executeWithoutResult(s -> outbox.append(PROJECT, List.of(event), "corr-int-test"));

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM domain_event_log WHERE event_type = 'IssueCreated' AND correlation_id = 'corr-int-test'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
