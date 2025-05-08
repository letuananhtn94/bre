package com.loan.domain;

public enum RuleType {
    SCRIPT,      // Rule defined by script (SpEL)
    API,         // Rule that calls external API
    DATABASE,    // Rule that queries database
    COMPOSITE    // Rule that combines multiple rules
} 