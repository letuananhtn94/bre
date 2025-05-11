package com.loan.service.impl;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.domain.RuleExecutionLog;
import com.loan.model.RuleResult;
import com.loan.repository.RuleRepository;
import com.loan.repository.RuleExecutionLogRepository;
import com.loan.rule.BaseRule;
import com.loan.rule.RuleFactory;
import com.loan.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineServiceImpl implements RuleEngineService {

    private final RuleRepository ruleRepository;
    private final RuleExecutionLogRepository ruleExecutionLogRepository;
    private final RuleFactory ruleFactory;

    @Override
    @Transactional
    public RuleResult executeRule(Rule rule, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        RuleResult result = RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .build();

        try {
            // Create rule instance
            BaseRule ruleInstance = ruleFactory.createRule(rule);

            // Validate input
            if (!ruleInstance.validateInput(context)) {
                result.setStatus(ExecutionStatus.ERROR);
                result.setErrorMessage("Invalid input for rule: " + rule.getName());
                return result;
            }

            // Execute rule
            result = ruleInstance.execute(context);

            // Log execution
            logRuleExecution(rule, context, result, startTime);

        } catch (Exception e) {
            log.error("Error executing rule: {}", rule.getName(), e);
            result.setStatus(ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
            logRuleExecution(rule, context, result, startTime);
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private void logRuleExecution(Rule rule, Map<String, Object> context, RuleResult result, long startTime) {
        try {
            RuleExecutionLog log = RuleExecutionLog.builder()
                .rule(rule)
                .loanRequestId((String) context.get("requestId"))
                .productCode(rule.getProductCode())
                .workflowStep(rule.getWorkflowStep())
                .executionTime(LocalDateTime.now())
                .executionStatus(result.getStatus())
                .inputData(context.toString())
                .outputData(result.getResult() != null ? result.getResult().toString() : null)
                .errorMessage(result.getErrorMessage())
                .executionDurationMs(System.currentTimeMillis() - startTime)
                .build();

            ruleExecutionLogRepository.save(log);
        } catch (Exception e) {
            log.error("Error logging rule execution: {}", rule.getName(), e);
        }
    }

    @Override
    public Object convertOutput(Object result, String outputType) {
        // Simple passthrough for now, customize as needed
        return result;
    }

    @Override
    public boolean validateRuleInput(com.loan.domain.Rule rule, java.util.Map<String, Object> input) {
        // Simple validation: always return true for now
        return true;
    }

    @Override
    public Object evaluateScript(String script, java.util.Map<String, Object> context) {
        // Not implemented, return null or throw exception as needed
        return null;
    }
} 