package com.oms.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopics {

    private String orderCreated = "order-created";
    private String paymentCompleted = "payment-completed";
    private String paymentFailed = "payment-failed";

    public String getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(String orderCreated) {
        this.orderCreated = orderCreated;
    }

    public String getPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(String paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public String getPaymentFailed() {
        return paymentFailed;
    }

    public void setPaymentFailed(String paymentFailed) {
        this.paymentFailed = paymentFailed;
    }
}

@Configuration
class KafkaTopicConfig {

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
