package com.oms.order.domain;

import com.oms.order.api.dto.OrderDtos.OrderItemRequest;
import com.oms.order.domain.DomainExceptions.BadRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderCreationPolicy {

    public static final int MAX_ITEMS = 20;
    public static final int MAX_QUANTITY_PER_LINE = 1000;

    public void validate(UUID userId, List<OrderItemRequest> items) {
        if (userId == null) {
            throw new BadRequestException("User id is required");
        }
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }
        if (items.size() > MAX_ITEMS) {
            throw new BadRequestException("Order cannot contain more than " + MAX_ITEMS + " items");
        }
        Set<UUID> seen = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (item == null || item.productId() == null) {
                throw new BadRequestException("Product id is required");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Quantity must be positive");
            }
            if (item.quantity() > MAX_QUANTITY_PER_LINE) {
                throw new BadRequestException("Quantity cannot exceed " + MAX_QUANTITY_PER_LINE);
            }
            if (!seen.add(item.productId())) {
                throw new BadRequestException("Duplicate product in order: " + item.productId());
            }
        }
    }
}
