package com.loan.service.impl;

import com.loan.domain.WorkflowStep;
import com.loan.service.SchedulerService;
import com.loan.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    private final Scheduler scheduler;
    private final WorkflowService workflowService;

    @Override
    public void scheduleWorkflowStep(WorkflowStep step) {
        try {
            if (!step.isAutomated() || step.getCronExpression() == null) {
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(WorkflowStepJob.class)
                .withIdentity(getJobKey(step))
                .usingJobData("productCode", step.getWorkflow().getProductCode())
                .usingJobData("stepCode", step.getStepCode())
                .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(step))
                .withSchedule(CronScheduleBuilder.cronSchedule(step.getCronExpression()))
                .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled workflow step: {} for product: {}", 
                step.getStepCode(), step.getWorkflow().getProductCode());
        } catch (SchedulerException e) {
            log.error("Error scheduling workflow step: {}", step.getStepCode(), e);
            throw new RuntimeException("Failed to schedule workflow step", e);
        }
    }

    @Override
    public void unscheduleWorkflowStep(String productCode, String stepCode) {
        try {
            JobKey jobKey = JobKey.jobKey(getJobName(productCode, stepCode));
            scheduler.deleteJob(jobKey);
            log.info("Unscheduled workflow step: {} for product: {}", stepCode, productCode);
        } catch (SchedulerException e) {
            log.error("Error unscheduling workflow step: {}", stepCode, e);
            throw new RuntimeException("Failed to unschedule workflow step", e);
        }
    }

    @Override
    public void executeScheduledStep(String productCode, String stepCode) {
        try {
            JobKey jobKey = JobKey.jobKey(getJobName(productCode, stepCode));
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            log.error("Error executing scheduled step: {}", stepCode, e);
            throw new RuntimeException("Failed to execute scheduled step", e);
        }
    }

    @Override
    public boolean isStepScheduled(String productCode, String stepCode) {
        try {
            JobKey jobKey = JobKey.jobKey(getJobName(productCode, stepCode));
            return scheduler.checkExists(jobKey);
        } catch (SchedulerException e) {
            log.error("Error checking if step is scheduled: {}", stepCode, e);
            return false;
        }
    }

    private JobKey getJobKey(WorkflowStep step) {
        return JobKey.jobKey(getJobName(step.getWorkflow().getProductCode(), step.getStepCode()));
    }

    private TriggerKey getTriggerKey(WorkflowStep step) {
        return TriggerKey.triggerKey(getTriggerName(step.getWorkflow().getProductCode(), step.getStepCode()));
    }

    private String getJobName(String productCode, String stepCode) {
        return String.format("workflow-step-%s-%s", productCode, stepCode);
    }

    private String getTriggerName(String productCode, String stepCode) {
        return String.format("workflow-step-trigger-%s-%s", productCode, stepCode);
    }
} 