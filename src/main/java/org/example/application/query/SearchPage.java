package org.example.application.query;

import java.util.List;

/**
 * One page of search results. {@code nextCursor} is an opaque token to fetch the next page
 * (null when there are no more); {@code hasMore} is computed by fetching limit+1 rows.
 */
public record SearchPage(List<IssueSummary> items, String nextCursor, boolean hasMore) {
}
