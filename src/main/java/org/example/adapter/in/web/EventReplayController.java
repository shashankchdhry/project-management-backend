package org.example.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.EventReplayResponse;
import org.example.application.query.ReplayQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Missed-event replay for reconnecting WebSocket clients. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventReplayController {

    private final ReplayQueryService replayQuery;

    @GetMapping("/projects/{projectKey}/events")
    public EventReplayResponse replay(@PathVariable String projectKey,
                                      @RequestParam(name = "after", defaultValue = "0") long after) {
        return new EventReplayResponse(projectKey, after, replayQuery.replay(projectKey, after));
    }
}
