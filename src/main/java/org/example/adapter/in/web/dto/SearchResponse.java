package org.example.adapter.in.web.dto;

import java.util.List;
import org.example.application.query.IssueSummary;
import org.example.application.query.SearchPage;

public record SearchResponse(List<IssueSummary> items, PageInfo page) {

    public record PageInfo(int limit, boolean hasMore, String nextCursor) {
    }

    public static SearchResponse from(SearchPage page, int limit) {
        return new SearchResponse(page.items(), new PageInfo(limit, page.hasMore(), page.nextCursor()));
    }
}
