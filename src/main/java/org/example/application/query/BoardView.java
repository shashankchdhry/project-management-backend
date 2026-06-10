package org.example.application.query;

import java.util.List;

public record BoardView(String projectKey, List<BoardColumnView> columns) {
}
