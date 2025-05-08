package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class ApiRule extends BaseRule {
    private final RestTemplate restTemplate;

    public ApiRule(Rule rule) {
        super(rule);
        this.restTemplate = new RestTemplate();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        RuleResult result = RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .build();

        try {
            if (!StringUtils.hasText(rule.getRuleScript())) {
                throw new IllegalArgumentException("API endpoint is not specified");
            }

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Add any additional headers from context
            if (context.containsKey("headers")) {
                Map<String, String> contextHeaders = (Map<String, String>) context.get("headers");
                contextHeaders.forEach(headers::set);
            }

            HttpEntity<Object> requestEntity = new HttpEntity<>(context.get("body"), headers);

            // Make API call
            ResponseEntity<Object> response = restTemplate.exchange(
                rule.getRuleScript(), // Using ruleScript field to store API endpoint
                HttpMethod.POST,
                requestEntity,
                Object.class
            );

            result.setResult(response.getBody());

        } catch (Exception e) {
            log.error("Error executing API rule: {}", rule.getName(), e);
            result.setStatus(ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        if (context == null) {
            return false;
        }
        // Validate required fields for API call
        return context.containsKey("body");
    }

    @Override
    public String getRuleType() {
        return "API";
    }
} 