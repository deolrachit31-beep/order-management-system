package com.oms.payment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.config.KafkaTopics;
import com.oms.payment.domain.DomainExceptions.ServiceUnavailableException;
import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentStatus;
import com.oms.payment.messaging.PaymentEvents.PaymentCompletedEvent;
import com.oms.payment.messaging.PaymentEvents.PaymentFailedEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopics topics;

    public PaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopics topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    public void publishCompleted(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                Instant.now());
        send(topics.getPaymentCompleted(), payment, event);
        log.info("Published PaymentCompleted for order {}", payment.getOrderId());
    }

    public void publishFailed(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReason(),
                Instant.now());
        send(topics.getPaymentFailed(), payment, event);
        log.info("Published PaymentFailed for order {} ({})", payment.getOrderId(), payment.getReason());
    }

    public void publish(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            publishCompleted(payment);
        } else {
            publishFailed(payment);
        }
    }

    private void send(String topic, Payment payment, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payment.getOrderId().toString(), payload).get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new ServiceUnavailableException("Failed to serialize payment event", ex);
        } catch (Exception ex) {
            throw new ServiceUnavailableException(
                    "Failed to publish payment event for order " + payment.getOrderId(), ex);
        }
    }
}
