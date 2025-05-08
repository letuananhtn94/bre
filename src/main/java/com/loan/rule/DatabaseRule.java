package com.loan.rule;

import com.loan.domain.Rule;
import com.loan.domain.ExecutionStatus;
import com.loan.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class DatabaseRule extends BaseRule {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public DatabaseRule(Rule rule, JdbcTemplate jdbcTemplate) {
        super(rule);
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public RuleResult execute(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        RuleResult result = RuleResult.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .status(ExecutionStatus.SUCCESS)
            .build();

        try {
            if (!StringUtils.hasText(rule.getRuleScript())) {
                throw new IllegalArgumentException("SQL query is not specified");
            }

            // Execute query with parameters
            MapSqlParameterSource params = new MapSqlParameterSource();
            context.forEach(params::addValue);

            Object queryResult;
            if (isSelectQuery(rule.getRuleScript())) {
                queryResult = namedParameterJdbcTemplate.queryForObject(
                    rule.getRuleScript(),
                    params,
                    Object.class
                );
            } else {
                queryResult = namedParameterJdbcTemplate.update(
                    rule.getRuleScript(),
                    params
                );
            }

            result.setResult(queryResult);

        } catch (Exception e) {
            log.error("Error executing database rule: {}", rule.getName(), e);
            result.setStatus(ExecutionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public boolean validateInput(Map<String, Object> context) {
        if (context == null) {
            return false;
        }
        // Validate SQL query
        if (!StringUtils.hasText(rule.getRuleScript())) {
            return false;
        }
        // Add more specific validation if needed
        return true;
    }

    @Override
    public String getRuleType() {
        return "DATABASE";
    }

    private boolean isSelectQuery(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }
} 