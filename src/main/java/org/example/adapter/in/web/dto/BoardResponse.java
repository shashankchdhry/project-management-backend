package org.example.adapter.in.web.dto;

import java.util.List;
import java.util.UUID;
import org.example.application.query.BoardView;

public record BoardResponse(String projectKey, List<Column> columns) {

    public record Column(UUID statusId, String name, String category, int position, List<Card> cards) {
    }

    public record Card(UUID issueId, String issueKey, String type, String title, String priority,
                       UUID assigneeId, String assigneeName, Integer storyPoints, long version) {
    }

    public static BoardResponse from(BoardView view) {
        List<Column> columns = view.columns().stream()
                .map(c -> new Column(c.statusId(), c.name(), c.category(), c.position(),
                        c.cards().stream()
                                .map(card -> new Card(card.issueId(), card.issueKey(), card.type(), card.title(),
                                        card.priority(), card.assigneeId(), card.assigneeName(),
                                        card.storyPoints(), card.version()))
                                .toList()))
                .toList();
        return new BoardResponse(view.projectKey(), columns);
    }
}
