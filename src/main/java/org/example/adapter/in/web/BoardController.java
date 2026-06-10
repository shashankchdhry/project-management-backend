package org.example.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.dto.BoardResponse;
import org.example.application.query.BoardQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BoardController {

    private final BoardQueryService boardQuery;

    @GetMapping("/projects/{projectKey}/board")
    public BoardResponse board(@PathVariable String projectKey) {
        return BoardResponse.from(boardQuery.getBoard(projectKey));
    }
}
