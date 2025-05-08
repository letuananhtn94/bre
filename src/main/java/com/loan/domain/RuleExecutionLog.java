package com.loan.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rule_execution_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_execution_log_seq")
    @SequenceGenerator(name = "rule_execution_log_seq", sequenceName = "rule_execution_log_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Column(name = "loan_request_id", nullable = false)
    private String loanRequestId;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "workflow_step", nullable = false)
    private String workflowStep;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "execution_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus executionStatus;

    @Column(name = "input_data", columnDefinition = "CLOB")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "CLOB")
    private String outputData;

    @Column(name = "error_message", columnDefinition = "CLOB")
    private String errorMessage;

    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;
} 