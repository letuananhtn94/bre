package com.loan.service;

import com.loan.model.LoanApprovalRequest;
import com.loan.model.LoanApprovalResult;

public interface KafkaService {
    void sendLoanApprovalResult(LoanApprovalResult result);
    void handleLoanApprovalRequest(LoanApprovalRequest request);
} 