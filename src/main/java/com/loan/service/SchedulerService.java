package com.loan.service;

import com.loan.domain.WorkflowStep;

public interface SchedulerService {
    void scheduleWorkflowStep(WorkflowStep step);
    void unscheduleWorkflowStep(String productCode, String stepCode);
    void executeScheduledStep(String productCode, String stepCode);
    boolean isStepScheduled(String productCode, String stepCode);
} 