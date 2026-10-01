package com.oms.order.api.dto;

import com.oms.order.domain.CustomerOrder;
import com.oms.order.domain.OrderItem;
import com.oms.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(
            @NotNull UUID productId,
            @NotNull @Positive @Max(1000) Integer quantity) {
    }

    public record CreateOrderRequest(
            @NotNull UUID userId,
            @NotEmpty @Size(max = 20) @Valid List<OrderItemRequest> items) {
    }

    public record UpdateOrderStatusRequest(
            @NotNull OrderStatus status) {
    }

    public record OrderItemResponse(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {

        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal());
        }
    }

    public record OrderResponse(
            UUID id,
            UUID userId,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant updatedAt) {

        public static OrderResponse from(CustomerOrder order) {
            return new OrderResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getCreatedAt(),
                    order.getUpdatedAt());
        }
    }

    public record ProductStockResponse(
            UUID productId,
            String name,
            BigDecimal price,
            int stockQuantity) {
    }
}
