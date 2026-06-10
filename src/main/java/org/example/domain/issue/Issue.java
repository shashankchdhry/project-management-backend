package org.example.domain.issue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.domain.shared.AggregateRoot;

/**
 * The Issue aggregate root. All issue types share this class (single-table modeling).
 * Mutations record domain events; {@code version} backs optimistic locking at the
 * persistence boundary — the domain just carries it.
 */
public class Issue extends AggregateRoot {

    private final UUID id;
    private final String key;
    private final long seq;
    private final UUID projectId;
    private final IssueType type;
    private final UUID reporterId;
    private final UUID parentId;

    private String title;
    private String description;
    private UUID statusId;
    private Priority priority;
    private UUID sprintId;
    private UUID assigneeId;
    private Integer storyPoints;
    private List<String> labels;
    private Map<String, Object> customFields;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Issue(UUID id, String key, long seq, UUID projectId, IssueType type, UUID reporterId, UUID parentId,
                  String title, String description, UUID statusId, Priority priority, UUID sprintId, UUID assigneeId,
                  Integer storyPoints, List<String> labels, Map<String, Object> customFields, long version,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.key = key;
        this.seq = seq;
        this.projectId = projectId;
        this.type = type;
        this.reporterId = reporterId;
        this.parentId = parentId;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.priority = priority;
        this.sprintId = sprintId;
        this.assigneeId = assigneeId;
        this.storyPoints = storyPoints;
        this.labels = new ArrayList<>(labels);
        this.customFields = new LinkedHashMap<>(customFields);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Create a brand-new issue in the workflow's initial status. */
    public static Issue create(UUID id, String key, long seq, UUID projectId, IssueType type, String title,
                               String description, UUID initialStatusId, Priority priority, UUID reporterId,
                               UUID parentId, Integer storyPoints) {
        Instant now = Instant.now();
        Issue issue = new Issue(id, key, seq, projectId, type, reporterId, parentId, title, description,
                initialStatusId, priority, null, null, storyPoints, new ArrayList<>(), new LinkedHashMap<>(),
                0L, now, now);
        issue.registerEvent(new IssueCreated(id, key, projectId, type, title, initialStatusId, reporterId, now));
        return issue;
    }

    /** Rehydrate an issue from persistence without raising events. */
    public static Issue rehydrate(UUID id, String key, long seq, UUID projectId, IssueType type, UUID reporterId,
                                  UUID parentId, String title, String description, UUID statusId, Priority priority,
                                  UUID sprintId, UUID assigneeId, Integer storyPoints, List<String> labels,
                                  Map<String, Object> customFields, long version, Instant createdAt, Instant updatedAt) {
        return new Issue(id, key, seq, projectId, type, reporterId, parentId, title, description, statusId, priority,
                sprintId, assigneeId, storyPoints, labels, customFields, version, createdAt, updatedAt);
    }

    /** Apply a partial field update (PATCH); records one {@link IssueUpdated} if anything changed. */
    public void updateDetails(IssueDetailsUpdate update) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (update.title() != null && !update.title().equals(title)) {
            changes.put("title", change(title, update.title()));
            title = update.title();
        }
        if (update.description() != null && !update.description().equals(description)) {
            changes.put("description", "updated");
            description = update.description();
        }
        if (update.priority() != null && update.priority() != priority) {
            changes.put("priority", change(priority, update.priority()));
            priority = update.priority();
        }
        if (update.storyPoints() != null && !update.storyPoints().equals(storyPoints)) {
            changes.put("storyPoints", change(storyPoints, update.storyPoints()));
            storyPoints = update.storyPoints();
        }
        if (update.assigneeId() != null && !update.assigneeId().equals(assigneeId)) {
            changes.put("assignee", change(assigneeId, update.assigneeId()));
            assigneeId = update.assigneeId();
        }
        if (update.labels() != null && !update.labels().equals(labels)) {
            changes.put("labels", List.copyOf(update.labels()));
            labels = new ArrayList<>(update.labels());
        }
        if (!changes.isEmpty()) {
            updatedAt = Instant.now();
            registerEvent(new IssueUpdated(id, key, changes, updatedAt));
        }
    }

    /**
     * Apply a status change. Called by the {@code WorkflowEngine} after it has validated the
     * transition and guards — this method does not re-check workflow rules.
     */
    public void applyTransition(UUID fromStatusId, String fromStatusName, UUID toStatusId, String toStatusName) {
        this.statusId = toStatusId;
        this.updatedAt = Instant.now();
        registerEvent(new StatusChanged(id, key, fromStatusId, fromStatusName, toStatusId, toStatusName, updatedAt));
    }

    /** Move to a sprint (or {@code null} to return to the backlog). */
    public void moveToSprint(UUID destinationSprintId) {
        this.sprintId = destinationSprintId;
        this.updatedAt = Instant.now();
        registerEvent(new IssueMovedToSprint(id, key, destinationSprintId, updatedAt));
    }

    private static Map<String, Object> change(Object oldValue, Object newValue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("old", oldValue == null ? null : String.valueOf(oldValue));
        m.put("new", newValue == null ? null : String.valueOf(newValue));
        return m;
    }

    public UUID id() { return id; }
    public String key() { return key; }
    public long seq() { return seq; }
    public UUID projectId() { return projectId; }
    public IssueType type() { return type; }
    public UUID reporterId() { return reporterId; }
    public UUID parentId() { return parentId; }
    public String title() { return title; }
    public String description() { return description; }
    public UUID statusId() { return statusId; }
    public Priority priority() { return priority; }
    public UUID sprintId() { return sprintId; }
    public UUID assigneeId() { return assigneeId; }
    public Integer storyPoints() { return storyPoints; }
    public List<String> labels() { return List.copyOf(labels); }
    public Map<String, Object> customFields() { return Map.copyOf(customFields); }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
