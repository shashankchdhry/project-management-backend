package org.example.application.query;

import java.util.List;

public record ActivityPage(List<ActivityEntry> items, String nextCursor, boolean hasMore) {
}
