package org.example.adapter.out.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.IssueSearchPort;
import org.example.application.query.IssueSearchCriteria;
import org.example.application.query.IssueSummary;
import org.example.application.query.SearchPage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Full-text + structured search over issues. Builds a parameterized SQL query — only
 * whitelisted fields become predicates and every value is a bound parameter, so user input can
 * never reach the SQL as a literal. Uses the GIN index via {@code websearch_to_tsquery} and
 * keyset (cursor) pagination ordered by relevance when searching, else by recency.
 */
@Component
@RequiredArgsConstructor
public class IssueSearchAdapter implements IssueSearchPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SearchPage search(IssueSearchCriteria c, List<UUID> allowedProjectIds) {
        boolean fts = c.q() != null && !c.q().isBlank();
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(
                "SELECT i.id, i.key, i.type, i.title, i.status_id, s.name AS status_name, i.priority, "
                        + "i.assignee_id, i.story_points, i.created_at, i.updated_at");
        if (fts) {
            sql.append(", ts_rank(i.search_vector, websearch_to_tsquery('english', :q))::float8 AS rank");
            params.addValue("q", c.q());
        }
        sql.append(" FROM issues i JOIN workflow_statuses s ON s.id = i.status_id WHERE 1=1");

        // Row-level scoping: restrict to the caller's projects.
        if (allowedProjectIds != null) {
            if (allowedProjectIds.isEmpty()) {
                sql.append(" AND 1=0");
            } else {
                sql.append(" AND i.project_id IN (:scope)");
                params.addValue("scope", allowedProjectIds);
            }
        }
        if (fts) {
            sql.append(" AND i.search_vector @@ websearch_to_tsquery('english', :q)");
        }
        // Whitelisted structured filters — each maps to a known column with a bound value.
        if (c.projectKey() != null) {
            sql.append(" AND i.project_id = (SELECT id FROM projects WHERE key = :pk)");
            params.addValue("pk", c.projectKey());
        }
        if (c.status() != null) {
            sql.append(" AND s.name = :status");
            params.addValue("status", c.status());
        }
        if (c.assignee() != null) {
            sql.append(" AND i.assignee_id = :assignee");
            params.addValue("assignee", c.assignee());
        }
        if (c.type() != null) {
            sql.append(" AND i.type = :type");
            params.addValue("type", c.type());
        }
        if (c.priority() != null) {
            sql.append(" AND i.priority = :priority");
            params.addValue("priority", c.priority());
        }
        if (c.sprintId() != null) {
            sql.append(" AND i.sprint_id = :sprint");
            params.addValue("sprint", c.sprintId());
        }

        // Keyset pagination: continue strictly after the cursor's (sortKey, id).
        Cursor cursor = decode(c.cursor());
        if (cursor != null) {
            if (fts && cursor.rank != null) {
                sql.append(" AND (ts_rank(i.search_vector, websearch_to_tsquery('english', :q))::float8 < :ckRank"
                        + " OR (ts_rank(i.search_vector, websearch_to_tsquery('english', :q))::float8 = :ckRank AND i.id < :ckId))");
                params.addValue("ckRank", cursor.rank);
                params.addValue("ckId", cursor.id);
            } else if (!fts && cursor.ts != null) {
                sql.append(" AND (i.created_at < :ckTs OR (i.created_at = :ckTs AND i.id < :ckId))");
                params.addValue("ckTs", java.time.OffsetDateTime.ofInstant(cursor.ts, java.time.ZoneOffset.UTC));
                params.addValue("ckId", cursor.id);
            }
        }

        sql.append(fts ? " ORDER BY rank DESC, i.id DESC" : " ORDER BY i.created_at DESC, i.id DESC");
        sql.append(" LIMIT :lim");
        params.addValue("lim", c.limit() + 1);

        List<RankedRow> rows = jdbc.query(sql.toString(), params, (rs, n) -> {
            IssueSummary summary = new IssueSummary(
                    rs.getObject("id", UUID.class), rs.getString("key"), rs.getString("type"),
                    rs.getString("title"), rs.getObject("status_id", UUID.class), rs.getString("status_name"),
                    rs.getString("priority"), rs.getObject("assignee_id", UUID.class),
                    (Integer) rs.getObject("story_points"),
                    rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
            double rank = fts ? rs.getDouble("rank") : 0.0;
            return new RankedRow(summary, rank);
        });

        boolean hasMore = rows.size() > c.limit();
        List<RankedRow> pageRows = hasMore ? rows.subList(0, c.limit()) : rows;
        String nextCursor = null;
        if (hasMore && !pageRows.isEmpty()) {
            RankedRow last = pageRows.get(pageRows.size() - 1);
            nextCursor = fts
                    ? encode("R", Double.toString(last.rank), last.summary.id())
                    : encode("T", last.summary.createdAt().toString(), last.summary.id());
        }
        List<IssueSummary> items = new ArrayList<>(pageRows.size());
        for (RankedRow r : pageRows) {
            items.add(r.summary);
        }
        return new SearchPage(items, nextCursor, hasMore);
    }

    private record RankedRow(IssueSummary summary, double rank) {
    }

    private record Cursor(Double rank, Instant ts, UUID id) {
    }

    private static String encode(String mode, String key, UUID id) {
        String raw = mode + "|" + key + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            UUID id = UUID.fromString(parts[2]);
            if ("R".equals(parts[0])) {
                return new Cursor(Double.parseDouble(parts[1]), null, id);
            }
            return new Cursor(null, Instant.parse(parts[1]), id); // full precision (preserves micros)
        } catch (RuntimeException e) {
            return null; // malformed cursor -> treat as first page
        }
    }
}
