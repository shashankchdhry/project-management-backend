package org.example.adapter.in.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.SearchResponse;
import org.example.application.query.IssueSearchCriteria;
import org.example.application.query.IssueSearchService;
import org.example.application.query.SearchPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search & filtering. Full-text {@code q} + structured filters, cursor-paginated and
 * scoped to the caller's projects.
 *
 * <p>Example: {@code GET /api/v1/search?q=oauth&status=In Progress&assignee=<uuid>&limit=20}
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final IssueSearchService searchService;

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(required = false) String projectKey,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID sprint,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        SearchPage page = searchService.search(
                new IssueSearchCriteria(projectKey, q, status, assignee, type, priority, sprint, cursor, capped));
        return SearchResponse.from(page, capped);
    }
}
