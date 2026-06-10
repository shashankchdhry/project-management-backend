package org.example.adapter.in.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.ActivityResponse;
import org.example.application.query.ActivityFeedService;
import org.example.application.query.ActivityPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Activity feed: a paginated, filterable event stream per project.
 * {@code GET /api/v1/projects/{key}/activity?type=StatusChanged&issueId=...&cursor=...&limit=20}
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityFeedService activityFeed;

    @GetMapping("/projects/{projectKey}/activity")
    public ActivityResponse activity(
            @PathVariable String projectKey,
            @RequestParam(name = "type", required = false) String eventType,
            @RequestParam(name = "issueId", required = false) UUID issueId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        ActivityPage page = activityFeed.feed(projectKey, eventType, issueId, cursor, capped);
        return ActivityResponse.from(page, capped);
    }
}
