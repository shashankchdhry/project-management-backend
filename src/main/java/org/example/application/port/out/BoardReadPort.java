package org.example.application.port.out;

import java.util.List;
import java.util.UUID;
import org.example.application.query.BoardCardView;

/** Read side of the board CQRS projection (issue_board_view). */
public interface BoardReadPort {

    List<BoardCardView> findByProject(UUID projectId);
}
