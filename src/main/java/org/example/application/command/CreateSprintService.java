package org.example.application.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.SprintRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.project.Project;
import org.example.domain.sprint.Sprint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSprintService {

    private final ProjectRepositoryPort projects;
    private final SprintRepositoryPort sprints;

    @Transactional
    public Sprint create(String projectKey, String name, String goal) {
        Project project = projects.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectKey + " not found"));
        Sprint sprint = Sprint.create(UUID.randomUUID(), project.id(), name, goal, null, null);
        return sprints.save(sprint);
    }
}
