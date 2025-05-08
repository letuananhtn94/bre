package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import com.loan.repository.RuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CompositeRule extends BaseRule {
    private final RuleRepository ruleRepository;
    private final RuleFactory ruleFactory;

    public CompositeRule(Rule rule, RuleRepository ruleRepository, RuleFactory ruleFactory) {
        super(rule);
        this.ruleRepository = ruleRepository;
        this.ruleFactory = ruleFactory;
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        RuleResult result = RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .build();

        try {
            if (!StringUtils.hasText(rule.getRuleScript())) {
                throw new IllegalArgumentException("Composite rule configuration is not specified");
            }

            // Parse composite rule configuration
            List<Long> ruleIds = parseRuleIds(rule.getRuleScript());
            List<RuleResult> subResults = new ArrayList<>();

            // Execute each sub-rule
            for (Long ruleId : ruleIds) {
                Rule subRule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("Sub-rule not found: " + ruleId));

                BaseRule ruleInstance = ruleFactory.createRule(subRule);
                RuleResult subResult = ruleInstance.execute(context);
                subResults.add(subResult);

                // Check if we should stop execution based on sub-rule result
                if (shouldStopExecution(subResult)) {
                    result.setStatus(ExecutionStatus.ERROR);
                    result.setErrorMessage("Composite rule execution stopped due to sub-rule failure");
                    result.setResult(subResults);
                    break;
                }
            }

            if (result.getStatus() != ExecutionStatus.ERROR) {
                result.setResult(subResults);
            }

        } catch (Exception e) {
            log.error("Error executing composite rule: {}", rule.getName(), e);
            result.setStatus(ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        if (context == null) {
            return false;
        }
        // Validate composite rule configuration
        if (!StringUtils.hasText(rule.getRuleScript())) {
            return false;
        }
        try {
            List<Long> ruleIds = parseRuleIds(rule.getRuleScript());
            return !ruleIds.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getRuleType() {
        return "COMPOSITE";
    }

    private List<Long> parseRuleIds(String configuration) {
        // Expected format: "1,2,3" or "1;2;3"
        String[] parts = configuration.split("[,;]");
        List<Long> ruleIds = new ArrayList<>();
        for (String part : parts) {
            try {
                ruleIds.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid rule ID in composite rule configuration: {}", part);
            }
        }
        return ruleIds;
    }

    private boolean shouldStopExecution(RuleResult subResult) {
        // Stop execution if sub-rule failed
        return subResult.getStatus() == ExecutionStatus.ERROR;
    }
} 