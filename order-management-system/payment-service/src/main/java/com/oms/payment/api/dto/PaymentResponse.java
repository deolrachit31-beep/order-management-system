package com.oms.payment.api.dto;

import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String reason,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
