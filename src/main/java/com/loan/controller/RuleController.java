package com.loan.controller;

import com.loan.domain.Rule;
import com.loan.service.RuleEngineService;
import com.loan.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleEngineService ruleEngineService;
    private final WorkflowService workflowService;

    @PostMapping("/{ruleId}/test")
    public ResponseEntity<?> testRule(
            @PathVariable Long ruleId,
            @RequestBody Map<String, Object> input) {
        try {
            // Comment out or remove the following lines:
            // Rule rule = ruleEngineService.getRule(ruleId);
            // if (rule == null) {
            //     return ResponseEntity.notFound().build();
            // }

            // Comment out or remove the following lines:
            // Object result = ruleEngineService.executeRule(rule, input);
            // return ResponseEntity.ok(result);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error testing rule: {}", ruleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ruleId}/activate")
    public ResponseEntity<?> activateRule(@PathVariable Long ruleId) {
        try {
            // Comment out or remove the following lines:
            // ruleEngineService.activateRule(ruleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error activating rule: {}", ruleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ruleId}/deactivate")
    public ResponseEntity<?> deactivateRule(@PathVariable Long ruleId) {
        try {
            // Comment out or remove the following lines:
            // ruleEngineService.deactivateRule(ruleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deactivating rule: {}", ruleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 