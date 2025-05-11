package com.loan.service;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.LoanApprovalRequest;
import com.loan.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.HashMap;

@Slf4j
@Service
public class ParallelRuleExample {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, Integer> failureCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILURES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    public List<RuleResult> executeParallelRules(LoanApprovalRequest request, List<Rule> rules) {
        // Convert request to context map
        Map<String, Object> context = convertRequestToContext(request);

        // Create parallel tasks for each rule with circuit breaker and retry
        List<CompletableFuture<RuleResult>> futures = rules.stream()
            .map(rule -> CompletableFuture.supplyAsync(() -> {
                try {
                    return executeRuleWithRetry(rule, context);
                } catch (Exception e) {
                    log.error("Error executing rule: {}", rule.getName(), e);
                    return RuleResult.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .status(ExecutionStatus.ERROR)
                        .errorMessage(e.getMessage())
                        .build();
                }
            }, executorService))
            .collect(Collectors.toList());

        // Wait for all rules to complete and collect results
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    private RuleResult executeRuleWithRetry(Rule rule, Map<String, Object> context) {
        int attempts = 0;
        while (attempts < MAX_FAILURES) {
            try {
                if (isCircuitBreakerOpen(rule.getName())) {
                    return createFallbackResult(rule);
                }

                RuleResult result = executeRule(rule, context);
                
                if (result.getStatus() == ExecutionStatus.SUCCESS) {
                    resetFailureCount(rule.getName());
                    return result;
                }

                incrementFailureCount(rule.getName());
                attempts++;
                
                if (attempts < MAX_FAILURES) {
                    Thread.sleep(RETRY_DELAY.toMillis());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return createErrorResult(rule, "Rule execution interrupted");
            } catch (Exception e) {
                incrementFailureCount(rule.getName());
                attempts++;
                if (attempts >= MAX_FAILURES) {
                    return createErrorResult(rule, e.getMessage());
                }
            }
        }
        return createErrorResult(rule, "Max retry attempts reached");
    }

    private boolean isCircuitBreakerOpen(String ruleName) {
        return failureCount.getOrDefault(ruleName, 0) >= MAX_FAILURES;
    }

    private void incrementFailureCount(String ruleName) {
        failureCount.merge(ruleName, 1, Integer::sum);
    }

    private void resetFailureCount(String ruleName) {
        failureCount.remove(ruleName);
    }

    private RuleResult createFallbackResult(Rule rule) {
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.ERROR)
            .errorMessage("Circuit breaker is open")
            .build();
    }

    private RuleResult createErrorResult(Rule rule, String errorMessage) {
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.ERROR)
            .errorMessage(errorMessage)
            .build();
    }

    private Map<String, Object> convertRequestToContext(LoanApprovalRequest request) {
        Map<String, Object> context = new HashMap<>();
        context.put("customerId", request.getCustomerId());
        context.put("customerName", request.getCustomerName());
        context.put("loanAmount", request.getLoanAmount());
        context.put("loanTermMonths", request.getLoanTermMonths());
        context.put("monthlyIncome", request.getMonthlyIncome());
        context.put("employmentStatus", request.getEmploymentStatus());
        context.put("dateOfBirth", request.getDateOfBirth());
        context.put("nationalId", request.getNationalId());
        context.put("email", request.getEmail());
        context.put("phoneNumber", request.getPhoneNumber());
        context.put("address", request.getAddress());
        context.put("city", request.getCity());
        context.put("country", request.getCountry());
        context.put("hasExistingLoan", request.isHasExistingLoan());
        context.put("existingLoanAmount", request.getExistingLoanAmount());
        context.put("creditScore", request.getCreditScore());
        context.put("yearsOfEmployment", request.getYearsOfEmployment());
        context.put("monthlyExpenses", request.getMonthlyExpenses());
        context.put("monthlyDebtPayments", request.getMonthlyDebtPayments());
        context.put("downPayment", request.getDownPayment());
        context.put("loanPurpose", request.getLoanPurpose());
        context.put("loanType", request.getLoanType());
        context.put("applicationDate", request.getApplicationDate());
        context.put("applicationSource", request.getApplicationSource());
        context.put("isFirstTimeBorrower", request.isFirstTimeBorrower());
        context.put("numberOfDependents", request.getNumberOfDependents());
        return context;
    }

    private RuleResult executeRule(Rule rule, Map<String, Object> context) {
        RuleResult result = RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .build();

        try {
            String ruleType = rule.getRuleType() != null ? rule.getRuleType().toString() : null;
            if ("INCOME_CHECK".equals(ruleType)) {
                return executeIncomeCheckRule(context);
            } else if ("CREDIT_SCORE_CHECK".equals(ruleType)) {
                return executeCreditScoreCheckRule(context);
            } else if ("EMPLOYMENT_CHECK".equals(ruleType)) {
                return executeEmploymentCheckRule(context);
            } else if ("DEBT_TO_INCOME_CHECK".equals(ruleType)) {
                return executeDebtToIncomeCheckRule(context);
            } else if ("LOAN_AMOUNT_CHECK".equals(ruleType)) {
                return executeLoanAmountCheckRule(context);
            } else if ("DOWN_PAYMENT_CHECK".equals(ruleType)) {
                return executeDownPaymentCheckRule(context);
            } else {
                result.setStatus(ExecutionStatus.ERROR);
                result.setErrorMessage("Unknown rule type: " + ruleType);
            }
        } catch (Exception e) {
            result.setStatus(ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private RuleResult executeIncomeCheckRule(Map<String, Object> context) {
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");
        
        if (loanAmount.compareTo(monthlyIncome.multiply(BigDecimal.valueOf(12))) > 0) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Loan amount exceeds annual income")
                .build();
        }
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    private RuleResult executeCreditScoreCheckRule(Map<String, Object> context) {
        Integer creditScore = (Integer) context.get("creditScore");
        if (creditScore < 600) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Credit score too low")
                .build();
        }
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    private RuleResult executeEmploymentCheckRule(Map<String, Object> context) {
        String employmentStatus = context.get("employmentStatus").toString();
        Integer yearsOfEmployment = (Integer) context.get("yearsOfEmployment");
        
        if ("UNEMPLOYED".equals(employmentStatus)) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Unemployed applicants not eligible")
                .build();
        }
        
        if (yearsOfEmployment < 2) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Insufficient employment history")
                .build();
        }
        
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    private RuleResult executeDebtToIncomeCheckRule(Map<String, Object> context) {
        BigDecimal monthlyIncome = (BigDecimal) context.get("monthlyIncome");
        BigDecimal monthlyDebtPayments = (BigDecimal) context.get("monthlyDebtPayments");
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        Integer loanTermMonths = (Integer) context.get("loanTermMonths");
        
        BigDecimal monthlyLoanPayment = loanAmount.divide(new BigDecimal(loanTermMonths), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalMonthlyPayments = monthlyDebtPayments.add(monthlyLoanPayment);
        BigDecimal debtToIncomeRatio = totalMonthlyPayments.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
        
        if (debtToIncomeRatio.compareTo(new BigDecimal("43")) > 0) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Debt-to-income ratio too high: " + debtToIncomeRatio + "%")
                .build();
        }
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    private RuleResult executeLoanAmountCheckRule(Map<String, Object> context) {
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        String loanType = context.get("loanType").toString();
        
        if ("SECURED".equals(loanType) && loanAmount.compareTo(new BigDecimal("500000")) > 0) {
            return RuleResult.builder()
                .status(ExecutionStatus.ERROR)
                .errorMessage("Secured loan amount exceeds maximum limit")
                .build();
        }
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    private RuleResult executeDownPaymentCheckRule(Map<String, Object> context) {
        BigDecimal loanAmount = (BigDecimal) context.get("loanAmount");
        BigDecimal downPayment = (BigDecimal) context.get("downPayment");
        String loanPurpose = context.get("loanPurpose").toString();
        
        if ("HOME".equals(loanPurpose) || "VEHICLE".equals(loanPurpose)) {
            BigDecimal requiredDownPayment = loanAmount.multiply(new BigDecimal("0.20"));
            if (downPayment.compareTo(requiredDownPayment) < 0) {
                return RuleResult.builder()
                    .status(ExecutionStatus.ERROR)
                    .errorMessage("Insufficient down payment for " + loanPurpose.toLowerCase() + " loan")
                    .build();
            }
        }
        return RuleResult.builder().status(ExecutionStatus.SUCCESS).build();
    }

    // Example usage
    public void example() {
        // Create sample loan request
        LoanApprovalRequest request = LoanApprovalRequest.builder()
            .customerId("CUST001")
            .customerName("John Doe")
            .loanAmount(new BigDecimal("50000"))
            .loanTermMonths(36)
            .monthlyIncome(new BigDecimal("5000"))
            .employmentStatus(LoanApprovalRequest.EmploymentStatus.EMPLOYED)
            .dateOfBirth(java.time.LocalDate.of(1985, 1, 1))
            .nationalId("123456789")
            .email("john.doe@example.com")
            .phoneNumber("+1234567890")
            .creditScore(650)
            .yearsOfEmployment(5)
            .monthlyExpenses(new BigDecimal("2000"))
            .monthlyDebtPayments(new BigDecimal("500"))
            .downPayment(new BigDecimal("10000"))
            .loanPurpose(LoanApprovalRequest.LoanPurpose.HOME)
            .loanType(LoanApprovalRequest.LoanType.SECURED)
            .build();

        // Create sample rules
        List<Rule> rules = new ArrayList<>();
        rules.add(Rule.builder().id(1L).name("Income Check").ruleType("INCOME_CHECK").build());
        rules.add(Rule.builder().id(2L).name("Credit Score Check").ruleType("CREDIT_SCORE_CHECK").build());
        rules.add(Rule.builder().id(3L).name("Employment Check").ruleType("EMPLOYMENT_CHECK").build());
        rules.add(Rule.builder().id(4L).name("Debt-to-Income Check").ruleType("DEBT_TO_INCOME_CHECK").build());
        rules.add(Rule.builder().id(5L).name("Loan Amount Check").ruleType("LOAN_AMOUNT_CHECK").build());
        rules.add(Rule.builder().id(6L).name("Down Payment Check").ruleType("DOWN_PAYMENT_CHECK").build());

        // Execute rules in parallel
        List<RuleResult> results = executeParallelRules(request, rules);

        // Process results
        results.forEach(result -> {
            log.info("Rule: {}, Status: {}, Message: {}", 
                result.getRuleName(), 
                result.getStatus(),
                result.getErrorMessage());
        });
    }
} 