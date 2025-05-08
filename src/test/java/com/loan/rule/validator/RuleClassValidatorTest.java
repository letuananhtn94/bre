package com.loan.rule.validator;

import com.loan.domain.Rule;
import com.loan.rule.CreditCardLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RuleClassValidatorTest {

    @Autowired
    private RuleClassValidator validator;

    private Rule validRule;
    private Rule invalidRule;
    private Rule wrongProductRule;

    @BeforeEach
    void setUp() {
        // Rule hợp lệ
        validRule = new Rule();
        validRule.setName("Credit Card Limit Rule");
        validRule.setRuleClass("CreditCardLimitRule");
        validRule.setProductCode("CREDIT_CARD");

        // Rule không hợp lệ - tên class không tồn tại
        invalidRule = new Rule();
        invalidRule.setName("Invalid Rule");
        invalidRule.setRuleClass("NonExistentRule");
        invalidRule.setProductCode("CREDIT_CARD");

        // Rule không hợp lệ - product code không khớp
        wrongProductRule = new Rule();
        wrongProductRule.setName("Wrong Product Rule");
        wrongProductRule.setRuleClass("CreditCardLimitRule");
        wrongProductRule.setProductCode("MORTGAGE_LOAN");
    }

    @Test
    void validateRuleClass_WithValidRule_ReturnsTrue() {
        assertTrue(validator.validateRuleClass(validRule));
    }

    @Test
    void validateRuleClass_WithInvalidRule_ReturnsFalse() {
        assertFalse(validator.validateRuleClass(invalidRule));
    }

    @Test
    void validateRuleClass_WithWrongProduct_ReturnsFalse() {
        assertFalse(validator.validateRuleClass(wrongProductRule));
    }

    @Test
    void getRuleClasses_ReturnsAllRegisteredRules() {
        var ruleClasses = validator.getRuleClasses();
        assertTrue(ruleClasses.containsKey("CreditCardLimitRule"));
        assertEquals(CreditCardLimitRule.class, ruleClasses.get("CreditCardLimitRule"));
    }

    @Configuration
    static class TestConfig {
        @Bean
        public CreditCardLimitRule creditCardLimitRule() {
            return new CreditCardLimitRule();
        }
    }
} 