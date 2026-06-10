package org.example.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Verifies sprint list + move-issue-to-sprint endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SprintEndpointsIntegrationTest {

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
    }

    static final String SPRINT1 = "d0000000-0000-0000-0000-000000000040";       // seeded ACTIVE sprint
    static final String WEB_PROJECT = "d0000000-0000-0000-0000-000000000030";
    static final String MEMBER = "d0000000-0000-0000-0000-0000000000a3";
    static final String CLOSED_SPRINT = "c1000000-0000-0000-0000-000000000099";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedClosedSprint() {
        jdbc.update("INSERT INTO sprints(id,project_id,name,state) VALUES (?::uuid,?::uuid,?,?) ON CONFLICT DO NOTHING",
                CLOSED_SPRINT, WEB_PROJECT, "Closed Sprint", "CLOSED");
    }

    private String createBacklogIssue() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/projects/WEB/issues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"Movable\",\"reporterId\":\"" + MEMBER + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(res.getResponse().getContentAsString()).get("key").asText();
    }

    @Test
    void listsSprintsForProject() throws Exception {
        mvc.perform(get("/api/v1/projects/WEB/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Sprint 1')]").exists());
    }

    @Test
    void movesIssueIntoSprintAndBackToBacklog() throws Exception {
        String key = createBacklogIssue();
        mvc.perform(put("/api/v1/issues/" + key + "/sprint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":\"" + SPRINT1 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(SPRINT1));

        mvc.perform(put("/api/v1/issues/" + key + "/sprint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(Matchers.nullValue()));
    }

    @Test
    void rejectsMoveIntoClosedSprint() throws Exception {
        String key = createBacklogIssue();
        mvc.perform(put("/api/v1/issues/" + key + "/sprint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sprintId\":\"" + CLOSED_SPRINT + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
