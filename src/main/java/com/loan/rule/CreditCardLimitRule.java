package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RuleClass(
    description = "Rule for calculating credit card limit based on income, credit score, employment type and payment history",
    productCode = "CREDIT_CARD"
)
public class CreditCardLimitRule extends BaseRule {
    
    private static final double BASE_INCOME_MULTIPLIER = 3.0;
    private static final double HIGH_CREDIT_SCORE_THRESHOLD = 750.0;
    private static final double MEDIUM_CREDIT_SCORE_THRESHOLD = 650.0;
    private static final double HIGH_CREDIT_SCORE_FACTOR = 1.5;
    private static final double MEDIUM_CREDIT_SCORE_FACTOR = 1.2;
    private static final double DEFAULT_CREDIT_SCORE_FACTOR = 1.0;
    private static final double PERMANENT_EMPLOYMENT_FACTOR = 1.3;
    private static final double DEFAULT_EMPLOYMENT_FACTOR = 1.0;
    private static final double NO_LATE_PAYMENT_FACTOR = 1.2;
    private static final double FEW_LATE_PAYMENTS_FACTOR = 1.0;
    private static final double MANY_LATE_PAYMENTS_FACTOR = 0.8;
    private static final int MAX_LATE_PAYMENTS = 2;

    @Override
    public RuleResult execute(Rule rule, Map<String, Object> input) {
        try {
            // Validate input
            validateInput(input, 
                "monthlyIncome", "creditScore", "employmentType", "latePayments");

            // Extract input values
            double monthlyIncome = ((Number) input.get("monthlyIncome")).doubleValue();
            double creditScore = ((Number) input.get("creditScore")).doubleValue();
            String employmentType = (String) input.get("employmentType");
            int latePayments = ((Number) input.get("latePayments")).intValue();

            // Calculate base limit
            double baseLimit = monthlyIncome * BASE_INCOME_MULTIPLIER;

            // Calculate credit score factor
            double creditScoreFactor = calculateCreditScoreFactor(creditScore);

            // Calculate employment factor
            double employmentFactor = calculateEmploymentFactor(employmentType);

            // Calculate history factor
            double historyFactor = calculateHistoryFactor(latePayments);

            // Calculate final limit
            double finalLimit = baseLimit * creditScoreFactor * employmentFactor * historyFactor;

            // Create result
            return RuleResult.builder()
                .status(RuleResult.Status.SUCCESS)
                .result(finalLimit)
                .build();

        } catch (Exception e) {
            log.error("Error executing CreditCardLimitRule: {}", e.getMessage(), e);
            return RuleResult.builder()
                .status(RuleResult.Status.ERROR)
                .errorMessage("Error calculating credit card limit: " + e.getMessage())
                .build();
        }
    }

    private double calculateCreditScoreFactor(double creditScore) {
        if (creditScore >= HIGH_CREDIT_SCORE_THRESHOLD) {
            return HIGH_CREDIT_SCORE_FACTOR;
        } else if (creditScore >= MEDIUM_CREDIT_SCORE_THRESHOLD) {
            return MEDIUM_CREDIT_SCORE_FACTOR;
        }
        return DEFAULT_CREDIT_SCORE_FACTOR;
    }

    private double calculateEmploymentFactor(String employmentType) {
        return "PERMANENT".equals(employmentType) ? 
            PERMANENT_EMPLOYMENT_FACTOR : DEFAULT_EMPLOYMENT_FACTOR;
    }

    private double calculateHistoryFactor(int latePayments) {
        if (latePayments == 0) {
            return NO_LATE_PAYMENT_FACTOR;
        } else if (latePayments <= MAX_LATE_PAYMENTS) {
            return FEW_LATE_PAYMENTS_FACTOR;
        }
        return MANY_LATE_PAYMENTS_FACTOR;
    }

    @Override
    public boolean supports(Rule rule) {
        return this.getClass().getSimpleName().equals(rule.getRuleClass());
    }
} 