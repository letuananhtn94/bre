package com.loan.service;

import com.loan.domain.Rule;
import com.loan.model.LoanApprovalRequest;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LoanApprovalExample {

    @Autowired
    private ParallelRuleExecutor parallelRuleExecutor;

    public void processLoanApproval() {
        // Create sample loan request
        LoanApprovalRequest request = LoanApprovalRequest.builder()
            .customerId("CUST001")
            .customerName("John Doe")
            .loanAmount(new BigDecimal("50000"))
            .loanTermMonths(36)
            .monthlyIncome(new BigDecimal("5000"))
            .employmentStatus(LoanApprovalRequest.EmploymentStatus.EMPLOYED)
            .dateOfBirth(LocalDate.of(1985, 1, 1))
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

        // Create rules with dependencies
        List<Rule> rules = createRules();

        // Convert request to context
        Map<String, Object> context = convertRequestToContext(request);

        // Execute rules in parallel
        Map<String, RuleResult> results = parallelRuleExecutor.executeRules(rules, context);

        // Process results
        results.forEach((ruleName, result) -> {
            log.info("Rule: {}, Status: {}, Message: {}", 
                ruleName, 
                result.getStatus(),
                result.getErrorMessage());
        });
    }

    private List<Rule> createRules() {
        List<Rule> rules = new ArrayList<>();

        // Basic validation rules (no dependencies)
        rules.add(Rule.builder()
            .id(1L)
            .name("Basic Validation")
            .ruleClass("com.loan.rule.BasicValidationRule")
            .build());

        // Income check (depends on basic validation)
        rules.add(Rule.builder()
            .id(2L)
            .name("Income Check")
            .ruleClass("com.loan.rule.IncomeCheckRule")
            .build());

        // Credit score check (depends on basic validation)
        rules.add(Rule.builder()
            .id(3L)
            .name("Credit Score Check")
            .ruleClass("com.loan.rule.CreditScoreCheckRule")
            .build());

        // Employment check (depends on basic validation)
        rules.add(Rule.builder()
            .id(4L)
            .name("Employment Check")
            .ruleClass("com.loan.rule.EmploymentCheckRule")
            .build());

        // Debt-to-income check (depends on income check)
        rules.add(Rule.builder()
            .id(5L)
            .name("Debt-to-Income Check")
            .ruleClass("com.loan.rule.DebtToIncomeCheckRule")
            .build());

        // Loan amount check (depends on income check and credit score check)
        rules.add(Rule.builder()
            .id(6L)
            .name("Loan Amount Check")
            .ruleClass("com.loan.rule.LoanAmountCheckRule")
            .build());

        // Down payment check (depends on loan amount check)
        rules.add(Rule.builder()
            .id(7L)
            .name("Down Payment Check")
            .ruleClass("com.loan.rule.DownPaymentCheckRule")
            .build());

        // Final approval (depends on all other rules)
        rules.add(Rule.builder()
            .id(8L)
            .name("Final Approval")
            .ruleClass("com.loan.rule.FinalApprovalRule")
            .build());

        return rules;
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
} 