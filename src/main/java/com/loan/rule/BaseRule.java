package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public abstract class BaseRule {
    protected Rule rule;
    
    @Autowired
    protected ApplicationContext applicationContext;

    public BaseRule() {}

    public BaseRule(Rule rule) {
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

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    protected <T> T getServiceInstance(Class<T> serviceClass) {
        try {
            return applicationContext.getBean(serviceClass);
        } catch (Exception e) {
            log.error("Error getting service instance for class: {}", serviceClass.getName(), e);
            throw new RuntimeException("Failed to get service instance: " + e.getMessage());
        }
    }

    protected <T> T getServiceInstance(String serviceClassName) {
        try {
            Class<?> serviceClass = Class.forName(serviceClassName);
            return (T) applicationContext.getBean(serviceClass);
        } catch (Exception e) {
            log.error("Error getting service instance for class name: {}", serviceClassName, e);
            throw new RuntimeException("Failed to get service instance: " + e.getMessage());
        }
    }

    protected boolean supports(Rule rule) {
        return this.getClass().getName().equals(rule.getRuleClass());
    }
} 