package org.example.adapter.in.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.CompleteSprintRequest;
import org.example.adapter.in.web.dto.CreateSprintRequest;
import org.example.adapter.in.web.dto.SprintCompletionResponse;
import org.example.adapter.in.web.dto.SprintResponse;
import org.example.application.command.CompleteSprintCommand;
import org.example.application.command.CreateSprintService;
import org.example.application.command.SprintService;
import org.example.application.query.SprintQueryService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintController {

    private final CreateSprintService createSprint;
    private final SprintService sprintService;
    private final SprintQueryService sprintQuery;

    @GetMapping("/projects/{projectKey}/sprints")
    public List<SprintResponse> list(@PathVariable String projectKey) {
        return sprintQuery.listByProject(projectKey).stream().map(SprintResponse::from).toList();
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse create(@Valid @RequestBody CreateSprintRequest req) {
        return SprintResponse.from(createSprint.create(req.projectKey(), req.name(), req.goal()));
    }

    @PostMapping("/sprints/{sprintId}/start")
    public SprintResponse start(@PathVariable UUID sprintId) {
        return SprintResponse.from(sprintService.start(sprintId, correlationId()));
    }

    @PostMapping("/sprints/{sprintId}/complete")
    public SprintCompletionResponse complete(@PathVariable UUID sprintId, @RequestBody CompleteSprintRequest req) {
        CompleteSprintCommand cmd = new CompleteSprintCommand(req.carryOverIssueIds(), req.targetSprintId());
        return SprintCompletionResponse.from(sprintService.complete(sprintId, cmd, correlationId()));
    }

    private static String correlationId() {
        return MDC.get("correlationId");
    }
}
