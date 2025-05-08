package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RuleClass(
    description = "Comprehensive loan approval rule that considers multiple factors",
    productCode = "PERSONAL_LOAN",
    dependsOn = {
        "CreditScoreCheck",
        "IncomeVerificationRule",
        "EmploymentVerificationRule",
        "DebtToIncomeRule",
        "BlacklistCheckRule"
    },
    timeout = 5000
)
@RuleExecution(
    maxRetries = 3,
    retryDelay = 2000,
    circuitBreakerThreshold = 5,
    circuitBreakerResetTimeout = 300000,
    fallbackEnabled = true,
    fallbackMethod = "fallbackApproval"
)
public class ComprehensiveLoanApprovalRule extends BaseRule {

    private static final double MIN_CREDIT_SCORE = 650.0;
    private static final double MIN_INCOME = 5000000.0; // 5M VND
    private static final double MAX_DTI_RATIO = 0.5; // 50%
    private static final int MIN_EMPLOYMENT_MONTHS = 6;

    @Override
    public RuleResult execute(Rule rule, Map<String, Object> input) {
        try {
            // Validate input dependencies
            validateInput(input, 
                "CreditScoreCheck",
                "IncomeVerificationRule",
                "EmploymentVerificationRule",
                "DebtToIncomeRule",
                "BlacklistCheckRule"
            );

            // Extract and validate credit score
            Map<String, Object> creditScoreResult = (Map<String, Object>) input.get("CreditScoreCheck");
            double creditScore = ((Number) creditScoreResult.get("score")).doubleValue();
            if (creditScore < MIN_CREDIT_SCORE) {
                return createRejectionResult("Insufficient credit score");
            }

            // Extract and validate income
            Map<String, Object> incomeResult = (Map<String, Object>) input.get("IncomeVerificationRule");
            double monthlyIncome = ((Number) incomeResult.get("verifiedIncome")).doubleValue();
            if (monthlyIncome < MIN_INCOME) {
                return createRejectionResult("Insufficient income");
            }

            // Extract and validate employment
            Map<String, Object> employmentResult = (Map<String, Object>) input.get("EmploymentVerificationRule");
            int employmentMonths = ((Number) employmentResult.get("employmentMonths")).intValue();
            if (employmentMonths < MIN_EMPLOYMENT_MONTHS) {
                return createRejectionResult("Insufficient employment history");
            }

            // Extract and validate DTI ratio
            Map<String, Object> dtiResult = (Map<String, Object>) input.get("DebtToIncomeRule");
            double dtiRatio = ((Number) dtiResult.get("dtiRatio")).doubleValue();
            if (dtiRatio > MAX_DTI_RATIO) {
                return createRejectionResult("High debt-to-income ratio");
            }

            // Check blacklist status
            Map<String, Object> blacklistResult = (Map<String, Object>) input.get("BlacklistCheckRule");
            boolean isBlacklisted = (boolean) blacklistResult.get("isBlacklisted");
            if (isBlacklisted) {
                return createRejectionResult("Customer is blacklisted");
            }

            // Calculate loan amount based on income and credit score
            double loanAmount = calculateLoanAmount(monthlyIncome, creditScore);

            // Create approval result
            return RuleResult.builder()
                .status(RuleResult.Status.SUCCESS)
                .result(Map.of(
                    "approved", true,
                    "loanAmount", loanAmount,
                    "interestRate", calculateInterestRate(creditScore),
                    "term", calculateLoanTerm(creditScore),
                    "reason", "All criteria met"
                ))
                .build();

        } catch (Exception e) {
            log.error("Error executing ComprehensiveLoanApprovalRule: {}", e.getMessage(), e);
            return RuleResult.builder()
                .status(RuleResult.Status.ERROR)
                .errorMessage("Error in comprehensive approval: " + e.getMessage())
                .build();
        }
    }

    private RuleResult createRejectionResult(String reason) {
        return RuleResult.builder()
            .status(RuleResult.Status.SUCCESS)
            .result(Map.of(
                "approved", false,
                "reason", reason
            ))
            .build();
    }

    private double calculateLoanAmount(double monthlyIncome, double creditScore) {
        double baseAmount = monthlyIncome * 12; // 12 months of income
        double multiplier = 1.0;

        if (creditScore >= 800) {
            multiplier = 2.0;
        } else if (creditScore >= 700) {
            multiplier = 1.5;
        }

        return baseAmount * multiplier;
    }

    private double calculateInterestRate(double creditScore) {
        if (creditScore >= 800) {
            return 0.08; // 8%
        } else if (creditScore >= 700) {
            return 0.10; // 10%
        }
        return 0.12; // 12%
    }

    private int calculateLoanTerm(double creditScore) {
        if (creditScore >= 800) {
            return 36; // 36 months
        } else if (creditScore >= 700) {
            return 24; // 24 months
        }
        return 12; // 12 months
    }

    public RuleResult fallbackApproval(Rule rule, Map<String, Object> input) {
        log.warn("Using fallback approval method for rule: {}", rule.getName());
        
        // Fallback logic - simplified approval based on basic criteria
        try {
            Map<String, Object> creditScoreResult = (Map<String, Object>) input.get("CreditScoreCheck");
            double creditScore = ((Number) creditScoreResult.get("score")).doubleValue();
            
            Map<String, Object> incomeResult = (Map<String, Object>) input.get("IncomeVerificationRule");
            double monthlyIncome = ((Number) incomeResult.get("verifiedIncome")).doubleValue();

            if (creditScore >= 700 && monthlyIncome >= MIN_INCOME) {
                return RuleResult.builder()
                    .status(RuleResult.Status.SUCCESS)
                    .result(Map.of(
                        "approved", true,
                        "loanAmount", monthlyIncome * 12,
                        "interestRate", 0.12,
                        "term", 12,
                        "reason", "Fallback approval based on basic criteria"
                    ))
                    .build();
            }

            return createRejectionResult("Fallback rejection - basic criteria not met");
        } catch (Exception e) {
            return RuleResult.builder()
                .status(RuleResult.Status.ERROR)
                .errorMessage("Error in fallback approval: " + e.getMessage())
                .build();
        }
    }

    @Override
    public boolean supports(Rule rule) {
        return this.getClass().getSimpleName().equals(rule.getRuleClass());
    }
} 