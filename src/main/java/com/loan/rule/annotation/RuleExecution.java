package com.loan.rule.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuleExecution {
    int maxRetries() default 0;
    long retryDelay() default 1000; // milliseconds
    int circuitBreakerThreshold() default 5;
    long circuitBreakerResetTimeout() default 60000; // milliseconds
    boolean fallbackEnabled() default false;
    String fallbackMethod() default "";
} 