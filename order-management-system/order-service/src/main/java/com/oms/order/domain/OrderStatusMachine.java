package com.oms.order.domain;

import com.oms.order.domain.DomainExceptions.InvalidStatusTransitionException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStatusMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.CREATED, EnumSet.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_FAILED, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    private OrderStatusMachine() {
    }

    public static OrderStatus transition(OrderStatus current, OrderStatus target) {
        if (current == null || target == null) {
            throw new InvalidStatusTransitionException("Order status is required");
        }
        if (current == target) {
            return current;
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change order status from " + current + " to " + target);
        }
        return target;
    }
}
