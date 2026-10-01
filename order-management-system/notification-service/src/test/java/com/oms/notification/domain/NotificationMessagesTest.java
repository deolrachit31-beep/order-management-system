package com.oms.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMessagesTest {

    private final UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void sendsWhenTheOrderAndUserAreKnown() {
        assertThat(NotificationMessages.statusFor(orderId, userId)).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void failsWhenTheRecipientIsUnknown() {
        assertThat(NotificationMessages.statusFor(orderId, null)).isEqualTo(NotificationStatus.FAILED);
        assertThat(NotificationMessages.statusFor(null, userId)).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void messagesDescribeEachEvent() {
        assertThat(NotificationMessages.build(
                NotificationEventType.ORDER_CREATED, orderId, new BigDecimal("25"), "USD", null))
                .isEqualTo("Order " + orderId + " was created. Total USD 25.00.");
        assertThat(NotificationMessages.build(
                NotificationEventType.PAYMENT_COMPLETED, orderId, new BigDecimal("25.00"), null, null))
                .isEqualTo("Payment for order " + orderId + " completed. Amount USD 25.00.");
        assertThat(NotificationMessages.build(
                NotificationEventType.PAYMENT_FAILED, orderId, new BigDecimal("6000.00"), "USD",
                "Amount exceeds payment limit of 5000.00"))
                .contains("failed")
                .contains("Amount exceeds payment limit of 5000.00");
    }
}
