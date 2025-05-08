-- Create rules table if not exists
CREATE TABLE IF NOT EXISTS rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    workflow_step VARCHAR(50),
    rule_type VARCHAR(20) NOT NULL,
    rule_class VARCHAR(255),
    rule_script TEXT,
    input_type VARCHAR(1000),
    output_type VARCHAR(1000),
    execution_order INT,
    active BOOLEAN NOT NULL DEFAULT true,
    error_threshold INT,
    retry_count INT,
    retry_delay_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample rules

-- 1. Script Rule - Age and Income Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Age and Income Check',
    'Kiểm tra tuổi và thu nhập tối thiểu',
    'PERSONAL_LOAN',
    'SCRIPT',
    'age >= 18 && monthlyIncome >= 5000000',
    '{"age": "number", "monthlyIncome": "number"}',
    'boolean',
    1,
    true
);

-- 2. API Rule - Credit Score Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Credit Score Check',
    'Kiểm tra điểm tín dụng từ hệ thống bên ngoài',
    'PERSONAL_LOAN',
    'API',
    'https://api.credit-score.com/v1/check',
    '{"customerId": "string", "productCode": "string"}',
    '{"score": "number", "status": "string"}',
    2,
    true
);

-- 3. Database Rule - Blacklist Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Blacklist Check',
    'Kiểm tra khách hàng có trong danh sách đen không',
    'PERSONAL_LOAN',
    'DATABASE',
    'SELECT COUNT(*) FROM blacklist WHERE customer_id = :customerId AND status = ''ACTIVE''',
    '{"customerId": "string"}',
    'number',
    3,
    true
);

-- 4. Database Rule - Loan History Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Loan History Check',
    'Kiểm tra lịch sử vay của khách hàng',
    'PERSONAL_LOAN',
    'DATABASE',
    'SELECT COUNT(*) as total_loans, 
            SUM(CASE WHEN status = ''DEFAULT'' THEN 1 ELSE 0 END) as default_count
     FROM loan_history 
     WHERE customer_id = :customerId 
     AND created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 12 MONTH)',
    '{"customerId": "string"}',
    '{"totalLoans": "number", "defaultCount": "number"}',
    4,
    true
);

-- 5. Composite Rule - Full Customer Validation
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Full Customer Validation',
    'Kiểm tra toàn diện thông tin khách hàng',
    'PERSONAL_LOAN',
    'COMPOSITE',
    '1,2,3,4',  -- IDs của các rule cần thực thi
    '{"customerId": "string", "age": "number", "monthlyIncome": "number"}',
    '{"isValid": "boolean", "details": "object"}',
    5,
    true
);

-- 6. Script Rule - Calculate Loan Limit
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Calculate Loan Limit',
    'Tính toán hạn mức vay dựa trên thu nhập và điểm tín dụng',
    'PERSONAL_LOAN',
    'SCRIPT',
    'monthlyIncome * 12 * (creditScore >= 700 ? 0.8 : (creditScore >= 600 ? 0.6 : 0.4))',
    '{"monthlyIncome": "number", "creditScore": "number"}',
    'number',
    6,
    true
);

-- 7. API Rule - Employment Verification
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Employment Verification',
    'Xác minh thông tin việc làm từ hệ thống bảo hiểm xã hội',
    'PERSONAL_LOAN',
    'API',
    'https://api.social-insurance.gov.vn/v1/employment/verify',
    '{"customerId": "string", "companyCode": "string"}',
    '{"isVerified": "boolean", "employmentDuration": "number"}',
    7,
    true
);

-- 8. Script Rule - Debt to Income Ratio Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Debt to Income Ratio Check',
    'Kiểm tra tỷ lệ nợ trên thu nhập',
    'PERSONAL_LOAN',
    'SCRIPT',
    'totalMonthlyDebt / monthlyIncome <= 0.5',
    '{"totalMonthlyDebt": "number", "monthlyIncome": "number"}',
    'boolean',
    8,
    true
);

-- 9. Database Rule - Collateral Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Collateral Check',
    'Kiểm tra tài sản đảm bảo',
    'PERSONAL_LOAN',
    'DATABASE',
    'SELECT COUNT(*) as collateral_count, 
            SUM(estimated_value) as total_value
     FROM collateral 
     WHERE customer_id = :customerId 
     AND status = ''ACTIVE''',
    '{"customerId": "string"}',
    '{"collateralCount": "number", "totalValue": "number"}',
    9,
    true
);

-- 10. Composite Rule - Final Approval Check
INSERT INTO rules (
    name, 
    description, 
    product_code, 
    rule_type, 
    rule_script, 
    input_type,
    output_type,
    execution_order,
    active
) VALUES (
    'Final Approval Check',
    'Kiểm tra cuối cùng trước khi phê duyệt khoản vay',
    'PERSONAL_LOAN',
    'COMPOSITE',
    '5,6,8,9',  -- IDs của các rule cần thực thi
    '{"customerId": "string", "monthlyIncome": "number", "totalMonthlyDebt": "number"}',
    '{"isApproved": "boolean", "approvedAmount": "number", "rejectionReason": "string"}',
    10,
    true
); 