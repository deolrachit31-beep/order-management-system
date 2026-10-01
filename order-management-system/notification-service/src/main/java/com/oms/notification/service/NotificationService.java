package com.oms.notification.service;

import com.oms.notification.domain.Notification;
import com.oms.notification.domain.NotificationEventType;
import com.oms.notification.domain.NotificationMessages;
import com.oms.notification.domain.NotificationStatus;
import com.oms.notification.repo.NotificationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification record(
            UUID orderId,
            UUID userId,
            NotificationEventType eventType,
            BigDecimal amount,
            String currency,
            String reason) {
        if (orderId == null || eventType == null) {
            throw new IllegalArgumentException("Order id and event type are required");
        }
        if (notificationRepository.existsByOrderIdAndEventType(orderId, eventType)) {
            log.info("Notification already stored for order {} event {}", orderId, eventType);
            return notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                    .filter(notification -> notification.getEventType() == eventType)
                    .findFirst()
                    .orElseThrow();
        }
        Notification notification = new Notification();
        notification.setOrderId(orderId);
        notification.setUserId(userId);
        notification.setEventType(eventType);
        notification.setChannel("EMAIL");
        notification.setMessage(NotificationMessages.build(eventType, orderId, amount, currency, reason));
        NotificationStatus status = NotificationMessages.statusFor(orderId, userId);
        notification.setStatus(status);
        try {
            Notification saved = notificationRepository.saveAndFlush(notification);
            if (status == NotificationStatus.SENT) {
                log.info("Simulated email notification {} for order {} to user {}", eventType, orderId, userId);
            } else {
                log.warn("Notification {} for order {} was not sent because the recipient is unknown", eventType, orderId);
            }
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate notification for order {} event {}", orderId, eventType);
            return notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                    .filter(existing -> existing.getEventType() == eventType)
                    .findFirst()
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> findByOrder(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
}
