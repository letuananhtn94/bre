package com.loan.rule.validator;

import com.loan.domain.Rule;
import com.loan.rule.BaseRule;
import com.loan.rule.annotation.RuleClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RuleClassValidator implements org.springframework.beans.factory.InitializingBean {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final Map<String, Class<? extends BaseRule>> ruleClasses = new HashMap<>();
    
    @Override
    public void afterPropertiesSet() {
        // Tìm tất cả các bean là BaseRule
        Map<String, BaseRule> ruleBeans = applicationContext.getBeansOfType(BaseRule.class);
        
        // Lưu tên class và class tương ứng
        for (BaseRule rule : ruleBeans.values()) {
            Class<? extends BaseRule> ruleClass = rule.getClass();
            String className = ruleClass.getSimpleName();
            
            // Kiểm tra annotation
            RuleClass annotation = ruleClass.getAnnotation(RuleClass.class);
            if (annotation != null) {
                log.info("Found rule class: {} with product code: {}", 
                    className, annotation.productCode());
            }
            
            ruleClasses.put(className, ruleClass);
        }
        
        log.info("Initialized {} rule classes", ruleClasses.size());
    }
    
    public boolean validateRuleClass(Rule rule) {
        String ruleClass = rule.getRuleClass();
        if (ruleClass == null || ruleClass.trim().isEmpty()) {
            log.error("Rule {} has no rule class specified", rule.getName());
            return false;
        }
        
        // Kiểm tra xem tên class có tồn tại trong hệ thống không
        if (!ruleClasses.containsKey(ruleClass)) {
            log.error("Rule class {} not found in system for rule {}", 
                ruleClass, rule.getName());
            return false;
        }
        
        // Kiểm tra annotation nếu có
        Class<? extends BaseRule> actualClass = ruleClasses.get(ruleClass);
        RuleClass annotation = actualClass.getAnnotation(RuleClass.class);
        if (annotation != null) {
            // Kiểm tra product code nếu được chỉ định
            if (!annotation.productCode().isEmpty() && 
                !annotation.productCode().equals(rule.getProductCode())) {
                log.error("Rule class {} is not valid for product code {} in rule {}", 
                    ruleClass, rule.getProductCode(), rule.getName());
                return false;
            }
        }
        
        return true;
    }
    
    public Map<String, Class<? extends BaseRule>> getRuleClasses() {
        return new HashMap<>(ruleClasses);
    }
} 