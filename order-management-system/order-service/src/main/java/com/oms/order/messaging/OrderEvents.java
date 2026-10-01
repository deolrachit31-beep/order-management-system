package com.oms.order.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oms.order.domain.CustomerOrder;
import com.oms.order.domain.OrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderEvents {

    private OrderEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItemPayload(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreatedEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            BigDecimal totalAmount,
            String currency,
            List<OrderItemPayload> items,
            Instant occurredAt) {

        public static OrderCreatedEvent from(CustomerOrder order) {
            List<OrderItemPayload> items = order.getItems().stream()
                    .map(OrderCreatedEvent::item)
                    .toList();
            return new OrderCreatedEvent(
                    UUID.randomUUID(),
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    items,
                    Instant.now());
        }

        private static OrderItemPayload item(OrderItem item) {
            return new OrderItemPayload(item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentCompletedEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            Instant occurredAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentFailedEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String reason,
            Instant occurredAt) {
    }
}
