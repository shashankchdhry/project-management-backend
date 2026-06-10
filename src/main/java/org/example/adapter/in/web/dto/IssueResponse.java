package org.example.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.domain.issue.Issue;

public record IssueResponse(
        UUID id,
        String key,
        UUID projectId,
        String type,
        String title,
        String description,
        UUID statusId,
        String priority,
        UUID assigneeId,
        UUID reporterId,
        UUID parentId,
        UUID sprintId,
        Integer storyPoints,
        List<String> labels,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static IssueResponse from(Issue i) {
        return new IssueResponse(i.id(), i.key(), i.projectId(), i.type().name(), i.title(), i.description(),
                i.statusId(), i.priority().name(), i.assigneeId(), i.reporterId(), i.parentId(), i.sprintId(),
                i.storyPoints(), i.labels(), i.version(), i.createdAt(), i.updatedAt());
    }
}
