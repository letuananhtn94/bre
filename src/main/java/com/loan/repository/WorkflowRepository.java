package com.loan.repository;

import com.loan.domain.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Optional<Workflow> findByProductCodeAndIsActiveTrue(String productCode);

    @Query("SELECT w FROM Workflow w " +
           "LEFT JOIN FETCH w.steps s " +
           "LEFT JOIN FETCH s.rules " +
           "WHERE w.productCode = :productCode " +
           "AND w.isActive = true")
    Optional<Workflow> findActiveWorkflowWithStepsAndRules(
        @Param("productCode") String productCode);

    boolean existsByProductCode(String productCode);
} 