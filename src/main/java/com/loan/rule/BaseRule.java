package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import java.util.Map;

public abstract class BaseRule {
    protected final Rule rule;

    protected BaseRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * Execute the rule logic
     * @param context The context containing input data
     * @return RuleResult containing the execution result
     */
    public abstract RuleResult execute(Map<String, Object> context);

    /**
     * Validate the input data
     * @param context The context containing input data
     * @return true if input is valid, false otherwise
     */
    public abstract boolean validateInput(Map<String, Object> context);

    /**
     * Get the rule type
     * @return The rule type
     */
    public abstract String getRuleType();

    /**
     * Get the rule instance
     * @return The rule instance
     */
    public Rule getRule() {
        return rule;
    }
} 