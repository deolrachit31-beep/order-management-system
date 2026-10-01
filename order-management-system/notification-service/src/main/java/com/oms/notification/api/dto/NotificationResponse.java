package com.oms.notification.api.dto;

import com.oms.notification.domain.Notification;
import com.oms.notification.domain.NotificationEventType;
import com.oms.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        NotificationEventType eventType,
        String channel,
        String message,
        NotificationStatus status,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrderId(),
                notification.getUserId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt());
    }
}
