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
    name = "CreditCardLimit",
    description = "Calculates credit card limit based on income, credit score, employment type and payment history",
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
public class CreditCardLimitRule extends BaseRule {
    
    private static final BigDecimal BASE_INCOME_MULTIPLIER = new BigDecimal("3.0");
    private static final BigDecimal HIGH_CREDIT_SCORE_THRESHOLD = new BigDecimal("750.0");
    private static final BigDecimal MEDIUM_CREDIT_SCORE_THRESHOLD = new BigDecimal("650.0");
    private static final BigDecimal HIGH_CREDIT_SCORE_FACTOR = new BigDecimal("1.5");
    private static final BigDecimal MEDIUM_CREDIT_SCORE_FACTOR = new BigDecimal("1.2");
    private static final BigDecimal DEFAULT_CREDIT_SCORE_FACTOR = new BigDecimal("1.0");
    private static final BigDecimal PERMANENT_EMPLOYMENT_FACTOR = new BigDecimal("1.3");
    private static final BigDecimal DEFAULT_EMPLOYMENT_FACTOR = new BigDecimal("1.0");
    private static final BigDecimal NO_LATE_PAYMENT_FACTOR = new BigDecimal("1.2");
    private static final BigDecimal FEW_LATE_PAYMENTS_FACTOR = new BigDecimal("1.0");
    private static final BigDecimal MANY_LATE_PAYMENTS_FACTOR = new BigDecimal("0.8");
    private static final int MAX_LATE_PAYMENTS = 2;

    public CreditCardLimitRule() {
        super();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing credit card limit calculation for customer: {}", context.get("customerId"));

        // Extract input values
        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");
        BigDecimal creditScore = (BigDecimal) context.get("creditScore");
        String employmentType = (String) context.get("employmentType");
        Integer latePayments = (Integer) context.get("latePayments");

        if (monthlyIncome == null || creditScore == null || employmentType == null || latePayments == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Required credit card limit calculation data is missing")
                .build();
        }

        // Calculate base limit
        BigDecimal baseLimit = monthlyIncome.multiply(BASE_INCOME_MULTIPLIER);

        // Calculate credit score factor
        BigDecimal creditScoreFactor = calculateCreditScoreFactor(creditScore);

        // Calculate employment factor
        BigDecimal employmentFactor = calculateEmploymentFactor(employmentType);

        // Calculate history factor
        BigDecimal historyFactor = calculateHistoryFactor(latePayments);

        // Calculate final limit
        BigDecimal finalLimit = baseLimit
            .multiply(creditScoreFactor)
            .multiply(employmentFactor)
            .multiply(historyFactor);

        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "monthlyIncome", monthlyIncome,
                "creditScore", creditScore,
                "employmentType", employmentType,
                "latePayments", latePayments,
                "baseLimit", baseLimit,
                "creditScoreFactor", creditScoreFactor,
                "employmentFactor", employmentFactor,
                "historyFactor", historyFactor,
                "finalLimit", finalLimit
            ))
            .build();
    }

    private BigDecimal calculateCreditScoreFactor(BigDecimal creditScore) {
        if (creditScore.compareTo(HIGH_CREDIT_SCORE_THRESHOLD) >= 0) {
            return HIGH_CREDIT_SCORE_FACTOR;
        } else if (creditScore.compareTo(MEDIUM_CREDIT_SCORE_THRESHOLD) >= 0) {
            return MEDIUM_CREDIT_SCORE_FACTOR;
        }
        return DEFAULT_CREDIT_SCORE_FACTOR;
    }

    private BigDecimal calculateEmploymentFactor(String employmentType) {
        return "PERMANENT".equals(employmentType) ? 
            PERMANENT_EMPLOYMENT_FACTOR : DEFAULT_EMPLOYMENT_FACTOR;
    }

    private BigDecimal calculateHistoryFactor(int latePayments) {
        if (latePayments == 0) {
            return NO_LATE_PAYMENT_FACTOR;
        } else if (latePayments <= MAX_LATE_PAYMENTS) {
            return FEW_LATE_PAYMENTS_FACTOR;
        }
        return MANY_LATE_PAYMENTS_FACTOR;
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId") 
            && context.containsKey("monthlyIncome")
            && context.containsKey("creditScore")
            && context.containsKey("employmentType")
            && context.containsKey("latePayments");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for credit card limit calculation for customer: {}", 
            context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "monthlyIncome", BigDecimal.ZERO,
                "creditScore", BigDecimal.ZERO,
                "employmentType", "UNKNOWN",
                "latePayments", 0,
                "baseLimit", BigDecimal.ZERO,
                "creditScoreFactor", DEFAULT_CREDIT_SCORE_FACTOR,
                "employmentFactor", DEFAULT_EMPLOYMENT_FACTOR,
                "historyFactor", DEFAULT_CREDIT_SCORE_FACTOR,
                "finalLimit", BigDecimal.ZERO,
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "CREDIT_CARD_LIMIT";
    }
} 