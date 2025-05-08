package com.loan.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalResult {
    private String requestId;
    private String productCode;
    private String workflowStep;
    private boolean approved;
    private Map<String, Object> resultData;
    private List<RuleResult> ruleResults;
    private String errorMessage;
    private Long timestamp;
} 