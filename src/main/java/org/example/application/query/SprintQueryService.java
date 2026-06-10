package org.example.application.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.port.out.SprintRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.config.security.CurrentUser;
import org.example.config.security.PermissionService;
import org.example.domain.project.Project;
import org.example.domain.project.Role;
import org.example.domain.sprint.Sprint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SprintQueryService {

    private final ProjectRepositoryPort projects;
    private final SprintRepositoryPort sprints;
    private final PermissionService permissions;

    @Transactional(readOnly = true)
    public List<Sprint> listByProject(String projectKey) {
        Project project = projects.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectKey + " not found"));
        CurrentUser.id().ifPresent(userId -> permissions.requireProjectRole(userId, projectKey, Role.VIEWER));
        return sprints.findByProject(project.id());
    }
}
