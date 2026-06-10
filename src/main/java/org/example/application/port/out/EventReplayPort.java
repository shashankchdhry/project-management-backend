package org.example.application.port.out;

import java.util.List;
import java.util.UUID;
import org.example.application.query.ReplayedEvent;

public interface EventReplayPort {

    List<ReplayedEvent> replay(UUID projectId, long afterSeq);
}
