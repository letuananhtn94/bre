package com.loan.service.impl;

import com.loan.domain.Workflow;
import com.loan.domain.WorkflowStep;
import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.LoanApprovalResult;
import com.loan.model.RuleResult;
import com.loan.repository.WorkflowRepository;
import com.loan.service.WorkflowService;
import com.loan.service.RuleEngineService;
import com.loan.service.ParallelRuleExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final RuleEngineService ruleEngineService;
    private final ParallelRuleExecutor parallelRuleExecutor;

    @Override
    @Transactional(readOnly = true)
    public LoanApprovalResult executeWorkflowStep(String productCode, String stepCode, Map<String, Object> context) {
        WorkflowStep step = getWorkflowStep(productCode, stepCode);
        if (step == null) {
            return LoanApprovalResult.builder()
                .requestId((String) context.get("requestId"))
                .productCode(productCode)
                .workflowStep(stepCode)
                .approved(false)
                .errorMessage("Workflow step not found")
                .timestamp(System.currentTimeMillis())
                .build();
        }

        // Execute rules in parallel if step is configured for parallel execution
        if (step.isParallelExecution()) {
            return executeRulesInParallel(step, context);
        }

        // Execute rules sequentially
        List<RuleResult> ruleResults = new ArrayList<>();
        boolean approved = true;

        for (Rule rule : step.getRules()) {
            if (!rule.isActive()) {
                continue;
            }

            RuleResult result = ruleEngineService.executeRule(rule, context);
            ruleResults.add(result);

            if (result.getStatus() == ExecutionStatus.ERROR) {
                approved = false;
                break;
            }

            // Add rule result to context for subsequent rules
            context.put(rule.getName(), result.getResult());
        }

        return LoanApprovalResult.builder()
            .requestId((String) context.get("requestId"))
            .productCode(productCode)
            .workflowStep(stepCode)
            .approved(approved)
            .ruleResults(ruleResults)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Workflow getWorkflow(String productCode) {
        return workflowRepository.findActiveWorkflowWithStepsAndRules(productCode)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found for product: " + productCode));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowStep getWorkflowStep(String productCode, String stepCode) {
        Workflow workflow = getWorkflow(productCode);
        return workflow.getSteps().stream()
            .filter(step -> step.getStepCode().equals(stepCode))
            .findFirst()
            .orElse(null);
    }

    private LoanApprovalResult executeRulesInParallel(WorkflowStep step, Map<String, Object> context) {
        log.info("Executing rules in parallel for step: {}", step.getStepCode());

        // Filter active rules
        List<Rule> activeRules = step.getRules().stream()
            .filter(Rule::isActive)
            .toList();

        if (activeRules.isEmpty()) {
            return LoanApprovalResult.builder()
                .requestId((String) context.get("requestId"))
                .productCode(step.getWorkflow().getProductCode())
                .workflowStep(step.getStepCode())
                .approved(true)
                .ruleResults(new ArrayList<>())
                .timestamp(System.currentTimeMillis())
                .build();
        }

        try {
            // Execute rules in parallel
            Map<String, RuleResult> results = parallelRuleExecutor.executeRules(activeRules, context);

            // Convert results to list and check for errors
            List<RuleResult> ruleResults = new ArrayList<>(results.values());
            boolean approved = ruleResults.stream()
                .noneMatch(result -> result.getStatus() == ExecutionStatus.ERROR);

            return LoanApprovalResult.builder()
                .requestId((String) context.get("requestId"))
                .productCode(step.getWorkflow().getProductCode())
                .workflowStep(step.getStepCode())
                .approved(approved)
                .ruleResults(ruleResults)
                .timestamp(System.currentTimeMillis())
                .build();

        } catch (Exception e) {
            log.error("Error executing rules in parallel for step: {}", step.getStepCode(), e);
            return LoanApprovalResult.builder()
                .requestId((String) context.get("requestId"))
                .productCode(step.getWorkflow().getProductCode())
                .workflowStep(step.getStepCode())
                .approved(false)
                .errorMessage("Error executing rules in parallel: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        }
    }

    public void validateWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }

        if (!StringUtils.hasText(workflow.getProductCode())) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one step");
        }

        // Validate step order
        for (int i = 0; i < workflow.getSteps().size(); i++) {
            WorkflowStep step = workflow.getSteps().get(i);
            if (step.getStepOrder() != i + 1) {
                throw new IllegalArgumentException("Invalid step order in workflow");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStepAutomated(String productCode, String stepCode) {
        WorkflowStep step = getWorkflowStep(productCode, stepCode);
        return step != null && step.isAutomated();
    }
} 