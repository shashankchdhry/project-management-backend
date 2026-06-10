package org.example.adapter.in.web.dto;

import java.util.List;
import org.example.application.query.ReplayedEvent;

public record EventReplayResponse(String projectKey, long fromSeq, List<ReplayedEvent> events) {
}
