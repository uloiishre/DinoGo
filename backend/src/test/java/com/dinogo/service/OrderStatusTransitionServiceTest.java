package com.dinogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.port.cart.CheckoutCartPort;
import com.dinogo.port.coupon.CouponPort;
import com.dinogo.port.inventory.ProductInventoryPort;
import com.dinogo.port.member.CurrentMemberProvider;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductInventoryPort productInventoryPort;

    @Mock
    private CurrentMemberProvider currentMemberProvider;

    @Mock
    private CouponPort couponPort;

    private OrderService createService() {
        return new OrderService(
                orderRepository,
                productInventoryPort,
                currentMemberProvider,
                couponPort,
                Optional.<CheckoutCartPort>empty());
    }

    @Test
    void shippedOrderCanMoveToDelivered() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(99)).thenReturn(Optional.of(order));

        createService().updateOrderStatus(99, OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getCompletedAt()).isNull();
    }

    @Test
    void deliveredOrderCanMoveToCompleted() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(99)).thenReturn(Optional.of(order));

        createService().updateOrderStatus(99, OrderStatus.COMPLETED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    void shippedOrderCannotSkipDelivered() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(99)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> createService().updateOrderStatus(99, OrderStatus.COMPLETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid order status transition: SHIPPED -> COMPLETED");
    }

    @Test
    void deliveredOrderCannotMoveBackToPaid() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(99)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> createService().updateOrderStatus(99, OrderStatus.PAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid order status transition: DELIVERED -> PAID");
    }

    private Order orderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setOrderId(99);
        order.setStatus(status);
        return order;
    }
}
