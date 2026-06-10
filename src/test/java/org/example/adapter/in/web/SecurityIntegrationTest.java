package org.example.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Verifies JWT authentication and RBAC against the demo seed (WEB project, member/viewer users).
 * Unlike the other web tests, this one keeps the real security filter chain enabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {

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
    }

    // From the demo seed (R__seed_demo_data.sql); password for all = "password".
    static final String BCRYPT_PASSWORD = "$2a$10$MOMO.1DoagVuMHi1cSvpA.rK.zwEBKwNtNLh/zd9WqnzQ9sjLS01W";
    static final String DEMO_WS = "d0000000-0000-0000-0000-000000000001";
    static final String WEB_PROJECT = "d0000000-0000-0000-0000-000000000030";
    static final String MEMBER_ID = "d0000000-0000-0000-0000-0000000000a3";
    static final String VIEWER_ID = "d0000000-0000-0000-0000-0000000000a4";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedViewer() {
        jdbc.update("INSERT INTO users(id,workspace_id,email,display_name,password_hash) VALUES (?::uuid,?::uuid,?,?,?) ON CONFLICT DO NOTHING",
                VIEWER_ID, DEMO_WS, "viewer@demo.test", "Demo Viewer", BCRYPT_PASSWORD);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?::uuid,?::uuid,?) ON CONFLICT DO NOTHING",
                WEB_PROJECT, VIEWER_ID, "VIEWER");
    }

    private String login(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mvc.perform(post("/api/v1/projects/WEB/issues")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"TASK\",\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanCreateAndReporterIsTheAuthenticatedUser() throws Exception {
        String token = login("member@demo.test");
        mvc.perform(post("/api/v1/projects/WEB/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"Member task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterId").value(MEMBER_ID));
    }

    @Test
    void viewerCannotCreate() throws Exception {
        String token = login("viewer@demo.test");
        mvc.perform(post("/api/v1/projects/WEB/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"Nope\"}"))
                .andExpect(status().isForbidden());
    }
}
