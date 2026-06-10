package org.example.application.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.WorkflowRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.issue.Issue;
import org.example.domain.project.Project;
import org.example.domain.workflow.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateIssueService {

    private final ProjectRepositoryPort projects;
    private final WorkflowRepositoryPort workflows;
    private final IssueRepositoryPort issues;
    private final EventOutboxPort outbox;

    @Transactional
    public Issue create(CreateIssueCommand cmd, String correlationId) {
        Project project = projects.findByKey(cmd.projectKey())
                .orElseThrow(() -> new ResourceNotFoundException("Project " + cmd.projectKey() + " not found"));
        Workflow workflow = workflows.load(project.workflowId());

        long seq = projects.nextIssueSeq(project.id());
        String key = project.key() + "-" + seq;
        Issue issue = Issue.create(UUID.randomUUID(), key, seq, project.id(), cmd.type(), cmd.title(),
                cmd.description(), workflow.initialStatus().id(), cmd.priority(), cmd.reporterId(),
                cmd.parentId(), cmd.storyPoints());

        Issue saved = issues.save(issue);
        outbox.append(project.id(), issue.pullDomainEvents(), correlationId);
        return saved;
    }
}
