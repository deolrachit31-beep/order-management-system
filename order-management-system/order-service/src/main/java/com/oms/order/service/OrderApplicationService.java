package com.oms.order.service;

import com.oms.order.api.dto.OrderDtos.CreateOrderRequest;
import com.oms.order.api.dto.OrderDtos.OrderItemRequest;
import com.oms.order.api.dto.OrderDtos.OrderResponse;
import com.oms.order.api.dto.OrderDtos.ProductStockResponse;
import com.oms.order.client.ProductStockClient;
import com.oms.order.domain.CustomerOrder;
import com.oms.order.domain.DomainExceptions.InvalidStatusTransitionException;
import com.oms.order.domain.DomainExceptions.NotFoundException;
import com.oms.order.domain.OrderCreationPolicy;
import com.oms.order.domain.OrderItem;
import com.oms.order.domain.OrderStatus;
import com.oms.order.domain.OrderStatusMachine;
import com.oms.order.domain.OrderTotals;
import com.oms.order.messaging.OrderEventPublisher;
import com.oms.order.repo.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final String CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final ProductStockClient productStockClient;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderCreationPolicy orderCreationPolicy;
    private final TransactionTemplate transactionTemplate;

    public OrderApplicationService(
            OrderRepository orderRepository,
            ProductStockClient productStockClient,
            OrderEventPublisher orderEventPublisher,
            OrderCreationPolicy orderCreationPolicy,
            PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.productStockClient = productStockClient;
        this.orderEventPublisher = orderEventPublisher;
        this.orderCreationPolicy = orderCreationPolicy;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OrderResponse create(CreateOrderRequest request) {
        orderCreationPolicy.validate(request.userId(), request.items());
        List<ReservedStock> reserved = reserveStock(request.items());
        CustomerOrder saved = null;
        try {
            saved = transactionTemplate.execute(status -> {
                CustomerOrder order = orderRepository.save(buildOrder(request.userId(), reserved));
                order.getItems().size();
                return order;
            });
            orderEventPublisher.publishOrderCreated(saved);
            log.info("Created order {} for user {} total {}", saved.getId(), saved.getUserId(), saved.getTotalAmount());
            return OrderResponse.from(saved);
        } catch (RuntimeException ex) {
            log.error("Order creation failed after reserving stock. Restoring stock.", ex);
            compensate(reserved);
            CustomerOrder persisted = saved;
            if (persisted != null && persisted.getId() != null) {
                try {
                    transactionTemplate.executeWithoutResult(status -> orderRepository.delete(persisted));
                } catch (RuntimeException deleteEx) {
                    log.error("Failed to delete order {} during compensation", persisted.getId(), deleteEx);
                }
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        order.getItems().size();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus target) {
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        OrderStatus previous = order.getStatus();
        OrderStatus next = OrderStatusMachine.transition(previous, target);
        if (next != previous) {
            order.setStatus(next);
            orderRepository.save(order);
            log.info("Order {} status changed from {} to {}", orderId, previous, next);
        } else {
            log.info("Order {} already has status {}", orderId, previous);
        }
        order.getItems().size();
        return OrderResponse.from(order);
    }

    @Transactional
    public void applyExternalStatus(UUID orderId, OrderStatus target) {
        try {
            updateStatus(orderId, target);
        } catch (InvalidStatusTransitionException ex) {
            log.warn("Ignored payment status {} for order {}: {}", target, orderId, ex.getMessage());
        }
    }

    private List<ReservedStock> reserveStock(List<OrderItemRequest> items) {
        List<ReservedStock> reserved = new ArrayList<>();
        try {
            for (OrderItemRequest item : items) {
                ProductStockResponse response = productStockClient.decrement(item.productId(), item.quantity());
                String name = response.name() == null || response.name().isBlank() ? "Product" : response.name();
                reserved.add(new ReservedStock(item.productId(), item.quantity(), name, response.price()));
                OrderTotals.lineTotal(response.price(), item.quantity());
            }
            return reserved;
        } catch (RuntimeException ex) {
            compensate(reserved);
            throw ex;
        }
    }

    private void compensate(List<ReservedStock> reserved) {
        for (int i = reserved.size() - 1; i >= 0; i--) {
            ReservedStock stock = reserved.get(i);
            try {
                productStockClient.increment(stock.productId(), stock.quantity());
                log.info("Restored {} units of product {}", stock.quantity(), stock.productId());
            } catch (RuntimeException ex) {
                log.error("Failed to restore stock for product {} quantity {}", stock.productId(), stock.quantity(), ex);
            }
        }
    }

    private CustomerOrder buildOrder(UUID userId, List<ReservedStock> reserved) {
        CustomerOrder order = new CustomerOrder();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCurrency(CURRENCY);
        List<BigDecimal> lineTotals = new ArrayList<>();
        for (ReservedStock stock : reserved) {
            BigDecimal lineTotal = OrderTotals.lineTotal(stock.unitPrice(), stock.quantity());
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(stock.productId());
            item.setProductName(stock.productName());
            item.setQuantity(stock.quantity());
            item.setUnitPrice(stock.unitPrice().setScale(2, RoundingMode.HALF_UP));
            item.setLineTotal(lineTotal);
            order.getItems().add(item);
            lineTotals.add(lineTotal);
        }
        order.setTotalAmount(OrderTotals.sum(lineTotals));
        return order;
    }

    private record ReservedStock(UUID productId, int quantity, String productName, BigDecimal unitPrice) {
    }
}
