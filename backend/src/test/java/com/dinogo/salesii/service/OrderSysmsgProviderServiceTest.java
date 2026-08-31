package com.dinogo.salesii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.ShipmentRepository;

//sysmsg-start，總共1次修改，第1次//
/** 驗證 Shipment DELIVERED 只形成 sysmsg 唯讀投影，不改寫 Sales Order。 */
@ExtendWith(MockitoExtension.class)
class OrderSysmsgProviderServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private ShipmentRepository shipments;

    @Test
    void projectsDeliveredShipmentWithoutChangingOrderStatus() {
        Order order = order(7, OrderStatus.SHIPPED);
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(orders.findById(7)).thenReturn(Optional.of(order));
        when(shipments.findByOrderOrderId(7)).thenReturn(Optional.of(shipment));

        OrderSysmsgProviderService provider = new OrderSysmsgProviderService(orders, shipments);

        assertEquals("DELIVERED", provider.getOrderForSysmsg(7).status());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void reviewReadKeepsRawOrderStatusAndDoesNotReadShipment() {
        Order order = order(8, OrderStatus.COMPLETED);
        when(orders.findById(8)).thenReturn(Optional.of(order));

        OrderSysmsgProviderService provider = new OrderSysmsgProviderService(orders, shipments);

        assertEquals("COMPLETED", provider.getOrder(8).status());
        verifyNoInteractions(shipments);
    }

    @Test
    void completedOrderTakesPriorityOverDeliveredProjection() {
        Order order = order(9, OrderStatus.COMPLETED);
        when(orders.findById(9)).thenReturn(Optional.of(order));

        OrderSysmsgProviderService provider = new OrderSysmsgProviderService(orders, shipments);

        assertEquals("COMPLETED", provider.getOrderForSysmsg(9).status());
        verifyNoInteractions(shipments);
    }

    @Test
    void projectsSuccessfulPaymentAsPaidWithoutChangingProcessingOrder() {
        Order order = order(10, OrderStatus.PROCESSING);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(1);
        method.setMethodName("信用卡");
        payment.setPaymentMethod(method);
        order.getPayments().add(payment);
        when(orders.findById(10)).thenReturn(Optional.of(order));
        when(shipments.findByOrderOrderId(10)).thenReturn(Optional.empty());

        OrderSysmsgProviderService provider = new OrderSysmsgProviderService(orders, shipments);

        assertEquals("PAID", provider.getOrderForSysmsg(10).status());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    @Test
    void paidSnapshotUsesSuccessfulPaymentInsteadOfNewerFailedPayment() {
        Order order = order(11, OrderStatus.PROCESSING);
        PaymentMethod successfulMethod = new PaymentMethod();
        successfulMethod.setPaymentMethodId(1);
        successfulMethod.setMethodName("信用卡");
        Payment successful = new Payment();
        successful.setPaymentId(100);
        successful.setStatus(PaymentStatus.SUCCESS);
        successful.setPaymentMethod(successfulMethod);

        PaymentMethod failedMethod = new PaymentMethod();
        failedMethod.setPaymentMethodId(2);
        failedMethod.setMethodName("轉帳");
        Payment failed = new Payment();
        failed.setPaymentId(101);
        failed.setStatus(PaymentStatus.FAILED);
        failed.setPaymentMethod(failedMethod);
        order.getPayments().add(successful);
        order.getPayments().add(failed);
        when(orders.findById(11)).thenReturn(Optional.of(order));
        when(shipments.findByOrderOrderId(11)).thenReturn(Optional.empty());

        OrderSysmsgResponse snapshot = new OrderSysmsgProviderService(orders, shipments)
                .getOrderForSysmsg(11);

        assertEquals("PAID", snapshot.status());
        assertEquals(1, snapshot.paymentMethodId());
        assertEquals("信用卡", snapshot.methodName());
    }

    private Order order(Integer id, OrderStatus status) {
        Order order = new Order();
        order.setOrderId(id);
        order.setOrderNo("ORD-" + id);
        order.setBuyerId(11);
        order.setSellerId(22);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.TEN);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 1, 12, 0));
        return order;
    }
}
//sysmsg-end，總共1次修改，第1次//

