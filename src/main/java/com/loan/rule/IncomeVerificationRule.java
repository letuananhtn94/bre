package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Component
@RuleClass(
    name = "IncomeVerification",
    description = "Verifies if customer's income meets loan requirements",
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
public class IncomeVerificationRule extends BaseRule {

    private static final BigDecimal MIN_INCOME_MULTIPLIER = new BigDecimal("0.3");
    private static final BigDecimal MAX_DEBT_TO_INCOME_RATIO = new BigDecimal("0.43");

    public IncomeVerificationRule() {
        super();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing income verification for customer: {}", context.get("customerId"));

        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        BigDecimal monthlyDebtPayments = (BigDecimal) context.get("monthlyDebtPayments");
        Integer loanTermMonths = (Integer) context.get("loanTermMonths");

        if (monthlyIncome == null || loanAmount == null || loanTermMonths == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Required income verification data is missing")
                .build();
        }

        // Calculate monthly loan payment
        BigDecimal monthlyLoanPayment = loanAmount
            .divide(new BigDecimal(loanTermMonths), 2, RoundingMode.HALF_UP);

        // Calculate total monthly obligations
        BigDecimal totalMonthlyObligations = monthlyLoanPayment;
        if (monthlyDebtPayments != null) {
            totalMonthlyObligations = totalMonthlyObligations.add(monthlyDebtPayments);
        }

        // Calculate debt-to-income ratio
        BigDecimal debtToIncomeRatio = totalMonthlyObligations
            .divide(monthlyIncome, 4, RoundingMode.HALF_UP);

        // Check minimum income requirement
        boolean meetsMinimumIncome = monthlyIncome
            .multiply(MIN_INCOME_MULTIPLIER)
            .compareTo(monthlyLoanPayment) >= 0;

        // Check debt-to-income ratio
        boolean meetsDebtToIncomeRatio = debtToIncomeRatio
            .compareTo(MAX_DEBT_TO_INCOME_RATIO) <= 0;

        boolean approved = meetsMinimumIncome && meetsDebtToIncomeRatio;

        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", approved,
                "meetsMinimumIncome", meetsMinimumIncome,
                "meetsDebtToIncomeRatio", meetsDebtToIncomeRatio,
                "monthlyLoanPayment", monthlyLoanPayment,
                "totalMonthlyObligations", totalMonthlyObligations,
                "debtToIncomeRatio", debtToIncomeRatio
            ))
            .build();
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId") 
            && context.containsKey("monthlyIncome")
            && context.containsKey("loanAmount")
            && context.containsKey("loanTermMonths");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for income verification for customer: {}", context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", true,
                "meetsMinimumIncome", true,
                "meetsDebtToIncomeRatio", true,
                "monthlyLoanPayment", BigDecimal.ZERO,
                "totalMonthlyObligations", BigDecimal.ZERO,
                "debtToIncomeRatio", BigDecimal.ZERO,
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "INCOME_VERIFICATION";
    }
} 