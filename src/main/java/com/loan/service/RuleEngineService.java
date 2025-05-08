package com.loan.service;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import java.util.Map;

public interface RuleEngineService {
    RuleResult executeRule(Rule rule, Map<String, Object> context);
    Object evaluateScript(String script, Map<String, Object> context);
    boolean validateRuleInput(Rule rule, Map<String, Object> input);
    Object convertOutput(Object result, String outputType);
} 