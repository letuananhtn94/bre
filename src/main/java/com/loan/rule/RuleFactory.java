package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleFactory {
    private final RuleRepository ruleRepository;
    private final JdbcTemplate jdbcTemplate;

    public BaseRule createRule(Rule rule) {
        try {
            // If rule class is specified, use reflection to create instance
            if (rule.getRuleClass() != null && !rule.getRuleClass().trim().isEmpty()) {
                return createRuleByClass(rule);
            }

            // Otherwise, create based on rule type
            return switch (rule.getRuleType()) {
                case SCRIPT -> new ScriptRule(rule);
                case API -> new ApiRule(rule);
                case DATABASE -> new DatabaseRule(rule, jdbcTemplate);
                case COMPOSITE -> new CompositeRule(rule, ruleRepository, this);
                default -> throw new IllegalArgumentException("Unsupported rule type: " + rule.getRuleType());
            };
        } catch (Exception e) {
            log.error("Error creating rule instance: {}", rule.getName(), e);
            throw new RuntimeException("Failed to create rule instance: " + e.getMessage());
        }
    }

    private BaseRule createRuleByClass(Rule rule) throws Exception {
        String ruleClass = rule.getRuleClass();
        Class<?> clazz = Class.forName(ruleClass);
        
        if (!BaseRule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Rule class must extend BaseRule");
        }

        // Handle special cases for rule types that need additional dependencies
        if (DatabaseRule.class.isAssignableFrom(clazz)) {
            return new DatabaseRule(rule, jdbcTemplate);
        } else if (CompositeRule.class.isAssignableFrom(clazz)) {
            return new CompositeRule(rule, ruleRepository, this);
        }

        // For other rule types, use default constructor
        return (BaseRule) clazz.getConstructor(Rule.class).newInstance(rule);
    }
} 