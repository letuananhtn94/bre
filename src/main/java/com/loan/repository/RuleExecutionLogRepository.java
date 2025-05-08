package com.loan.repository;

import com.loan.domain.RuleExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleExecutionLogRepository extends JpaRepository<RuleExecutionLog, Long> {
    List<RuleExecutionLog> findByLoanRequestIdOrderByExecutionTimeAsc(String loanRequestId);

    @Query("SELECT l FROM RuleExecutionLog l " +
           "WHERE l.loanRequestId = :loanRequestId " +
           "AND l.workflowStep = :workflowStep " +
           "ORDER BY l.executionTime ASC")
    List<RuleExecutionLog> findByLoanRequestIdAndWorkflowStep(
        @Param("loanRequestId") String loanRequestId,
        @Param("workflowStep") String workflowStep);

    @Query("SELECT l FROM RuleExecutionLog l " +
           "WHERE l.loanRequestId = :loanRequestId " +
           "AND l.executionStatus = 'ERROR' " +
           "ORDER BY l.executionTime DESC")
    List<RuleExecutionLog> findErrorsByLoanRequestId(
        @Param("loanRequestId") String loanRequestId);
} 