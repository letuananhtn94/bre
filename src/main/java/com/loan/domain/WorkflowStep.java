package com.loan.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@Entity
@Table(name = "workflow_steps")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_step_seq")
    @SequenceGenerator(name = "workflow_step_seq", sequenceName = "workflow_step_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "step_code", nullable = false)
    private String stepCode;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "is_automated", nullable = false)
    private boolean isAutomated;

    @Column(name = "cron_expression")
    private String cronExpression;

    @ManyToMany
    @JoinTable(
        name = "workflow_step_rules",
        joinColumns = @JoinColumn(name = "step_id"),
        inverseJoinColumns = @JoinColumn(name = "rule_id")
    )
    @OrderBy("executionOrder ASC")
    private List<Rule> rules;

    @Column(name = "description")
    private String description;
} 