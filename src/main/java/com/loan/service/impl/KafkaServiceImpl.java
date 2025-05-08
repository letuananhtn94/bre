package com.loan.service.impl;

import com.loan.model.LoanApprovalRequest;
import com.loan.model.LoanApprovalResult;
import com.loan.service.KafkaService;
import com.loan.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaServiceImpl implements KafkaService {

    private final KafkaTemplate<String, LoanApprovalResult> kafkaTemplate;
    private final WorkflowService workflowService;

    @Value("${kafka.topics.loan-approval-results}")
    private String loanApprovalResultsTopic;

    @Override
    public void sendLoanApprovalResult(LoanApprovalResult result) {
        CompletableFuture<SendResult<String, LoanApprovalResult>> future = 
            kafkaTemplate.send(loanApprovalResultsTopic, result.getRequestId(), result);

        future.whenComplete((sendResult, ex) -> {
            if (ex == null) {
                log.info("Sent loan approval result for request: {}", result.getRequestId());
            } else {
                log.error("Failed to send loan approval result for request: {}", result.getRequestId(), ex);
            }
        });
    }

    @Override
    @KafkaListener(topics = "${kafka.topics.loan-approval-requests}")
    public void handleLoanApprovalRequest(LoanApprovalRequest request) {
        try {
            log.info("Received loan approval request: {}", request.getRequestId());
            
            LoanApprovalResult result = workflowService.executeWorkflowStep(
                request.getProductCode(),
                request.getWorkflowStep(),
                request.getData()
            );

            sendLoanApprovalResult(result);
        } catch (Exception e) {
            log.error("Error processing loan approval request: {}", request.getRequestId(), e);
            
            LoanApprovalResult errorResult = LoanApprovalResult.builder()
                .requestId(request.getRequestId())
                .productCode(request.getProductCode())
                .workflowStep(request.getWorkflowStep())
                .approved(false)
                .errorMessage("Error processing request: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

            sendLoanApprovalResult(errorResult);
        }
    }
} 