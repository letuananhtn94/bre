package com.loan.repository;

import com.loan.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByProductCodeAndWorkflowStepAndIsActiveTrueOrderByExecutionOrderAsc(
        String productCode, String workflowStep);

    @Query("SELECT r FROM Rule r WHERE r.productCode = :productCode " +
           "AND r.workflowStep = :workflowStep " +
           "AND r.isActive = true " +
           "ORDER BY r.executionOrder ASC")
    List<Rule> findActiveRulesForWorkflowStep(
        @Param("productCode") String productCode,
        @Param("workflowStep") String workflowStep);

    boolean existsByNameAndProductCode(String name, String productCode);
} 