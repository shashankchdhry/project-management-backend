package org.example.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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

/** End-to-end test of the issue/sprint slice over HTTP against real PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IssueApiIntegrationTest {

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
        registry.add("app.security.enabled", () -> "false"); // this test covers business logic, not auth
    }

    // fixed ids so seeding is idempotent across runs (ON CONFLICT DO NOTHING)
    static final UUID WS = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID WF = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID TODO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID INPROG = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    static final UUID DONE = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    static final UUID PROJECT = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID T_INIT = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID T_START = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID T_FINISH = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO workspaces(id,key,name) VALUES (?,?,?) ON CONFLICT DO NOTHING", WS, "ACME", "Acme");
        jdbc.update("INSERT INTO users(id,workspace_id,email,display_name,password_hash) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                USER, WS, "a@acme.test", "Alice", "x");
        jdbc.update("INSERT INTO workflows(id,name) VALUES (?,?) ON CONFLICT DO NOTHING", WF, "Default");
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", TODO, WF, "To Do", "TODO", 0);
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", INPROG, WF, "In Progress", "IN_PROGRESS", 1);
        jdbc.update("INSERT INTO workflow_statuses(id,workflow_id,name,category,position) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", DONE, WF, "Done", "DONE", 2);
        jdbc.update("INSERT INTO workflow_transitions(id,workflow_id,name,from_status_id,to_status_id) VALUES (?,?,?,NULL,?) ON CONFLICT DO NOTHING", T_INIT, WF, "Create", TODO);
        jdbc.update("INSERT INTO workflow_transitions(id,workflow_id,name,from_status_id,to_status_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", T_START, WF, "Start Progress", TODO, INPROG);
        jdbc.update("INSERT INTO workflow_transitions(id,workflow_id,name,from_status_id,to_status_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", T_FINISH, WF, "Finish", INPROG, DONE);
        jdbc.update("INSERT INTO projects(id,workspace_id,key,name,workflow_id) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING", PROJECT, WS, "PROJ", "Project", WF);
    }

    @Test
    void issueLifecycleAndSprint() throws Exception {
        // CREATE
        String createBody = "{\"type\":\"STORY\",\"title\":\"OAuth login\",\"reporterId\":\"" + USER + "\"}";
        MvcResult created = mvc.perform(post("/api/v1/projects/PROJ/issues")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").exists())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.statusId").value(TODO.toString()))
                .andReturn();
        String key = om.readTree(created.getResponse().getContentAsString()).get("key").asText();

        // GET
        mvc.perform(get("/api/v1/issues/" + key))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));

        // ILLEGAL transition To Do -> Done  => 422 with allowed list
        mvc.perform(post("/api/v1/issues/" + key + "/transitions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"Done\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.allowedTransitions[0]").value("In Progress"));

        // VALID transition -> In Progress
        mvc.perform(post("/api/v1/issues/" + key + "/transitions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"In Progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusId").value(INPROG.toString()));

        // STALE-VERSION update => 409 with current state
        mvc.perform(patch("/api/v1/issues/" + key)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":999,\"title\":\"hijack\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.current.key").value(key));

        // SPRINT create -> start -> complete (empty sprint => velocity 0)
        MvcResult sprint = mvc.perform(post("/api/v1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectKey\":\"PROJ\",\"name\":\"Sprint 1\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sprintId = om.readTree(sprint.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/v1/sprints/" + sprintId + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"));

        mvc.perform(post("/api/v1/sprints/" + sprintId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.velocity").value(0));
    }
}
