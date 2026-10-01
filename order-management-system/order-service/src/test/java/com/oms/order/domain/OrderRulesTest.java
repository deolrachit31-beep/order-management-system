package com.oms.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oms.order.api.dto.OrderDtos.OrderItemRequest;
import com.oms.order.domain.DomainExceptions.BadRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderRulesTest {

    private final OrderCreationPolicy policy = new OrderCreationPolicy();

    @Test
    void acceptsASingleProductLine() {
        policy.validate(UUID.randomUUID(), List.of(new OrderItemRequest(UUID.randomUUID(), 1)));
    }

    @Test
    void rejectsMissingUser() {
        assertThatThrownBy(() -> policy.validate(null, List.of(new OrderItemRequest(UUID.randomUUID(), 1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User id is required");
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Order must contain at least one item");
        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        UUID productId = UUID.randomUUID();
        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), List.of(new OrderItemRequest(productId, 0))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity must be positive");
        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), List.of(new OrderItemRequest(productId, -1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsDuplicateProducts() {
        UUID productId = UUID.randomUUID();
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(productId, 1),
                new OrderItemRequest(productId, 2));

        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), items))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(productId.toString());
    }

    @Test
    void rejectsTooManyLinesOrQuantity() {
        List<OrderItemRequest> items = new ArrayList<>();
        IntStream.range(0, OrderCreationPolicy.MAX_ITEMS + 1)
                .forEach(index -> items.add(new OrderItemRequest(UUID.randomUUID(), 1)));

        assertThatThrownBy(() -> policy.validate(UUID.randomUUID(), items))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("20");

        assertThatThrownBy(() -> policy.validate(
                UUID.randomUUID(),
                List.of(new OrderItemRequest(UUID.randomUUID(), OrderCreationPolicy.MAX_QUANTITY_PER_LINE + 1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void lineTotalAndSumUseCurrencyScale() {
        assertThat(OrderTotals.lineTotal(new BigDecimal("12.50"), 2)).isEqualByComparingTo("25.00");
        assertThat(OrderTotals.lineTotal(new BigDecimal("0.10"), 3)).isEqualByComparingTo("0.30");
        assertThat(OrderTotals.sum(List.of(new BigDecimal("25.00"), new BigDecimal("3.50"))))
                .isEqualByComparingTo("28.50");
    }

    @Test
    void lineTotalRejectsNonPositivePriceOrQuantity() {
        assertThatThrownBy(() -> OrderTotals.lineTotal(BigDecimal.ZERO, 1))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> OrderTotals.lineTotal(null, 1))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> OrderTotals.lineTotal(new BigDecimal("1.00"), 0))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED,PAID",
            "CREATED,PAYMENT_FAILED",
            "CREATED,CANCELLED",
            "PAID,SHIPPED",
            "PAID,CANCELLED",
            "PAYMENT_FAILED,CANCELLED"
    })
    void allowsStatusTransitions(OrderStatus current, OrderStatus target) {
        assertThat(OrderStatusMachine.transition(current, target)).isEqualTo(target);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED,SHIPPED",
            "PAID,CREATED",
            "PAID,PAYMENT_FAILED",
            "PAYMENT_FAILED,PAID",
            "PAYMENT_FAILED,SHIPPED",
            "SHIPPED,CANCELLED",
            "SHIPPED,PAID",
            "CANCELLED,CREATED",
            "CANCELLED,PAID"
    })
    void rejectsStatusTransitions(OrderStatus current, OrderStatus target) {
        assertThatThrownBy(() -> OrderStatusMachine.transition(current, target))
                .isInstanceOf(DomainExceptions.InvalidStatusTransitionException.class)
                .hasMessageContaining(current.name())
                .hasMessageContaining(target.name());
    }

    @Test
    void repeatingTheCurrentStatusIsIdempotent() {
        assertThat(OrderStatusMachine.transition(OrderStatus.PAID, OrderStatus.PAID)).isEqualTo(OrderStatus.PAID);
        assertThat(OrderStatusMachine.transition(OrderStatus.PAYMENT_FAILED, OrderStatus.PAYMENT_FAILED))
                .isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void nullStatusIsRejected() {
        assertThatThrownBy(() -> OrderStatusMachine.transition(null, OrderStatus.PAID))
                .isInstanceOf(DomainExceptions.InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> OrderStatusMachine.transition(OrderStatus.CREATED, null))
                .isInstanceOf(DomainExceptions.InvalidStatusTransitionException.class);
    }
}
