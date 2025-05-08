package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RuleClass(
    description = "Rule for calculating credit limit based on credit score and income",
    productCode = "CREDIT_CARD",
    dependsOn = {"CreditScoreCheck"},
    timeout = 3000
)
public class CreditScoreBasedLimitRule extends BaseRule {
    
    private static final double HIGH_SCORE_MULTIPLIER = 1.5;
    private static final double MEDIUM_SCORE_MULTIPLIER = 1.2;
    private static final double LOW_SCORE_MULTIPLIER = 0.8;
    private static final double HIGH_SCORE_THRESHOLD = 750.0;
    private static final double MEDIUM_SCORE_THRESHOLD = 650.0;

    @Override
    public RuleResult execute(Rule rule, Map<String, Object> input) {
        try {
            // Validate input
            validateInput(input, "monthlyIncome", "CreditScoreCheck");

            // Extract input values
            double monthlyIncome = ((Number) input.get("monthlyIncome")).doubleValue();
            
            // Lấy kết quả từ CreditScoreCheck rule
            Map<String, Object> creditScoreResult = (Map<String, Object>) input.get("CreditScoreCheck");
            double creditScore = ((Number) creditScoreResult.get("score")).doubleValue();
            String status = (String) creditScoreResult.get("status");

            // Kiểm tra trạng thái credit score
            if (!"VALID".equals(status)) {
                return RuleResult.builder()
                    .status(RuleResult.Status.ERROR)
                    .errorMessage("Invalid credit score status: " + status)
                    .build();
            }

            // Tính toán hạn mức dựa trên điểm tín dụng
            double multiplier = calculateMultiplier(creditScore);
            double limit = monthlyIncome * 12 * multiplier;

            // Tạo kết quả
            return RuleResult.builder()
                .status(RuleResult.Status.SUCCESS)
                .result(limit)
                .build();

        } catch (Exception e) {
            log.error("Error executing CreditScoreBasedLimitRule: {}", e.getMessage(), e);
            return RuleResult.builder()
                .status(RuleResult.Status.ERROR)
                .errorMessage("Error calculating credit limit: " + e.getMessage())
                .build();
        }
    }

    private double calculateMultiplier(double creditScore) {
        if (creditScore >= HIGH_SCORE_THRESHOLD) {
            return HIGH_SCORE_MULTIPLIER;
        } else if (creditScore >= MEDIUM_SCORE_THRESHOLD) {
            return MEDIUM_SCORE_MULTIPLIER;
        }
        return LOW_SCORE_MULTIPLIER;
    }

    @Override
    public boolean supports(Rule rule) {
        return this.getClass().getSimpleName().equals(rule.getRuleClass());
    }
} 