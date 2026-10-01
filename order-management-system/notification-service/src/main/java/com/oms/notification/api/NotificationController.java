package com.oms.notification.api;

import com.oms.notification.api.dto.NotificationResponse;
import com.oms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List simulated notifications recorded for an order")
    public List<NotificationResponse> byOrder(@RequestParam UUID orderId) {
        return notificationService.findByOrder(orderId).stream().map(NotificationResponse::from).toList();
    }
}
