package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import com.loan.service.CreditScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RuleClass(
    name = "CreditScoreCheck",
    description = "Checks if customer's credit score meets minimum requirements",
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
public class CreditScoreCheckRule extends BaseRule {

    private static final BigDecimal MIN_CREDIT_SCORE = new BigDecimal("600");
    private static final BigDecimal GOOD_CREDIT_SCORE = new BigDecimal("700");
    private static final BigDecimal EXCELLENT_CREDIT_SCORE = new BigDecimal("800");

    private final CreditScoreService creditScoreService;

    @Autowired
    public CreditScoreCheckRule(CreditScoreService creditScoreService) {
        super();
        this.creditScoreService = creditScoreService;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing credit score check for customer: {}", context.get("customerId"));

        String customerId = (String) context.get("customerId");
        if (customerId == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Customer ID is required")
                .build();
        }

        try {
            // Get credit score from service
            BigDecimal creditScore = creditScoreService.getCreditScore(customerId);
            
            if (creditScore == null) {
                return RuleResult.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .status(ExecutionStatus.ERROR)
                    .errorMessage("Could not retrieve credit score")
                    .build();
            }

            // Determine credit score category
            String scoreCategory;
            if (creditScore.compareTo(EXCELLENT_CREDIT_SCORE) >= 0) {
                scoreCategory = "EXCELLENT";
            } else if (creditScore.compareTo(GOOD_CREDIT_SCORE) >= 0) {
                scoreCategory = "GOOD";
            } else if (creditScore.compareTo(MIN_CREDIT_SCORE) >= 0) {
                scoreCategory = "FAIR";
            } else {
                scoreCategory = "POOR";
            }

            // Check if meets minimum requirement
            boolean meetsMinimum = creditScore.compareTo(MIN_CREDIT_SCORE) >= 0;

            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.SUCCESS)
                .result(Map.of(
                    "creditScore", creditScore,
                    "scoreCategory", scoreCategory,
                    "meetsMinimum", meetsMinimum,
                    "minimumRequired", MIN_CREDIT_SCORE
                ))
                .build();

        } catch (Exception e) {
            log.error("Error checking credit score for customer {}: {}", customerId, e.getMessage(), e);
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Error checking credit score: " + e.getMessage())
                .build();
        }
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for credit score check for customer: {}", context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "creditScore", BigDecimal.ZERO,
                "scoreCategory", "UNKNOWN",
                "meetsMinimum", false,
                "minimumRequired", MIN_CREDIT_SCORE,
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "CREDIT_SCORE_CHECK";
    }
} 