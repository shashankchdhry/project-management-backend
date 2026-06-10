package org.example.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.ActivityFeedPort;
import org.example.application.query.ActivityEntry;
import org.example.application.query.ActivityPage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads the activity feed from {@code activity_log} with keyset (cursor) pagination ordered newest
 * first, plus optional event-type / issue filters. Cursor is an opaque base64 of (created_at, id).
 */
@Component
@RequiredArgsConstructor
public class ActivityFeedAdapter implements ActivityFeedPort {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public ActivityPage feed(UUID projectId, String eventType, UUID issueId, String cursor, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("projectId", projectId);
        StringBuilder sql = new StringBuilder(
                "SELECT id, issue_id, actor_id, event_type, summary, changes::text AS changes, "
                        + "correlation_id, created_at FROM activity_log WHERE project_id = :projectId");
        if (eventType != null) {
            sql.append(" AND event_type = :eventType");
            params.addValue("eventType", eventType);
        }
        if (issueId != null) {
            sql.append(" AND issue_id = :issueId");
            params.addValue("issueId", issueId);
        }
        Cursor c = decode(cursor);
        if (c != null) {
            sql.append(" AND (created_at < :ckTs OR (created_at = :ckTs AND id < :ckId))");
            params.addValue("ckTs", OffsetDateTime.ofInstant(c.ts(), ZoneOffset.UTC));
            params.addValue("ckId", c.id());
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :lim");
        params.addValue("lim", limit + 1);

        List<ActivityEntry> rows = jdbc.query(sql.toString(), params, (rs, n) -> {
            String changesJson = rs.getString("changes");
            Map<String, Object> changes = null;
            if (changesJson != null) {
                try {
                    changes = objectMapper.readValue(changesJson, MAP_TYPE);
                } catch (Exception ignored) {
                    changes = null;
                }
            }
            return new ActivityEntry(
                    rs.getObject("id", UUID.class), rs.getObject("issue_id", UUID.class),
                    rs.getObject("actor_id", UUID.class), rs.getString("event_type"), rs.getString("summary"),
                    changes, rs.getString("correlation_id"), rs.getTimestamp("created_at").toInstant());
        });

        boolean hasMore = rows.size() > limit;
        List<ActivityEntry> page = hasMore ? List.copyOf(rows.subList(0, limit)) : rows;
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            ActivityEntry last = page.get(page.size() - 1);
            nextCursor = encode(last.createdAt(), last.id());
        }
        return new ActivityPage(page, nextCursor, hasMore);
    }

    private record Cursor(Instant ts, UUID id) {
    }

    private static String encode(Instant ts, UUID id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((ts.toString() + "|" + id).getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            return null; // malformed cursor -> treat as first page
        }
    }
}
