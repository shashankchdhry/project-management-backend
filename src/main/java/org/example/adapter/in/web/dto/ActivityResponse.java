package org.example.adapter.in.web.dto;

import java.util.List;
import org.example.application.query.ActivityEntry;
import org.example.application.query.ActivityPage;

public record ActivityResponse(List<ActivityEntry> items, PageInfo page) {

    public record PageInfo(int limit, boolean hasMore, String nextCursor) {
    }

    public static ActivityResponse from(ActivityPage page, int limit) {
        return new ActivityResponse(page.items(), new PageInfo(limit, page.hasMore(), page.nextCursor()));
    }
}
