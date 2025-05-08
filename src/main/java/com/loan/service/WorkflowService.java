package com.loan.service;

import com.loan.domain.Workflow;
import com.loan.domain.WorkflowStep;
import com.loan.model.LoanApprovalResult;
import java.util.Map;

public interface WorkflowService {
    LoanApprovalResult executeWorkflowStep(String productCode, String stepCode, Map<String, Object> context);
    Workflow getWorkflow(String productCode);
    WorkflowStep getWorkflowStep(String productCode, String stepCode);
    void validateWorkflow(Workflow workflow);
    boolean isStepAutomated(String productCode, String stepCode);
} 