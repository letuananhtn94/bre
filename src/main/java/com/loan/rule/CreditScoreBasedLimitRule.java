package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RuleClass(
    name = "CreditScoreBasedLimit",
    description = "Calculates loan limit based on credit score and income",
    timeout = 5000
)
@RuleExecution(
    maxRetries = 3,
    retryDelay = 1000,
    circuitBreakerThreshold = 5,
    circuitBreakerResetTimeout = 60000,
    fallbackEnabled = true,
    fallbackMethod = "fallbackCheck"
)
public class CreditScoreBasedLimitRule extends BaseRule {

    private static final BigDecimal EXCELLENT_CREDIT_SCORE = new BigDecimal("800");
    private static final BigDecimal GOOD_CREDIT_SCORE = new BigDecimal("700");
    private static final BigDecimal FAIR_CREDIT_SCORE = new BigDecimal("600");

    private static final BigDecimal EXCELLENT_MULTIPLIER = new BigDecimal("2.0");
    private static final BigDecimal GOOD_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal FAIR_MULTIPLIER = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("0.5");

    public CreditScoreBasedLimitRule() {
        super();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing credit score based limit calculation for customer: {}", 
            context.get("customerId"));

        BigDecimal creditScore = (BigDecimal) context.get("creditScore");
        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");

        if (creditScore == null || monthlyIncome == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Required credit score or income data is missing")
                .build();
        }

        // Calculate base limit (12 months of income)
        BigDecimal baseLimit = monthlyIncome.multiply(new BigDecimal("12"));

        // Determine multiplier based on credit score
        BigDecimal multiplier;
        String scoreCategory;

        if (creditScore.compareTo(EXCELLENT_CREDIT_SCORE) >= 0) {
            multiplier = EXCELLENT_MULTIPLIER;
            scoreCategory = "EXCELLENT";
        } else if (creditScore.compareTo(GOOD_CREDIT_SCORE) >= 0) {
            multiplier = GOOD_MULTIPLIER;
            scoreCategory = "GOOD";
        } else if (creditScore.compareTo(FAIR_CREDIT_SCORE) >= 0) {
            multiplier = FAIR_MULTIPLIER;
            scoreCategory = "FAIR";
        } else {
            multiplier = DEFAULT_MULTIPLIER;
            scoreCategory = "POOR";
        }

        // Calculate final limit
        BigDecimal finalLimit = baseLimit.multiply(multiplier);

        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "creditScore", creditScore,
                "scoreCategory", scoreCategory,
                "monthlyIncome", monthlyIncome,
                "baseLimit", baseLimit,
                "multiplier", multiplier,
                "finalLimit", finalLimit
            ))
            .build();
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId") 
            && context.containsKey("creditScore")
            && context.containsKey("monthlyIncome");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for credit score based limit calculation for customer: {}", 
            context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "creditScore", BigDecimal.ZERO,
                "scoreCategory", "UNKNOWN",
                "monthlyIncome", BigDecimal.ZERO,
                "baseLimit", BigDecimal.ZERO,
                "multiplier", DEFAULT_MULTIPLIER,
                "finalLimit", BigDecimal.ZERO,
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "CREDIT_SCORE_BASED_LIMIT";
    }
} 