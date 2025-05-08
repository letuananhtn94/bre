package com.loan.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.loan-approval-requests}")
    private String loanApprovalRequestsTopic;

    @Value("${kafka.topics.loan-approval-results}")
    private String loanApprovalResultsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic loanApprovalRequestsTopic() {
        return TopicBuilder.name(loanApprovalRequestsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic loanApprovalResultsTopic() {
        return TopicBuilder.name(loanApprovalResultsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }
} 