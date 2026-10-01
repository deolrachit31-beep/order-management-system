package com.oms.notification.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public final class NotificationMessages {

    private NotificationMessages() {
    }

    public static NotificationStatus statusFor(UUID orderId, UUID userId) {
        if (orderId == null || userId == null) {
            return NotificationStatus.FAILED;
        }
        return NotificationStatus.SENT;
    }

    public static String build(
            NotificationEventType type,
            UUID orderId,
            BigDecimal amount,
            String currency,
            String reason) {
        String money = (currency == null || currency.isBlank() ? "USD" : currency) + " " + scale(amount);
        return switch (type) {
            case ORDER_CREATED -> "Order " + orderId + " was created. Total " + money + ".";
            case PAYMENT_COMPLETED -> "Payment for order " + orderId + " completed. Amount " + money + ".";
            case PAYMENT_FAILED -> "Payment for order " + orderId + " failed. "
                    + (reason == null || reason.isBlank() ? "Payment was declined." : reason);
        };
    }

    private static String scale(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
