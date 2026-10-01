package com.oms.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.order.config.KafkaTopics;
import com.oms.order.domain.CustomerOrder;
import com.oms.order.domain.DomainExceptions.ServiceUnavailableException;
import com.oms.order.messaging.OrderEvents.OrderCreatedEvent;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopics topics;

    public OrderEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopics topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    public void publishOrderCreated(CustomerOrder order) {
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topics.getOrderCreated(), order.getId().toString(), payload).get(5, TimeUnit.SECONDS);
            log.info("Published OrderCreated for order {}", order.getId());
        } catch (JsonProcessingException ex) {
            throw new ServiceUnavailableException("Failed to serialize OrderCreated", ex);
        } catch (Exception ex) {
            throw new ServiceUnavailableException("Failed to publish OrderCreated for order " + order.getId(), ex);
        }
    }
}
