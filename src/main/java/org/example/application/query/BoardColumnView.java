package org.example.application.query;

import java.util.List;
import java.util.UUID;

public record BoardColumnView(
        UUID statusId,
        String name,
        String category,
        int position,
        List<BoardCardView> cards) {
}
