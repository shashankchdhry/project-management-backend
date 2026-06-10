package org.example.adapter.in.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.example.adapter.out.events.OutboxRelay;
import org.example.application.command.CreateIssueCommand;
import org.example.application.command.CreateIssueService;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the real-time chain end-to-end: a domain event flows outbox → relay → Redis pub/sub →
 * STOMP, and a subscribed WebSocket client receives the board broadcast.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StompRealtimeIntegrationTest {

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
    }

    static final UUID WS = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID WF = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID TODO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID PROJECT = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @LocalServerPort int port;
    @Autowired CreateIssueService createIssue;
    @Autowired OutboxRelay relay;
    @Autowired JdbcTemplate jdbc;

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
    void broadcastsBoardEventToSubscriber() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());

        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/projects/" + PROJECT + "/board", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        });
        Thread.sleep(500); // let the subscription register before we publish

        createIssue.create(new CreateIssueCommand("PROJ", IssueType.STORY, "Realtime!", null,
                Priority.MEDIUM, USER, null, null), "corr-ws");
        relay.poll(); // event -> Redis -> listener -> STOMP

        String message = received.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).contains("IssueCreated");

        session.disconnect();
    }
}
