package org.example.application.port.out;

import java.util.List;
import java.util.UUID;
import org.example.application.query.IssueSearchCriteria;
import org.example.application.query.SearchPage;

public interface IssueSearchPort {

    /**
     * @param allowedProjectIds row-level scoping — restrict to these projects; {@code null} means
     *                          no scoping (e.g. security disabled in tests)
     */
    SearchPage search(IssueSearchCriteria criteria, List<UUID> allowedProjectIds);
}
