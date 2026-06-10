package org.example.application.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.EventOutboxPort;
import org.example.application.port.out.IssueRepositoryPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.SprintRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.config.security.CurrentUser;
import org.example.config.security.PermissionService;
import org.example.domain.issue.Issue;
import org.example.domain.project.Project;
import org.example.domain.project.Role;
import org.example.domain.shared.BusinessRuleException;
import org.example.domain.sprint.Sprint;
import org.example.domain.sprint.SprintState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveIssueToSprintService {

    private final IssueRepositoryPort issues;
    private final SprintRepositoryPort sprints;
    private final ProjectRepositoryPort projects;
    private final EventOutboxPort outbox;
    private final PermissionService permissions;

    /** Move an issue into a sprint, or to the backlog when {@code sprintId} is null. */
    @Transactional
    public Issue move(String issueKey, UUID sprintId, String correlationId) {
        Issue issue = issues.findByKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue " + issueKey + " not found"));
        Project project = projects.findById(issue.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project for issue " + issueKey + " not found"));
        CurrentUser.id().ifPresent(userId -> permissions.requireProjectRole(userId, project.key(), Role.MEMBER));

        if (sprintId != null) {
            Sprint sprint = sprints.findById(sprintId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint " + sprintId + " not found"));
            if (!sprint.projectId().equals(issue.projectId())) {
                throw new BusinessRuleException("Sprint belongs to a different project");
            }
            if (sprint.state() == SprintState.CLOSED) {
                throw new BusinessRuleException("Cannot move an issue into a closed sprint");
            }
        }

        issue.moveToSprint(sprintId);
        Issue saved = issues.save(issue);
        outbox.append(issue.projectId(), issue.pullDomainEvents(), correlationId);
        return saved;
    }
}
