package org.example.adapter.out.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies the outbox relay + projectors: a created issue lands in the board read model + activity feed. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BoardProjectionIntegrationTest {

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
        registry.add("app.security.enabled", () -> "false"); // this test covers projections, not auth
        registry.add("outbox.relay.delay-ms", () -> "3600000"); // disable auto-poll; we drive it explicitly
    }

    static final UUID WS = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID WF = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID TODO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID INPROG = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    static final UUID PROJECT = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxRelay relay;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO workspaces(id,key,name) VALUES (?,?,?) ON CONFLICT DO NOTHING", WS, "ACME", "Acme");
        jdbc.update("INSERT INTO users(id,workspace_id,email,display_name,password_hash) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                USER, WS, "a@acme.test", "Alice", "x");
        jdbc.update("INSERT INTO workflows(id,name) VALUES (?,?) ON CONFLICT DO NOTHING", WF, "Default");
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", TODO, WF, "To Do", "TODO", 0);
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", INPROG, WF, "In Progress", "IN_PROGRESS", 1);
        jdbc.update("INSERT INTO workflow_transitions(id,workflow_id,name,from_status_id,to_status_id) VALUES (?,?,?,NULL,?) ON CONFLICT DO NOTHING",
                UUID.fromString("00000000-0000-0000-0000-0000000000b1"), WF, "Create", TODO);
        jdbc.update("INSERT INTO projects(id,workspace_id,key,name,workflow_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", PROJECT, WS, "PROJ", "Project", WF);
    }

    @Test
    void createdIssueIsProjectedToBoardAndActivityFeed() throws Exception {
        String body = "{\"type\":\"STORY\",\"title\":\"Board me\",\"reporterId\":\"" + USER + "\"}";
        MvcResult created = mvc.perform(post("/api/v1/projects/PROJ/issues")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String key = om.readTree(created.getResponse().getContentAsString()).get("key").asText();

        relay.poll(); // drain the outbox -> projectors run

        mvc.perform(get("/api/v1/projects/PROJ/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].name").value("To Do"))
                .andExpect(jsonPath("$.columns[0].cards[0].issueKey").value(key))
                .andExpect(jsonPath("$.columns[0].cards[0].title").value("Board me"));

        Integer activity = jdbc.queryForObject(
                "SELECT count(*) FROM activity_log WHERE event_type = 'IssueCreated'", Integer.class);
        assertThat(activity).isGreaterThanOrEqualTo(1);

        // replay endpoint returns the durable event stream
        mvc.perform(get("/api/v1/projects/PROJ/events?after=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].type").value("IssueCreated"));
    }
}
