package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import com.loan.rule.annotation.RuleClass;
import com.loan.rule.annotation.RuleExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RuleClass(
    name = "DocumentValidation",
    description = "Validates required documents for loan application",
    timeout = 5000
)
@RuleExecution(
    maxRetries = 3,
    retryDelay = 1000,
    circuitBreakerThreshold = 5,
    circuitBreakerResetTimeout = 60000,
    fallbackEnabled = true,
    fallbackMethod = "fallbackCheck"
)
public class DocumentValidationRule extends BaseRule {

    private static final Map<String, List<String>> REQUIRED_DOCUMENTS = Map.of(
        "PERSONAL", Arrays.asList("ID_CARD", "PROOF_OF_INCOME", "BANK_STATEMENT"),
        "BUSINESS", Arrays.asList("ID_CARD", "BUSINESS_LICENSE", "FINANCIAL_STATEMENT", "TAX_RETURN"),
        "HOME", Arrays.asList("ID_CARD", "PROOF_OF_INCOME", "PROPERTY_DOCUMENTS", "DOWN_PAYMENT_PROOF"),
        "CAR", Arrays.asList("ID_CARD", "PROOF_OF_INCOME", "VEHICLE_DOCUMENTS", "INSURANCE_DOCUMENTS")
    );

    public DocumentValidationRule() {
        super();
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        log.info("Executing document validation for customer: {}", context.get("customerId"));

        String loanPurpose = (String) context.get("loanPurpose");
        @SuppressWarnings("unchecked")
        List<String> providedDocuments = (List<String>) context.get("documents");

        if (loanPurpose == null || providedDocuments == null) {
            return RuleResult.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .status(ExecutionStatus.ERROR)
                .errorMessage("Required document validation data is missing")
                .build();
        }

        List<String> requiredDocs = REQUIRED_DOCUMENTS.getOrDefault(loanPurpose, 
            REQUIRED_DOCUMENTS.get("PERSONAL"));

        List<String> missingDocuments = new ArrayList<>();
        for (String doc : requiredDocs) {
            if (!providedDocuments.contains(doc)) {
                missingDocuments.add(doc);
            }
        }

        boolean approved = missingDocuments.isEmpty();

        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", approved,
                "requiredDocuments", requiredDocs,
                "providedDocuments", providedDocuments,
                "missingDocuments", missingDocuments
            ))
            .build();
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        return context.containsKey("customerId") 
            && context.containsKey("loanPurpose")
            && context.containsKey("documents");
    }

    public RuleResult fallbackCheck(Map<String, Object> context) {
        log.warn("Using fallback for document validation for customer: {}", context.get("customerId"));
        
        return RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .result(Map.of(
                "approved", true,
                "requiredDocuments", Collections.emptyList(),
                "providedDocuments", Collections.emptyList(),
                "missingDocuments", Collections.emptyList(),
                "isFallback", true
            ))
            .build();
    }

    @Override
    public String getRuleType() {
        return "DOCUMENT_VALIDATION";
    }
} 