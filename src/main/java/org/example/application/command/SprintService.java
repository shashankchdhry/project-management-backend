package org.example.application.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.AdvisoryLockPort;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.SprintRepositoryPort;
import org.example.application.port.out.WorkflowRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.issue.Issue;
import org.example.domain.project.Project;
import org.example.domain.shared.BusinessRuleException;
import org.example.domain.shared.DomainEvent;
import org.example.domain.sprint.Sprint;
import org.example.domain.sprint.SprintCompletion;
import org.example.domain.sprint.SprintCompletionResult;
import org.example.domain.workflow.Workflow;
import org.example.domain.workflow.WorkflowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepositoryPort sprints;
    private final IssueRepositoryPort issues;
    private final ProjectRepositoryPort projects;
    private final WorkflowRepositoryPort workflows;
    private final SprintCompletion sprintCompletion;
    private final AdvisoryLockPort locks;
    private final EventOutboxPort outbox;

    /** Start a sprint. Serialized per sprint; only one ACTIVE sprint per project. */
    @Transactional
    public Sprint start(UUID sprintId, String correlationId) {
        locks.acquireXact(AdvisoryLockPort.Namespace.SPRINT, sprintId);
        Sprint sprint = sprints.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint " + sprintId + " not found"));
        sprints.findActiveByProject(sprint.projectId()).ifPresent(active -> {
            throw new BusinessRuleException("Project already has an active sprint");
        });
        sprint.start(Instant.now());
        Sprint saved = sprints.save(sprint);
        outbox.append(sprint.projectId(), sprint.pullDomainEvents(), correlationId);
        return saved;
    }

    /**
     * Complete a sprint: compute velocity from DONE issues, carry over the selected incomplete
     * ones, and close it — all under an advisory lock so concurrent completes can't double-apply.
     */
    @Transactional
    public SprintCompletionResult complete(UUID sprintId, CompleteSprintCommand cmd, String correlationId) {
        locks.acquireXact(AdvisoryLockPort.Namespace.SPRINT, sprintId);
        Sprint sprint = sprints.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint " + sprintId + " not found"));
        Project project = projects.findById(sprint.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project for sprint " + sprintId + " not found"));
        Workflow workflow = workflows.load(project.workflowId());
        Set<UUID> doneStatusIds = workflow.statuses().stream()
                .filter(WorkflowStatus::isDone)
                .map(WorkflowStatus::id)
                .collect(Collectors.toSet());

        List<Issue> sprintIssues = issues.findBySprint(sprintId);
        SprintCompletionResult result = sprintCompletion.complete(
                sprint, sprintIssues, doneStatusIds, cmd.carryOverIssueIds(), cmd.targetSprintId(), Instant.now());

        List<DomainEvent> events = new ArrayList<>(sprint.pullDomainEvents());
        sprints.save(sprint);
        for (Issue issue : sprintIssues) {
            List<DomainEvent> issueEvents = issue.pullDomainEvents();
            if (!issueEvents.isEmpty()) {
                issues.save(issue);
                events.addAll(issueEvents);
            }
        }
        outbox.append(project.id(), events, correlationId);
        return result;
    }
}
