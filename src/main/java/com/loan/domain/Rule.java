package com.loan.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Entity
@Table(name = "rules")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "workflow_step")
    private String workflowStep;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "rule_class", nullable = false)
    private String ruleClass;

    @Column(name = "rule_script")
    private String ruleScript;

    @Column(name = "input_type")
    private String inputType;

    @Column(name = "output_type")
    private String outputType;

    @Column(name = "execution_order")
    private Integer executionOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "error_threshold")
    private Integer errorThreshold;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "retry_delay_ms")
    private Long retryDelayMs;

    @Column(name = "priority")
    private int priority;

    @Column(name = "parameters")
    private String parameters;
} 