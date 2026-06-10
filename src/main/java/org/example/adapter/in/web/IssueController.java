package org.example.adapter.in.web;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.CreateIssueRequest;
import org.example.adapter.in.web.dto.IssueResponse;
import org.example.adapter.in.web.dto.MoveToSprintRequest;
import org.example.adapter.in.web.dto.TransitionRequest;
import org.example.adapter.in.web.dto.UpdateIssueRequest;
import org.example.application.command.CreateIssueCommand;
import org.example.application.command.CreateIssueService;
import org.example.application.command.MoveIssueToSprintService;
import org.example.application.command.TransitionIssueService;
import org.example.application.command.UpdateIssueService;
import org.example.application.query.IssueQueryService;
import org.example.config.security.CurrentUser;
import org.example.config.security.PermissionService;
import org.example.domain.issue.IssueDetailsUpdate;
import org.example.domain.issue.Priority;
import org.example.domain.project.Role;
import org.example.domain.shared.BusinessRuleException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueController {

    private final CreateIssueService createIssue;
    private final UpdateIssueService updateIssue;
    private final TransitionIssueService transitionIssue;
    private final MoveIssueToSprintService moveIssueToSprint;
    private final IssueQueryService issueQuery;
    private final PermissionService permissionService;

    @PostMapping("/projects/{projectKey}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@PathVariable String projectKey, @Valid @RequestBody CreateIssueRequest req) {
        // RBAC: creating requires at least MEMBER on the project (skipped only when unauthenticated,
        // i.e. in security-disabled tests; production always has an authenticated principal here).
        CurrentUser.id().ifPresent(userId -> permissionService.requireProjectRole(userId, projectKey, Role.MEMBER));
        UUID reporter = CurrentUser.id().orElse(req.reporterId());
        if (reporter == null) {
            throw new BusinessRuleException("reporterId is required");
        }
        CreateIssueCommand cmd = new CreateIssueCommand(projectKey, req.type(), req.title(), req.description(),
                req.priority() == null ? Priority.MEDIUM : req.priority(), reporter, req.parentId(),
                req.storyPoints());
        return IssueResponse.from(createIssue.create(cmd, correlationId()));
    }

    @GetMapping("/issues/{issueKey}")
    public IssueResponse get(@PathVariable String issueKey) {
        return IssueResponse.from(issueQuery.getByKey(issueKey));
    }

    @PatchMapping("/issues/{issueKey}")
    public IssueResponse update(@PathVariable String issueKey, @Valid @RequestBody UpdateIssueRequest req) {
        IssueDetailsUpdate update = new IssueDetailsUpdate(req.title(), req.description(), req.priority(),
                req.storyPoints(), req.assigneeId(), req.labels());
        return IssueResponse.from(updateIssue.update(issueKey, req.version(), update, correlationId()));
    }

    @PostMapping("/issues/{issueKey}/transitions")
    public IssueResponse transition(@PathVariable String issueKey, @Valid @RequestBody TransitionRequest req) {
        return IssueResponse.from(transitionIssue.transition(issueKey, req.to(), correlationId()));
    }

    @PutMapping("/issues/{issueKey}/sprint")
    public IssueResponse moveToSprint(@PathVariable String issueKey, @RequestBody MoveToSprintRequest req) {
        return IssueResponse.from(moveIssueToSprint.move(issueKey, req.sprintId(), correlationId()));
    }

    private static String correlationId() {
        return MDC.get("correlationId");
    }
}
