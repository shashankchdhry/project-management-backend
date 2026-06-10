package org.example.application.command;

import lombok.RequiredArgsConstructor;
import org.example.application.port.out.AdvisoryLockPort;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.WorkflowRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.issue.Issue;
import org.example.domain.project.Project;
import org.example.domain.shared.BusinessRuleException;
import org.example.domain.workflow.Workflow;
import org.example.domain.workflow.WorkflowEngine;
import org.example.domain.workflow.WorkflowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransitionIssueService {

    private final IssueRepositoryPort issues;
    private final ProjectRepositoryPort projects;
    private final WorkflowRepositoryPort workflows;
    private final WorkflowEngine workflowEngine;
    private final AdvisoryLockPort locks;
    private final EventOutboxPort outbox;

    /**
     * Move an issue to {@code toStatusName}. Illegal transitions throw (→422 with allowed list);
     * WIP-limited target columns are enforced race-safely under an advisory lock.
     */
    @Transactional
    public Issue transition(String issueKey, String toStatusName, String correlationId) {
        Issue issue = issues.findByKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue " + issueKey + " not found"));
        Project project = projects.findById(issue.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project for issue " + issueKey + " not found"));
        Workflow workflow = workflows.load(project.workflowId());

        WorkflowStatus target = workflow.statuses().stream()
                .filter(s -> s.name().equalsIgnoreCase(toStatusName))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Unknown status: '" + toStatusName + "'"));

        if (target.hasWipLimit()) {
            locks.acquireXact(AdvisoryLockPort.Namespace.WIP, target.id());
            if (issues.countByStatus(target.id()) >= target.wipLimit()) {
                throw new BusinessRuleException("WIP limit reached for '" + target.name() + "'");
            }
        }

        workflowEngine.transition(issue, workflow, target.id());
        Issue saved = issues.save(issue);
        outbox.append(issue.projectId(), issue.pullDomainEvents(), correlationId);
        return saved;
    }
}
