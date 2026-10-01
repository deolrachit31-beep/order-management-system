package com.oms.payment.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentEvents {

    private PaymentEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreatedEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            BigDecimal totalAmount,
            String currency,
            Instant occurredAt) {
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
