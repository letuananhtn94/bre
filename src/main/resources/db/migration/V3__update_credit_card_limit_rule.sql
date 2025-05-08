-- Update Credit Card Limit rule to use rule class
UPDATE rules 
SET rule_class = 'CreditCardLimitRule',
    rule_type = 'CLASS',
    rule_script = NULL
WHERE name = 'Credit Card Limit Calculation';

-- Add example usage comment
/*
Example usage of CreditCardLimitRule:

Input:
{
    "monthlyIncome": 15000000,
    "creditScore": 780,
    "employmentType": "PERMANENT",
    "latePayments": 0
}

Expected output:
{
    "status": "SUCCESS",
    "result": 105300000.0  // 15,000,000 * 3 * 1.5 * 1.3 * 1.2
}

Calculation breakdown:
1. Base limit = monthlyIncome * 3 = 45,000,000
2. Credit score factor (780) = 1.5
3. Employment factor (PERMANENT) = 1.3
4. History factor (0 late payments) = 1.2
5. Final limit = 45,000,000 * 1.5 * 1.3 * 1.2 = 105,300,000
*/ 