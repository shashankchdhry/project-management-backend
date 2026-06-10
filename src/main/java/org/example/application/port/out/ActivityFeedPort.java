package org.example.application.port.out;

import java.util.UUID;
import org.example.application.query.ActivityPage;

public interface ActivityFeedPort {

    /** Activity for a project, newest first, keyset-paginated. {@code eventType}/{@code issueId} are optional filters. */
    ActivityPage feed(UUID projectId, String eventType, UUID issueId, String cursor, int limit);
}
