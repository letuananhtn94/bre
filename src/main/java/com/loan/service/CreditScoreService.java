package com.loan.service;

import java.math.BigDecimal;

public interface CreditScoreService {
    /**
     * Retrieves the credit score for a customer
     * @param customerId The ID of the customer
     * @return The customer's credit score, or null if not found
     */
    BigDecimal getCreditScore(String customerId);
} 