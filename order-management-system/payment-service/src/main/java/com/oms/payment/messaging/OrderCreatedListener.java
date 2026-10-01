package com.oms.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.payment.messaging.PaymentEvents.OrderCreatedEvent;
import com.oms.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentProcessor paymentProcessor;

    public OrderCreatedListener(ObjectMapper objectMapper, PaymentProcessor paymentProcessor) {
        this.objectMapper = objectMapper;
        this.paymentProcessor = paymentProcessor;
    }

    @KafkaListener(topics = "${app.kafka.order-created}")
    public void onOrderCreated(String payload) {
        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (Exception ex) {
            log.error("Discarding malformed OrderCreated payload", ex);
            return;
        }
        if (event.orderId() == null) {
            log.error("Discarding OrderCreated payload without an order id");
            return;
        }
        log.info("Received OrderCreated for order {}", event.orderId());
        paymentProcessor.process(event);
    }
}
