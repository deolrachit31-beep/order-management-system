package com.oms.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.notification.domain.NotificationEventType;
import com.oms.notification.service.NotificationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotificationListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.order-created}")
    public void onOrderCreated(String payload) {
        OrderCreatedEvent event = read(payload, OrderCreatedEvent.class);
        if (event == null || event.orderId() == null) {
            return;
        }
        notificationService.record(
                event.orderId(),
                event.userId(),
                NotificationEventType.ORDER_CREATED,
                event.totalAmount(),
                event.currency(),
                null);
    }

    @KafkaListener(topics = "${app.kafka.payment-completed}")
    public void onPaymentCompleted(String payload) {
        PaymentCompletedEvent event = read(payload, PaymentCompletedEvent.class);
        if (event == null || event.orderId() == null) {
            return;
        }
        notificationService.record(
                event.orderId(),
                event.userId(),
                NotificationEventType.PAYMENT_COMPLETED,
                event.amount(),
                event.currency(),
                null);
    }

    @KafkaListener(topics = "${app.kafka.payment-failed}")
    public void onPaymentFailed(String payload) {
        PaymentFailedEvent event = read(payload, PaymentFailedEvent.class);
        if (event == null || event.orderId() == null) {
            return;
        }
        notificationService.record(
                event.orderId(),
                event.userId(),
                NotificationEventType.PAYMENT_FAILED,
                event.amount(),
                event.currency(),
                event.reason());
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception ex) {
            log.error("Discarding malformed {} payload", type.getSimpleName(), ex);
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreatedEvent(UUID orderId, UUID userId, BigDecimal totalAmount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentCompletedEvent(UUID orderId, UUID userId, BigDecimal amount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentFailedEvent(
            UUID orderId, UUID userId, BigDecimal amount, String currency, String reason) {
    }
}
