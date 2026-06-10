package org.example.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.example.adapter.out.events.OutboxRelay;
import org.example.application.command.CreateIssueCommand;
import org.example.application.command.CreateIssueService;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Verifies the activity feed API: entries from the projector, filtering, and pagination. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ActivityFeedIntegrationTest {

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
        registry.add("app.security.enabled", () -> "false");
        registry.add("outbox.relay.delay-ms", () -> "3600000"); // we drive the relay explicitly
    }

    static final UUID MEMBER = UUID.fromString("d0000000-0000-0000-0000-0000000000a3");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired CreateIssueService createIssue;
    @Autowired OutboxRelay relay;

    @BeforeEach
    void generateActivity() {
        for (int i = 0; i < 3; i++) {
            createIssue.create(new CreateIssueCommand("WEB", IssueType.TASK, "Activity " + i, null,
                    Priority.MEDIUM, MEMBER, null, null), "corr-activity");
        }
        relay.poll(); // project the IssueCreated events into activity_log
    }

    private JsonNode getJson(String url) throws Exception {
        return om.readTree(mvc.perform(get(url)).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString());
    }

    private Set<String> idsOf(JsonNode response) {
        Set<String> ids = new HashSet<>();
        response.get("items").forEach(n -> ids.add(n.get("id").asText()));
        return ids;
    }

    @Test
    void returnsCreatedEntries() throws Exception {
        JsonNode res = getJson("/api/v1/projects/WEB/activity");
        assertThat(res.get("items").size()).isGreaterThanOrEqualTo(3);
        res.get("items").forEach(n -> assertThat(n.get("eventType").asText()).isEqualTo("IssueCreated"));
    }

    @Test
    void filterByEventTypeExcludesOthers() throws Exception {
        assertThat(getJson("/api/v1/projects/WEB/activity?type=StatusChanged").get("items").size()).isZero();
    }

    @Test
    void cursorPaginationWalksEntries() throws Exception {
        JsonNode page1 = getJson("/api/v1/projects/WEB/activity?limit=2");
        assertThat(page1.get("items").size()).isEqualTo(2);
        assertThat(page1.get("page").get("hasMore").asBoolean()).isTrue();
        String cursor = page1.get("page").get("nextCursor").asText();

        JsonNode page2 = getJson("/api/v1/projects/WEB/activity?limit=2&cursor=" + cursor);
        assertThat(page2.get("items").size()).isGreaterThanOrEqualTo(1);
        assertThat(idsOf(page1)).doesNotContainAnyElementsOf(idsOf(page2));
    }
}
