package com.oms.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCreatedTopic(KafkaTopics topics) {
        return TopicBuilder.name(topics.getOrderCreated()).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCompletedTopic(KafkaTopics topics) {
        return TopicBuilder.name(topics.getPaymentCompleted()).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic(KafkaTopics topics) {
        return TopicBuilder.name(topics.getPaymentFailed()).partitions(1).replicas(1).build();
    }
}
