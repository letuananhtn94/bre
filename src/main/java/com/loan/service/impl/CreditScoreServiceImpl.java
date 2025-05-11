package com.loan.service.impl;

import com.loan.service.CreditScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CreditScoreServiceImpl implements CreditScoreService {
    
    // In-memory cache for demonstration
    private final Map<String, BigDecimal> creditScoreCache = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getCreditScore(String customerId) {
        log.info("Retrieving credit score for customer: {}", customerId);
        
        // In a real implementation, this would call an external credit bureau service
        // For demonstration, we'll use a cached value or generate a random score
        return creditScoreCache.computeIfAbsent(customerId, this::generateRandomScore);
    }

    private BigDecimal generateRandomScore(String customerId) {
        // Generate a random score between 300 and 850
        double randomScore = 300 + Math.random() * 550;
        return new BigDecimal(String.format("%.0f", randomScore));
    }
} 