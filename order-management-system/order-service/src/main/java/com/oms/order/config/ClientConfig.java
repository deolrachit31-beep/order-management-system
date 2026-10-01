package com.oms.order.config;

import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient productRestClient(RestClient.Builder builder, ProductServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
    }

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
