package org.example.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Verifies search & filtering against the demo seed (WEB-1 "OAuth 2.0 login" / In Progress,
 * WEB-2 "Board flickers" / To Do, WEB-3 "Write API docs" / Done): full-text, structured filter, and
 * cursor pagination.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SearchIntegrationTest {

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
        registry.add("app.security.enabled", () -> "false"); // exercise search itself, not auth
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private JsonNode getJson(String url) throws Exception {
        MvcResult res = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return om.readTree(res.getResponse().getContentAsString());
    }

    private List<String> keysOf(JsonNode response) {
        List<String> keys = new ArrayList<>();
        response.get("items").forEach(n -> keys.add(n.get("key").asText()));
        return keys;
    }

    @Test
    void fullTextSearchMatchesTitle() throws Exception {
        assertThat(keysOf(getJson("/api/v1/search?q=oauth"))).contains("WEB-1");
    }

    @Test
    void structuredFilterByStatus() throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/search").param("status", "To Do"))
                .andExpect(status().isOk()).andReturn();
        assertThat(keysOf(om.readTree(res.getResponse().getContentAsString()))).containsExactly("WEB-2");
    }

    @Test
    void cursorPaginationWalksAllResults() throws Exception {
        JsonNode page1 = getJson("/api/v1/search?limit=2");
        assertThat(page1.get("items").size()).isEqualTo(2);
        assertThat(page1.get("page").get("hasMore").asBoolean()).isTrue();
        String cursor = page1.get("page").get("nextCursor").asText();

        JsonNode page2 = getJson("/api/v1/search?limit=2&cursor=" + cursor);
        assertThat(page2.get("items").size()).isEqualTo(1);
        assertThat(page2.get("page").get("hasMore").asBoolean()).isFalse();

        Set<String> all = new HashSet<>(keysOf(page1));
        all.addAll(keysOf(page2));
        assertThat(all).containsExactlyInAnyOrder("WEB-1", "WEB-2", "WEB-3");
    }
}
