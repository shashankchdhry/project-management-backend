package org.example.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.out.persistence.entity.IssueEntity;
import org.example.adapter.out.persistence.entity.WorkflowStatusEntity;
import org.example.application.port.out.BoardReadPort;
import org.example.application.query.BoardCardView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Board CQRS read model store. Read models are query-optimized infrastructure, so this uses raw
 * SQL (JdbcTemplate) directly. {@link #upsert} is the write side (called by the board projector);
 * {@link #findByProject} is the read side ({@link BoardReadPort}).
 */
@Component
@RequiredArgsConstructor
public class BoardViewAdapter implements BoardReadPort {

    private final JdbcTemplate jdbc;

    public void upsert(IssueEntity issue, WorkflowStatusEntity status) {
        List<String> names = issue.getAssigneeId() == null ? List.of()
                : jdbc.queryForList("SELECT display_name FROM users WHERE id = ?", String.class, issue.getAssigneeId());
        String assigneeName = names.isEmpty() ? null : names.get(0);
        String rank = String.format("%012d", issue.getSeq());

        jdbc.update("""
                INSERT INTO issue_board_view
                    (issue_id, project_id, status_id, status_name, status_category, rank, issue_key,
                     type, title, priority, assignee_id, assignee_name, story_points, sprint_id, version, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (issue_id) DO UPDATE SET
                    status_id = excluded.status_id, status_name = excluded.status_name,
                    status_category = excluded.status_category, rank = excluded.rank,
                    title = excluded.title, priority = excluded.priority,
                    assignee_id = excluded.assignee_id, assignee_name = excluded.assignee_name,
                    story_points = excluded.story_points, sprint_id = excluded.sprint_id,
                    version = excluded.version, updated_at = excluded.updated_at
                """,
                issue.getId(), issue.getProjectId(), status.getId(), status.getName(), status.getCategory(),
                rank, issue.getKey(), issue.getType(), issue.getTitle(), issue.getPriority(),
                issue.getAssigneeId(), assigneeName, issue.getStoryPoints(), issue.getSprintId(),
                issue.getVersion(), OffsetDateTime.ofInstant(issue.getUpdatedAt(), ZoneOffset.UTC));
    }

    @Override
    public List<BoardCardView> findByProject(UUID projectId) {
        return jdbc.query("""
                SELECT issue_id, issue_key, status_id, type, title, priority,
                       assignee_id, assignee_name, story_points, version
                FROM issue_board_view
                WHERE project_id = ?
                ORDER BY status_id, rank
                """,
                (rs, n) -> new BoardCardView(
                        rs.getObject("issue_id", UUID.class),
                        rs.getString("issue_key"),
                        rs.getObject("status_id", UUID.class),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("priority"),
                        rs.getObject("assignee_id", UUID.class),
                        rs.getString("assignee_name"),
                        (Integer) rs.getObject("story_points"),
                        rs.getLong("version")),
                projectId);
    }
}
