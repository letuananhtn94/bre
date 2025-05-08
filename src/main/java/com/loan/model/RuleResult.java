package com.loan.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.loan.domain.ExecutionStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {
    private Long ruleId;
    private String ruleName;
    private ExecutionStatus status;
    private Object result;
    private String errorMessage;
    private Long executionTimeMs;
} 