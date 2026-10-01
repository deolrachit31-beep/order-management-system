package com.oms.notification.repo;

import com.oms.notification.domain.Notification;
import com.oms.notification.domain.NotificationEventType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByOrderIdAndEventType(UUID orderId, NotificationEventType eventType);

    List<Notification> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
