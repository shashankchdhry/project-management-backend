package org.example.application.query;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.BoardReadPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.WorkflowRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.project.Project;
import org.example.domain.workflow.Workflow;
import org.example.domain.workflow.WorkflowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardQueryService {

    private final ProjectRepositoryPort projects;
    private final WorkflowRepositoryPort workflows;
    private final BoardReadPort boardRead;

    @Transactional(readOnly = true)
    public BoardView getBoard(String projectKey) {
        Project project = projects.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectKey + " not found"));
        Workflow workflow = workflows.load(project.workflowId());

        Map<UUID, List<BoardCardView>> cardsByStatus = boardRead.findByProject(project.id()).stream()
                .collect(Collectors.groupingBy(BoardCardView::statusId));

        List<BoardColumnView> columns = workflow.statuses().stream()
                .sorted(Comparator.comparingInt(WorkflowStatus::position))
                .map(s -> new BoardColumnView(s.id(), s.name(), s.category().name(), s.position(),
                        cardsByStatus.getOrDefault(s.id(), List.of())))
                .toList();

        return new BoardView(projectKey, columns);
    }
}
