package org.example.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_transitions")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowTransitionEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "from_status_id")
    private UUID fromStatusId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "guard", columnDefinition = "jsonb")
    private Map<String, Object> guard;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "post_action", columnDefinition = "jsonb")
    private Map<String, Object> postAction;
}
