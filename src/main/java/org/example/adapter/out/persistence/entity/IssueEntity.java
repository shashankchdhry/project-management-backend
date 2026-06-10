package org.example.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for the {@code issues} table. Intentionally separate from the {@code domain.issue.Issue}
 * aggregate — relationships are stored as plain UUID columns, not JPA associations,
 * to keep reads flat and avoid lazy-loading surprises (open-in-view is disabled).
 *
 * <p>The {@code search_vector} generated column is deliberately not mapped.
 */
@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
public class IssueEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "seq", nullable = false)
    private long seq;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "story_points")
    private Integer storyPoints;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "labels", columnDefinition = "text[]")
    private String[] labels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
