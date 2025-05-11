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
    name = "ComprehensiveLoanApproval",
    description = "Comprehensive rule for loan approval that combines multiple checks",
    timeout = 10000
)
@RuleExecution(
    maxRetries = 3,
    retryDelay = 1000,
    circuitBreakerThreshold = 5,
    circuitBreakerResetTimeout = 60000,
    fallbackEnabled = true,
    fallbackMethod = "fallbackCheck"
)
public class ComprehensiveLoanApprovalRule extends BaseRule {

    private static final BigDecimal MIN_CREDIT_SCORE = new BigDecimal("600");
    private static final BigDecimal MIN_INCOME_MULTIPLIER = new BigDecimal("0.3");
    private static final BigDecimal MAX_DEBT_TO_INCOME_RATIO = new BigDecimal("0.43");
    private static final BigDecimal MIN_DOWN_PAYMENT_PERCENTAGE = new BigDecimal("0.2");

    public ComprehensiveLoanApprovalRule() {
        super();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing comprehensive loan approval check for customer: {}", context.get("customerId"));

        // Extract input values
        BigDecimal creditScore = (BigDecimal) context.get("creditScore");
        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        BigDecimal monthlyDebtPayments = (BigDecimal) context.get("monthlyDebtPayments");
        Integer loanTermMonths = (Integer) context.get("loanTermMonths");
        BigDecimal downPayment = (BigDecimal) context.get("downPayment");
        String employmentStatus = (String) context.get("employmentStatus");

        // Validate required fields
        if (creditScore == null || monthlyIncome == null || loanAmount == null || 
            loanTermMonths == null || downPayment == null || employmentStatus == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Required loan approval data is missing")
                .build();
        }

        // Check credit score
        boolean meetsCreditScore = creditScore.compareTo(MIN_CREDIT_SCORE) >= 0;

        // Check income requirements
        BigDecimal monthlyLoanPayment = loanAmount
            .divide(new BigDecimal(loanTermMonths), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalMonthlyObligations = monthlyLoanPayment;
        if (monthlyDebtPayments != null) {
            totalMonthlyObligations = totalMonthlyObligations.add(monthlyDebtPayments);
        }
        BigDecimal debtToIncomeRatio = totalMonthlyObligations
            .divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
        boolean meetsIncomeRequirements = monthlyIncome
            .multiply(MIN_INCOME_MULTIPLIER)
            .compareTo(monthlyLoanPayment) >= 0;
        boolean meetsDebtToIncomeRatio = debtToIncomeRatio
            .compareTo(MAX_DEBT_TO_INCOME_RATIO) <= 0;

        // Check down payment
        BigDecimal downPaymentPercentage = downPayment
            .divide(loanAmount, 4, BigDecimal.ROUND_HALF_UP);
        boolean meetsDownPaymentRequirement = downPaymentPercentage
            .compareTo(MIN_DOWN_PAYMENT_PERCENTAGE) >= 0;

        // Check employment status
        boolean hasStableEmployment = "EMPLOYED".equals(employmentStatus) || 
            "SELF_EMPLOYED".equals(employmentStatus);

        // Final approval decision
        boolean approved = meetsCreditScore && 
            meetsIncomeRequirements && 
            meetsDebtToIncomeRatio && 
            meetsDownPaymentRequirement && 
            hasStableEmployment;

        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", approved,
                "meetsCreditScore", meetsCreditScore,
                "meetsIncomeRequirements", meetsIncomeRequirements,
                "meetsDebtToIncomeRatio", meetsDebtToIncomeRatio,
                "meetsDownPaymentRequirement", meetsDownPaymentRequirement,
                "hasStableEmployment", hasStableEmployment,
                "monthlyLoanPayment", monthlyLoanPayment,
                "totalMonthlyObligations", totalMonthlyObligations,
                "debtToIncomeRatio", debtToIncomeRatio,
                "downPaymentPercentage", downPaymentPercentage
            ))
            .build();
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId") 
            && context.containsKey("creditScore")
            && context.containsKey("monthlyIncome")
            && context.containsKey("loanAmount")
            && context.containsKey("loanTermMonths")
            && context.containsKey("downPayment")
            && context.containsKey("employmentStatus");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for comprehensive loan approval check for customer: {}", 
            context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", false,
                "meetsCreditScore", false,
                "meetsIncomeRequirements", false,
                "meetsDebtToIncomeRatio", false,
                "meetsDownPaymentRequirement", false,
                "hasStableEmployment", false,
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "COMPREHENSIVE_LOAN_APPROVAL";
    }
} 