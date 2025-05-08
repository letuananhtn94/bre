package com.loan.service;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import com.loan.rule.BaseRule;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ParallelRuleExecutor {

    @Autowired
    private Map<String, BaseRule> ruleBeans;

    private final ExecutorService executorService;
    private final Map<String, CompletableFuture<RuleResult>> ruleFutures = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public ParallelRuleExecutor() {
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }

    public Map<String, RuleResult> executeRules(List<Rule> rules, Map<String, Object> initialContext) {
        // Reset futures map
        ruleFutures.clear();

        // Tạo map kết quả
        Map<String, RuleResult> results = new ConcurrentHashMap<>();
        
        // Tạo context chung
        Map<String, Object> sharedContext = new ConcurrentHashMap<>(initialContext);

        try {
            // Sắp xếp rules theo dependencies
            List<Rule> sortedRules = sortRulesByDependencies(rules);

            // Thực thi rules
            for (Rule rule : sortedRules) {
                executeRule(rule, sharedContext, results);
            }

            // Đợi tất cả rules hoàn thành
            CompletableFuture.allOf(
                ruleFutures.values().toArray(new CompletableFuture[0])
            ).get(30, TimeUnit.SECONDS);

            // Lấy kết quả
            ruleFutures.forEach((ruleName, future) -> {
                try {
                    results.put(ruleName, future.get());
                } catch (Exception e) {
                    log.error("Error getting result for rule {}: {}", ruleName, e.getMessage());
                    results.put(ruleName, RuleResult.builder()
                        .status(RuleResult.Status.ERROR)
                        .errorMessage("Error executing rule: " + e.getMessage())
                        .build());
                }
            });

        } catch (Exception e) {
            log.error("Error executing rules: {}", e.getMessage());
            throw new RuntimeException("Error executing rules", e);
        }

        return results;
    }

    private void executeRule(Rule rule, Map<String, Object> sharedContext, 
                           Map<String, RuleResult> results) {
        BaseRule ruleBean = ruleBeans.get(rule.getRuleClass());
        if (ruleBean == null) {
            throw new IllegalArgumentException("Rule not found: " + rule.getRuleClass());
        }

        RuleClass ruleClassAnnotation = ruleBean.getClass().getAnnotation(RuleClass.class);
        RuleExecution ruleExecutionAnnotation = ruleBean.getClass().getAnnotation(RuleExecution.class);
        
        if (ruleClassAnnotation == null) {
            throw new IllegalArgumentException("Rule class not annotated: " + rule.getRuleClass());
        }

        // Kiểm tra dependencies
        if (ruleClassAnnotation.dependsOn().length > 0) {
            for (String dependency : ruleClassAnnotation.dependsOn()) {
                CompletableFuture<RuleResult> dependencyFuture = ruleFutures.get(dependency);
                if (dependencyFuture == null) {
                    throw new IllegalStateException(
                        "Dependency not found: " + dependency + " for rule: " + rule.getName());
                }
            }
        }

        // Tạo input cho rule
        Map<String, Object> ruleInput = new HashMap<>(sharedContext);
        
        // Thêm kết quả của các rule phụ thuộc vào input
        if (ruleClassAnnotation.dependsOn().length > 0) {
            for (String dependency : ruleClassAnnotation.dependsOn()) {
                RuleResult dependencyResult = results.get(dependency);
                if (dependencyResult != null && dependencyResult.getStatus() == RuleResult.Status.SUCCESS) {
                    ruleInput.put(dependency, dependencyResult.getResult());
                }
            }
        }

        // Thực thi rule với retry và circuit breaker
        CompletableFuture<RuleResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Kiểm tra circuit breaker
                CircuitBreaker circuitBreaker = getCircuitBreaker(rule.getName(), ruleExecutionAnnotation);
                if (!circuitBreaker.allowExecution()) {
                    log.warn("Circuit breaker is open for rule: {}", rule.getName());
                    if (ruleExecutionAnnotation != null && ruleExecutionAnnotation.fallbackEnabled()) {
                        return executeFallback(ruleBean, rule, ruleInput, ruleExecutionAnnotation.fallbackMethod());
                    }
                    return RuleResult.builder()
                        .status(RuleResult.Status.ERROR)
                        .errorMessage("Circuit breaker is open")
                        .build();
                }

                // Thực thi rule với retry
                RuleResult result = executeWithRetry(ruleBean, rule, ruleInput, ruleExecutionAnnotation);
                
                // Cập nhật circuit breaker
                circuitBreaker.recordResult(result.getStatus() == RuleResult.Status.SUCCESS);
                
                return result;
            } catch (Exception e) {
                log.error("Error executing rule {}: {}", rule.getName(), e.getMessage());
                return RuleResult.builder()
                    .status(RuleResult.Status.ERROR)
                    .errorMessage("Error executing rule: " + e.getMessage())
                    .build();
            }
        }, executorService).orTimeout(ruleClassAnnotation.timeout(), TimeUnit.MILLISECONDS);

        ruleFutures.put(rule.getName(), future);
    }

    private RuleResult executeWithRetry(BaseRule ruleBean, Rule rule, 
                                      Map<String, Object> input,
                                      RuleExecution annotation) {
        if (annotation == null || annotation.maxRetries() == 0) {
            return ruleBean.execute(rule, input);
        }

        int attempts = 0;
        while (attempts <= annotation.maxRetries()) {
            try {
                return ruleBean.execute(rule, input);
            } catch (Exception e) {
                attempts++;
                if (attempts > annotation.maxRetries()) {
                    throw e;
                }
                log.warn("Retry attempt {} for rule {}: {}", 
                    attempts, rule.getName(), e.getMessage());
                try {
                    Thread.sleep(annotation.retryDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        throw new RuntimeException("Max retry attempts exceeded");
    }

    private RuleResult executeFallback(BaseRule ruleBean, Rule rule,
                                     Map<String, Object> input,
                                     String fallbackMethod) {
        try {
            return (RuleResult) ruleBean.getClass()
                .getMethod(fallbackMethod, Rule.class, Map.class)
                .invoke(ruleBean, rule, input);
        } catch (Exception e) {
            log.error("Error executing fallback for rule {}: {}", 
                rule.getName(), e.getMessage());
            return RuleResult.builder()
                .status(RuleResult.Status.ERROR)
                .errorMessage("Error in fallback: " + e.getMessage())
                .build();
        }
    }

    private CircuitBreaker getCircuitBreaker(String ruleName, RuleExecution annotation) {
        return circuitBreakers.computeIfAbsent(ruleName, k -> {
            if (annotation == null) {
                return new CircuitBreaker(5, 60000); // Default values
            }
            return new CircuitBreaker(
                annotation.circuitBreakerThreshold(),
                annotation.circuitBreakerResetTimeout()
            );
        });
    }

    private List<Rule> sortRulesByDependencies(List<Rule> rules) {
        Map<String, Set<String>> dependencyGraph = new HashMap<>();
        Map<String, Rule> ruleMap = new HashMap<>();

        // Xây dựng đồ thị phụ thuộc
        for (Rule rule : rules) {
            BaseRule ruleBean = ruleBeans.get(rule.getRuleClass());
            if (ruleBean != null) {
                RuleClass annotation = ruleBean.getClass().getAnnotation(RuleClass.class);
                if (annotation != null) {
                    dependencyGraph.put(rule.getName(), 
                        new HashSet<>(Arrays.asList(annotation.dependsOn())));
                }
            }
            ruleMap.put(rule.getName(), rule);
        }

        // Sắp xếp topologically
        List<Rule> sortedRules = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> temp = new HashSet<>();

        for (String ruleName : dependencyGraph.keySet()) {
            if (!visited.contains(ruleName)) {
                topologicalSort(ruleName, dependencyGraph, visited, temp, sortedRules, ruleMap);
            }
        }

        return sortedRules;
    }

    private void topologicalSort(String ruleName, Map<String, Set<String>> graph,
                               Set<String> visited, Set<String> temp,
                               List<Rule> sorted, Map<String, Rule> ruleMap) {
        if (temp.contains(ruleName)) {
            throw new IllegalStateException("Circular dependency detected for rule: " + ruleName);
        }
        if (visited.contains(ruleName)) {
            return;
        }

        temp.add(ruleName);
        Set<String> dependencies = graph.getOrDefault(ruleName, Collections.emptySet());
        for (String dependency : dependencies) {
            topologicalSort(dependency, graph, visited, temp, sorted, ruleMap);
        }

        temp.remove(ruleName);
        visited.add(ruleName);
        sorted.add(ruleMap.get(ruleName));
    }

    private static class CircuitBreaker {
        private final int threshold;
        private final long resetTimeout;
        private int failureCount;
        private long lastFailureTime;
        private boolean isOpen;

        public CircuitBreaker(int threshold, long resetTimeout) {
            this.threshold = threshold;
            this.resetTimeout = resetTimeout;
            this.failureCount = 0;
            this.lastFailureTime = 0;
            this.isOpen = false;
        }

        public synchronized boolean allowExecution() {
            if (isOpen) {
                if (System.currentTimeMillis() - lastFailureTime >= resetTimeout) {
                    reset();
                    return true;
                }
                return false;
            }
            return true;
        }

        public synchronized void recordResult(boolean success) {
            if (success) {
                reset();
            } else {
                failureCount++;
                lastFailureTime = System.currentTimeMillis();
                if (failureCount >= threshold) {
                    isOpen = true;
                }
            }
        }

        private void reset() {
            failureCount = 0;
            isOpen = false;
        }
    }
} 