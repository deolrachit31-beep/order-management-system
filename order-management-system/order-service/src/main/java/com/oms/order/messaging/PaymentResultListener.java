package com.oms.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.order.domain.OrderStatus;
import com.oms.order.messaging.OrderEvents.PaymentCompletedEvent;
import com.oms.order.messaging.OrderEvents.PaymentFailedEvent;
import com.oms.order.service.OrderApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final ObjectMapper objectMapper;
    private final OrderApplicationService orderApplicationService;

    public PaymentResultListener(ObjectMapper objectMapper, OrderApplicationService orderApplicationService) {
        this.objectMapper = objectMapper;
        this.orderApplicationService = orderApplicationService;
    }

    @KafkaListener(topics = "${app.kafka.payment-completed}")
    public void onPaymentCompleted(String payload) {
        PaymentCompletedEvent event = read(payload, PaymentCompletedEvent.class);
        if (event == null || event.orderId() == null) {
            log.error("Discarding PaymentCompleted payload without an order id");
            return;
        }
        log.info("Received PaymentCompleted for order {}", event.orderId());
        orderApplicationService.applyExternalStatus(event.orderId(), OrderStatus.PAID);
    }

    @KafkaListener(topics = "${app.kafka.payment-failed}")
    public void onPaymentFailed(String payload) {
        PaymentFailedEvent event = read(payload, PaymentFailedEvent.class);
        if (event == null || event.orderId() == null) {
            log.error("Discarding PaymentFailed payload without an order id");
            return;
        }
        log.info("Received PaymentFailed for order {} ({})", event.orderId(), event.reason());
        orderApplicationService.applyExternalStatus(event.orderId(), OrderStatus.PAYMENT_FAILED);
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception ex) {
            log.error("Discarding malformed {} payload", type.getSimpleName(), ex);
            return null;
        }
    }
}
