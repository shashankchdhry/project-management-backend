package org.example.application.query;

import java.util.UUID;

/** A denormalized issue card on the board read model. */
public record BoardCardView(
        UUID issueId,
        String issueKey,
        UUID statusId,
        String type,
        String title,
        String priority,
        UUID assigneeId,
        String assigneeName,
        Integer storyPoints,
        long version) {
}
