package org.example.adapter.out.persistence.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.adapter.out.persistence.entity.IssueEntity;
import org.example.domain.issue.Issue;
import org.example.domain.issue.IssueType;
import org.example.domain.issue.Priority;
import org.springframework.stereotype.Component;

/** Translates between the {@link Issue} domain aggregate and {@link IssueEntity}. */
@Component
public class IssueMapper {

    public Issue toDomain(IssueEntity e) {
        List<String> labels = e.getLabels() == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(e.getLabels()));
        Map<String, Object> custom = e.getCustomFields() == null ? new LinkedHashMap<>() : e.getCustomFields();
        return Issue.rehydrate(
                e.getId(), e.getKey(), e.getSeq(), e.getProjectId(), IssueType.valueOf(e.getType()),
                e.getReporterId(), e.getParentId(), e.getTitle(), e.getDescription(), e.getStatusId(),
                Priority.valueOf(e.getPriority()), e.getSprintId(), e.getAssigneeId(), e.getStoryPoints(),
                labels, custom, e.getVersion(), e.getCreatedAt(), e.getUpdatedAt());
    }

    /** Build a fresh entity for a new issue (all columns, version defaults to 0). */
    public IssueEntity newEntity(Issue i) {
        IssueEntity e = new IssueEntity();
        e.setId(i.id());
        e.setKey(i.key());
        e.setSeq(i.seq());
        e.setProjectId(i.projectId());
        e.setType(i.type().name());
        e.setReporterId(i.reporterId());
        e.setParentId(i.parentId());
        e.setCreatedAt(i.createdAt());
        applyMutable(i, e);
        return e;
    }

    /** Copy mutable fields onto a managed entity; leaves {@code version} to JPA. */
    public void applyMutable(Issue i, IssueEntity e) {
        e.setTitle(i.title());
        e.setDescription(i.description());
        e.setStatusId(i.statusId());
        e.setPriority(i.priority().name());
        e.setSprintId(i.sprintId());
        e.setAssigneeId(i.assigneeId());
        e.setStoryPoints(i.storyPoints());
        e.setLabels(i.labels().toArray(new String[0]));
        e.setCustomFields(i.customFields());
        e.setUpdatedAt(i.updatedAt());
    }
}
