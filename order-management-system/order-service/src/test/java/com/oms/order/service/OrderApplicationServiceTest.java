package com.oms.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oms.order.api.dto.OrderDtos.CreateOrderRequest;
import com.oms.order.api.dto.OrderDtos.OrderItemRequest;
import com.oms.order.api.dto.OrderDtos.OrderResponse;
import com.oms.order.api.dto.OrderDtos.ProductStockResponse;
import com.oms.order.client.ProductStockClient;
import com.oms.order.domain.CustomerOrder;
import com.oms.order.domain.DomainExceptions.BadRequestException;
import com.oms.order.domain.DomainExceptions.ConflictException;
import com.oms.order.domain.DomainExceptions.ServiceUnavailableException;
import com.oms.order.domain.OrderCreationPolicy;
import com.oms.order.domain.OrderStatus;
import com.oms.order.messaging.OrderEventPublisher;
import com.oms.order.repo.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductStockClient productStockClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private OrderApplicationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID firstProductId = UUID.randomUUID();
    private final UUID secondProductId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        service = new OrderApplicationService(
                orderRepository,
                productStockClient,
                orderEventPublisher,
                new OrderCreationPolicy(),
                transactionManager);
    }

    @Test
    void createReservesStockPersistsOrderAndPublishesEvent() {
        when(productStockClient.decrement(firstProductId, 2))
                .thenReturn(stock(firstProductId, "Notebook", "12.50", 8));
        when(productStockClient.decrement(secondProductId, 1))
                .thenReturn(stock(secondProductId, "Pen", "3.50", 10));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        OrderResponse response = service.create(new CreateOrderRequest(userId, List.of(
                new OrderItemRequest(firstProductId, 2),
                new OrderItemRequest(secondProductId, 1))));

        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("28.50");
        assertThat(response.items()).hasSize(2);
        assertThat(response.currency()).isEqualTo("USD");
        verify(orderEventPublisher).publishOrderCreated(any(CustomerOrder.class));
        verify(productStockClient, never()).increment(any(), anyInt());
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void duplicateProductIsRejectedBeforeStockIsReserved() {
        assertThatThrownBy(() -> service.create(new CreateOrderRequest(userId, List.of(
                new OrderItemRequest(firstProductId, 1),
                new OrderItemRequest(firstProductId, 1)))))
                .isInstanceOf(BadRequestException.class);

        verify(productStockClient, never()).decrement(any(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void failedSecondReservationRestoresTheFirstProduct() {
        when(productStockClient.decrement(firstProductId, 1))
                .thenReturn(stock(firstProductId, "Notebook", "10.00", 4));
        when(productStockClient.decrement(secondProductId, 2))
                .thenThrow(new ConflictException("Insufficient stock for product " + secondProductId));

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(userId, List.of(
                new OrderItemRequest(firstProductId, 1),
                new OrderItemRequest(secondProductId, 2)))))
                .isInstanceOf(ConflictException.class);

        verify(productStockClient).increment(firstProductId, 1);
        verify(productStockClient, never()).increment(secondProductId, 2);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void invalidPriceRollsBackTheReservation() {
        when(productStockClient.decrement(firstProductId, 1))
                .thenReturn(stock(firstProductId, "Notebook", "0.00", 4));

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(
                userId, List.of(new OrderItemRequest(firstProductId, 1)))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unit price must be positive");

        verify(productStockClient).increment(firstProductId, 1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void publishFailureRestoresStockAndDeletesTheOrder() {
        when(productStockClient.decrement(firstProductId, 1))
                .thenReturn(stock(firstProductId, "Notebook", "10.00", 4));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        org.mockito.Mockito.doThrow(new ServiceUnavailableException("kafka down", null))
                .when(orderEventPublisher).publishOrderCreated(any());

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(
                userId, List.of(new OrderItemRequest(firstProductId, 1)))))
                .isInstanceOf(ServiceUnavailableException.class);

        verify(productStockClient).increment(firstProductId, 1);
        verify(orderRepository).delete(any(CustomerOrder.class));
    }

    private static ProductStockResponse stock(UUID productId, String name, String price, int remaining) {
        return new ProductStockResponse(productId, name, new BigDecimal(price), remaining);
    }
}
