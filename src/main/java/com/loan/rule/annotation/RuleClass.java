package com.loan.rule.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuleClass {
    String description() default "";
    String productCode() default "";
    boolean parallel() default true;
    String[] dependsOn() default {};
    int timeout() default 5000; // milliseconds
} 