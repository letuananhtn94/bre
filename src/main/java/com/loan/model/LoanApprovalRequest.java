package com.loan.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanApprovalRequest {
    private String requestId;
    private String productCode;
    private String workflowStep;
    private Map<String, Object> data;

    // Customer Information
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String address;
    private String city;
    private String country;

    // Loan Information
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.0", message = "Loan amount must be at least 1,000")
    @DecimalMax(value = "1000000.0", message = "Loan amount cannot exceed 1,000,000")
    private BigDecimal loanAmount;

    @NotNull(message = "Loan term is required")
    @Min(value = 1, message = "Loan term must be at least 1 month")
    @Max(value = 360, message = "Loan term cannot exceed 360 months")
    private Integer loanTermMonths;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", message = "Monthly income cannot be negative")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    @Builder.Default
    private Integer yearsOfEmployment = 0;

    @Builder.Default
    private BigDecimal monthlyExpenses = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal monthlyDebtPayments = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal downPayment = BigDecimal.ZERO;

    @Builder.Default
    private LoanPurpose loanPurpose = LoanPurpose.PERSONAL;

    @Builder.Default
    private LoanType loanType = LoanType.UNSECURED;

    @Builder.Default
    private LocalDateTime applicationDate = LocalDateTime.now();

    @Builder.Default
    private String applicationSource = "ONLINE";

    @Builder.Default
    private boolean isFirstTimeBorrower = true;

    @Builder.Default
    private Integer numberOfDependents = 0;

    // Credit Information
    @Builder.Default
    private Integer creditScore = 0;

    @Builder.Default
    private boolean hasExistingLoan = false;

    @Builder.Default
    private BigDecimal existingLoanAmount = BigDecimal.ZERO;

    public enum EmploymentStatus {
        EMPLOYED,
        SELF_EMPLOYED,
        UNEMPLOYED,
        RETIRED
    }

    public enum LoanPurpose {
        HOME,
        CAR,
        EDUCATION,
        BUSINESS,
        PERSONAL,
        DEBT_CONSOLIDATION
    }

    public enum LoanType {
        SECURED,
        UNSECURED
    }

    // Business logic methods
    public BigDecimal calculateDebtToIncomeRatio() {
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return monthlyDebtPayments.add(calculateMonthlyLoanPayment())
            .divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    public BigDecimal calculateMonthlyLoanPayment() {
        if (loanAmount == null || loanTermMonths == null || loanTermMonths == 0) {
            return BigDecimal.ZERO;
        }
        // Simple calculation - in real application, use proper loan payment formula
        return loanAmount.divide(new BigDecimal(loanTermMonths), 2, BigDecimal.ROUND_HALF_UP);
    }

    public boolean isEligibleForLoan() {
        return creditScore >= 600 
            && calculateDebtToIncomeRatio().compareTo(new BigDecimal("43")) <= 0
            && employmentStatus != EmploymentStatus.UNEMPLOYED;
    }
} 