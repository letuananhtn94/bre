-- Add rules for Credit Card product

-- 1. Credit Card - Income and Employment Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Credit Card Income Check',
    'Kiểm tra thu nhập và thời gian làm việc cho thẻ tín dụng',
    'CREDIT_CARD',
    'SCRIPT',
    'monthlyIncome >= 8000000 && employmentDuration >= 12 && employmentType in {''FULL_TIME'', ''PERMANENT''}',
    '{"monthlyIncome": "number", "employmentDuration": "number", "employmentType": "string"}',
    'boolean',
    1,
    true,
    3,
    2,
    1000
);

-- 2. Credit Card - Credit History Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Credit Card History Check',
    'Kiểm tra lịch sử sử dụng thẻ tín dụng',
    'CREDIT_CARD',
    'DATABASE',
    'SELECT 
        COUNT(*) as total_cards,
        SUM(CASE WHEN status = ''ACTIVE'' THEN 1 ELSE 0 END) as active_cards,
        MAX(credit_limit) as max_limit,
        SUM(CASE WHEN payment_status = ''LATE'' THEN 1 ELSE 0 END) as late_payments
     FROM credit_card_history 
     WHERE customer_id = :customerId 
     AND created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 24 MONTH)',
    '{"customerId": "string"}',
    '{"totalCards": "number", "activeCards": "number", "maxLimit": "number", "latePayments": "number"}',
    2,
    true,
    3,
    2,
    1000
);

-- 3. Credit Card - Credit Limit Calculation
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Credit Card Limit Calculation',
    'Tính toán hạn mức thẻ tín dụng dựa trên nhiều yếu tố',
    'CREDIT_CARD',
    'SCRIPT',
    'let baseLimit = monthlyIncome * 3;
     let creditScoreFactor = creditScore >= 750 ? 1.5 : (creditScore >= 650 ? 1.2 : 1.0);
     let employmentFactor = employmentType == ''PERMANENT'' ? 1.3 : 1.0;
     let historyFactor = latePayments == 0 ? 1.2 : (latePayments <= 2 ? 1.0 : 0.8);
     baseLimit * creditScoreFactor * employmentFactor * historyFactor',
    '{"monthlyIncome": "number", "creditScore": "number", "employmentType": "string", "latePayments": "number"}',
    'number',
    3,
    true,
    3,
    2,
    1000
);

-- Add rules for Mortgage Loan product

-- 4. Mortgage - Property Value Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Property Value Check',
    'Kiểm tra giá trị tài sản thế chấp',
    'MORTGAGE_LOAN',
    'API',
    'https://api.property-valuation.com/v1/assess',
    '{"propertyId": "string", "propertyType": "string", "location": "string"}',
    '{"estimatedValue": "number", "confidenceScore": "number", "valuationDate": "string"}',
    1,
    true,
    5,
    3,
    2000
);

-- 5. Mortgage - Loan to Value Ratio
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'LTV Ratio Check',
    'Kiểm tra tỷ lệ khoản vay trên giá trị tài sản',
    'MORTGAGE_LOAN',
    'SCRIPT',
    'let ltvRatio = loanAmount / propertyValue;
     let maxLtv = propertyType == ''APARTMENT'' ? 0.7 : 
                  (propertyType == ''HOUSE'' ? 0.8 : 0.6);
     ltvRatio <= maxLtv',
    '{"loanAmount": "number", "propertyValue": "number", "propertyType": "string"}',
    'boolean',
    2,
    true,
    3,
    2,
    1000
);

-- 6. Mortgage - Complex Income Verification
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Complex Income Verification',
    'Kiểm tra thu nhập phức tạp cho khoản vay thế chấp',
    'MORTGAGE_LOAN',
    'COMPOSITE',
    '7,8,9',  -- IDs của các rule con
    '{"customerId": "string", "incomeSources": "array", "employmentHistory": "array"}',
    '{"isVerified": "boolean", "totalIncome": "number", "incomeDetails": "object"}',
    3,
    true,
    3,
    2,
    1000
);

-- 7. Mortgage - Primary Income Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Primary Income Check',
    'Kiểm tra thu nhập chính',
    'MORTGAGE_LOAN',
    'SCRIPT',
    'let primaryIncome = incomeSources.find(s => s.type == ''PRIMARY'');
     primaryIncome != null && 
     primaryIncome.amount >= 15000000 && 
     primaryIncome.duration >= 24',
    '{"incomeSources": "array"}',
    'boolean',
    4,
    true,
    3,
    2,
    1000
);

-- 8. Mortgage - Additional Income Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Additional Income Check',
    'Kiểm tra thu nhập phụ',
    'MORTGAGE_LOAN',
    'SCRIPT',
    'let additionalIncome = incomeSources
        .filter(s => s.type != ''PRIMARY'')
        .reduce((sum, source) => sum + source.amount, 0);
     additionalIncome <= primaryIncome * 0.5',
    '{"incomeSources": "array", "primaryIncome": "number"}',
    'boolean',
    5,
    true,
    3,
    2,
    1000
);

-- 9. Mortgage - Employment History Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Employment History Check',
    'Kiểm tra lịch sử việc làm',
    'MORTGAGE_LOAN',
    'SCRIPT',
    'let totalExperience = employmentHistory
        .reduce((sum, job) => sum + job.duration, 0);
     let jobChanges = employmentHistory.length;
     totalExperience >= 36 && jobChanges <= 3',
    '{"employmentHistory": "array"}',
    'boolean',
    6,
    true,
    3,
    2,
    1000
);

-- 10. Complex Rule with Timeout and Retry
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active,
    error_threshold,
    retry_count,
    retry_delay_ms
) VALUES (
    'Complex Risk Assessment',
    'Đánh giá rủi ro phức tạp với timeout và retry',
    'MORTGAGE_LOAN',
    'COMPOSITE',
    '4,5,6',  -- IDs của các rule con
    '{"customerId": "string", "loanAmount": "number", "propertyDetails": "object", "incomeDetails": "object"}',
    '{"riskScore": "number", "riskLevel": "string", "riskFactors": "array", "approvalRecommendation": "string"}',
    7,
    true,
    5,
    3,
    2000
); 