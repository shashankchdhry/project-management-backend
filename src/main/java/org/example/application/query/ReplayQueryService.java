package org.example.application.query;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.EventReplayPort;
import org.example.application.port.out.ProjectRepositoryPort;
import org.example.application.shared.ResourceNotFoundException;
import org.example.domain.project.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplayQueryService {

    private final ProjectRepositoryPort projects;
    private final EventReplayPort replayPort;

    @Transactional(readOnly = true)
    public List<ReplayedEvent> replay(String projectKey, long afterSeq) {
        Project project = projects.findByKey(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectKey + " not found"));
        return replayPort.replay(project.id(), afterSeq);
    }
}
