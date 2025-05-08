package com.loan.controller;

import com.loan.domain.Workflow;
import com.loan.service.WorkflowService;
import com.loan.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final SchedulerService schedulerService;

    @GetMapping("/{productCode}")
    public ResponseEntity<?> getWorkflow(@PathVariable String productCode) {
        try {
            Workflow workflow = workflowService.getWorkflow(productCode);
            return ResponseEntity.ok(workflow);
        } catch (Exception e) {
            log.error("Error getting workflow: {}", productCode, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{productCode}/steps/{stepCode}/execute")
    public ResponseEntity<?> executeWorkflowStep(
            @PathVariable String productCode,
            @PathVariable String stepCode,
            @RequestBody Map<String, Object> context) {
        try {
            var result = workflowService.executeWorkflowStep(productCode, stepCode, context);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing workflow step: {} for product: {}", stepCode, productCode, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{productCode}/steps/{stepCode}/schedule")
    public ResponseEntity<?> scheduleWorkflowStep(
            @PathVariable String productCode,
            @PathVariable String stepCode) {
        try {
            var step = workflowService.getWorkflowStep(productCode, stepCode);
            if (step == null) {
                return ResponseEntity.notFound().build();
            }

            schedulerService.scheduleWorkflowStep(step);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error scheduling workflow step: {} for product: {}", stepCode, productCode, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{productCode}/steps/{stepCode}/unschedule")
    public ResponseEntity<?> unscheduleWorkflowStep(
            @PathVariable String productCode,
            @PathVariable String stepCode) {
        try {
            schedulerService.unscheduleWorkflowStep(productCode, stepCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error unscheduling workflow step: {} for product: {}", stepCode, productCode, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{productCode}/steps/{stepCode}/status")
    public ResponseEntity<?> getWorkflowStepStatus(
            @PathVariable String productCode,
            @PathVariable String stepCode) {
        try {
            boolean isScheduled = schedulerService.isStepScheduled(productCode, stepCode);
            return ResponseEntity.ok(Map.of("scheduled", isScheduled));
        } catch (Exception e) {
            log.error("Error getting workflow step status: {} for product: {}", stepCode, productCode, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 