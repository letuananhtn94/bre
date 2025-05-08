package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class ScriptRule extends BaseRule {
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public ScriptRule(Rule rule) {
        super(rule);
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
                throw new IllegalArgumentException("Rule script is empty");
            }

            Expression expression = expressionParser.parseExpression(rule.getRuleScript());
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            context.forEach(evalContext::setVariable);
            
            Object ruleResult = expression.getValue(evalContext);
            result.setResult(ruleResult);

        } catch (Exception e) {
            log.error("Error executing script rule: {}", rule.getName(), e);
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
        // Add specific validation for script rules if needed
        return true;
    }

    @Override
    public String getRuleType() {
        return "SCRIPT";
    }
} 