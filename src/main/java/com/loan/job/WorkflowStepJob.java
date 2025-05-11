package com.loan.job;

import com.loan.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowStepJob implements Job {

    private final WorkflowService workflowService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String productCode = context.getJobDetail().getJobDataMap().getString("productCode");
        String stepCode = context.getJobDetail().getJobDataMap().getString("stepCode");

        try {
            log.info("Executing scheduled workflow step: {} for product: {}", stepCode, productCode);
            
            Map<String, Object> jobContext = new HashMap<>();
            jobContext.put("requestId", "SCHEDULED-" + System.currentTimeMillis());
            jobContext.put("source", "SCHEDULER");

            workflowService.executeWorkflowStep(productCode, stepCode, jobContext);
            
            log.info("Completed scheduled workflow step: {} for product: {}", stepCode, productCode);
        } catch (Exception e) {
            log.error("Error executing scheduled workflow step: {} for product: {}", 
                stepCode, productCode, e);
            throw new JobExecutionException(e);
        }
    }
} 