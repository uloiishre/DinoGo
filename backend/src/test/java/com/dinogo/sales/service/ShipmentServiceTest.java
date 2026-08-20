package com.dinogo.sales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.sales.dto.shipment.CreateShipmentRequest;
import com.dinogo.sales.dto.shipment.UpdateShipmentStatusRequest;
import com.dinogo.sales.dto.shipment.UpdateShipmentTrackingInfoRequest;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.repository.ShipmentRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private Seller seller;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(
                shipmentRepository, orderRepository, paymentRepository, sellerRepository);
    }

    @Test
    void sellerCreatesPreparingShipmentForPaidOrder() {
        Order order = order(10, 6, 30, OrderStatus.PAID);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(orderRepository.findForShipmentCreation(10, 30)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderOrderId(10)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = shipmentService.createShipment(
                10, 8, new CreateShipmentRequest("  Black Cat  ", "  TRACK-1  "));

        assertEquals(10, response.orderId());
        assertEquals("Black Cat", response.carrierName());
        assertEquals("TRACK-1", response.trackingNo());
        assertEquals(ShipmentStatus.PREPARING, response.status());
    }

    @Test
    void rejectsShipmentForUnpaidOrder() {
        Order order = order(10, 6, 30, OrderStatus.PENDING_PAYMENT);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(orderRepository.findForShipmentCreation(10, 30)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderException.class, () -> shipmentService.createShipment(
                10, 8, new CreateShipmentRequest(null, null)));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateShipment() {
        Order order = order(10, 6, 30, OrderStatus.PROCESSING);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(orderRepository.findForShipmentCreation(10, 30)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderOrderId(10)).thenReturn(true);

        assertThrows(InvalidOrderException.class, () -> shipmentService.createShipment(
                10, 8, new CreateShipmentRequest(null, null)));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void buyerCanGetShipment() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(shipmentRepository.findByOrderOrderId(10)).thenReturn(Optional.of(shipment));

        var response = shipmentService.getShipment(10, 6);

        assertEquals(10, response.orderId());
        assertNull(response.trackingNo());
        verify(sellerRepository, never()).findByMember_MemberId(any());
    }

    @Test
    void orderSellerCanGetShipment() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(shipmentRepository.findByOrderOrderId(10)).thenReturn(Optional.of(shipment));
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));

        var response = shipmentService.getShipment(10, 8);

        assertEquals(10, response.orderId());
    }

    @Test
    void unrelatedMemberCannotGetShipment() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(shipmentRepository.findByOrderOrderId(10)).thenReturn(Optional.of(shipment));
        when(sellerRepository.findByMember_MemberId(9)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> shipmentService.getShipment(10, 9));
    }

    @Test
    void shippedStatusMarksShipmentAndOrderShipped() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        var response = shipmentService.updateShipmentStatus(
                10, 8, new UpdateShipmentStatusRequest(ShipmentStatus.SHIPPED));

        assertEquals(ShipmentStatus.SHIPPED, response.status());
        assertNotNull(response.shippedAt());
        assertEquals(OrderStatus.SHIPPED, shipment.getOrder().getStatus());
    }

    @Test
    void availableForPickupSetsTimestampAndKeepsOrderShipped() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.SHIPPED));
        shipment.setStatus(ShipmentStatus.SHIPPED);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        var response = shipmentService.updateShipmentStatus(
                10, 8, new UpdateShipmentStatusRequest(
                        ShipmentStatus.AVAILABLE_FOR_PICKUP));

        assertEquals(ShipmentStatus.AVAILABLE_FOR_PICKUP, response.status());
        assertNotNull(response.availablePickupAt());
        assertEquals(OrderStatus.SHIPPED, shipment.getOrder().getStatus());
    }

    @Test
    void activeOrderSellerUpdatesNormalizedTrackingInfo() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        shipment.setCarrierName("Old Carrier");
        shipment.setTrackingNo("OLD-1");
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        var response = shipmentService.updateShipmentTrackingInfo(
                10, 8, new UpdateShipmentTrackingInfoRequest(
                        "  Black Cat  ", "  TRACK-2  "));

        assertEquals("Black Cat", response.carrierName());
        assertEquals("TRACK-2", response.trackingNo());
        assertEquals(ShipmentStatus.PREPARING, response.status());
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void completedShipmentTrackingInfoCannotBeChanged() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.COMPLETED));
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setCarrierName("Original Carrier");
        shipment.setTrackingNo("ORIGINAL-1");
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));

        assertThrows(InvalidOrderException.class,
                () -> shipmentService.updateShipmentTrackingInfo(
                        10, 8, new UpdateShipmentTrackingInfoRequest(
                                "Changed Carrier", "CHANGED-2")));

        assertEquals("Original Carrier", shipment.getCarrierName());
        assertEquals("ORIGINAL-1", shipment.getTrackingNo());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void unrelatedSellerCannotUpdateTrackingInfo() {
        when(seller.getSellerId()).thenReturn(31);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(9)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 31)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> shipmentService.updateShipmentTrackingInfo(
                        10, 9, new UpdateShipmentTrackingInfoRequest(
                                "Black Cat", "TRACK-2")));

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void buyerConfirmationDeliversShipmentAndCompletesOrder() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.SHIPPED));
        shipment.setStatus(ShipmentStatus.AVAILABLE_FOR_PICKUP);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(shipmentRepository.findForDeliveryConfirmation(10, 6))
                .thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                10, PaymentStatus.PENDING, "CASH_ON_DELIVERY"))
                .thenReturn(Optional.of(payment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        var response = shipmentService.confirmDelivery(10, 6);

        assertEquals(ShipmentStatus.DELIVERED, response.status());
        assertNotNull(response.deliveredAt());
        assertEquals(OrderStatus.COMPLETED, shipment.getOrder().getStatus());
        assertNotNull(shipment.getOrder().getCompletedAt());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNotNull(payment.getPaidAt());
    }

    @Test
    void retryingSameStatusReturnsExistingShipmentWithoutSaving() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.SHIPPED));
        shipment.setStatus(ShipmentStatus.SHIPPED);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));

        var response = shipmentService.updateShipmentStatus(
                10, 8, new UpdateShipmentStatusRequest(ShipmentStatus.SHIPPED));

        assertEquals(ShipmentStatus.SHIPPED, response.status());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void sellerCannotMarkShipmentDelivered() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));
        when(shipmentRepository.findForStatusUpdate(10, 30)).thenReturn(Optional.of(shipment));

        assertThrows(InvalidOrderException.class,
                () -> shipmentService.updateShipmentStatus(
                        10, 8, new UpdateShipmentStatusRequest(ShipmentStatus.DELIVERED)));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void inactiveSellerCannotCreateShipment() {
        when(seller.getStatus()).thenReturn("SUSPENDED");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));

        assertThrows(OrderNotFoundException.class, () -> shipmentService.createShipment(
                10, 8, new CreateShipmentRequest(null, null)));

        verify(orderRepository, never()).findForShipmentCreation(any(), any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void inactiveSellerCannotGetShipment() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.PROCESSING));
        when(shipmentRepository.findByOrderOrderId(10)).thenReturn(Optional.of(shipment));
        when(seller.getStatus()).thenReturn("SUSPENDED");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));

        assertThrows(OrderNotFoundException.class,
                () -> shipmentService.getShipment(10, 8));
    }

    @Test
    void inactiveSellerCannotUpdateShipmentStatus() {
        when(seller.getStatus()).thenReturn("SUSPENDED");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));

        assertThrows(OrderNotFoundException.class,
                () -> shipmentService.updateShipmentStatus(
                        10, 8, new UpdateShipmentStatusRequest(ShipmentStatus.SHIPPED)));

        verify(shipmentRepository, never()).findForStatusUpdate(any(), any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void retryingBuyerConfirmationReturnsDeliveredShipmentWithoutSaving() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.COMPLETED));
        shipment.setStatus(ShipmentStatus.DELIVERED);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(shipmentRepository.findForDeliveryConfirmation(10, 6))
                .thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                10, PaymentStatus.PENDING, "CASH_ON_DELIVERY"))
                .thenReturn(Optional.of(payment));

        var response = shipmentService.confirmDelivery(10, 6);

        assertEquals(ShipmentStatus.DELIVERED, response.status());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNotNull(payment.getPaidAt());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void deliveredShipmentRepairsLegacyShippedOrder() {
        Order order = order(10, 6, 30, OrderStatus.SHIPPED);
        Shipment shipment = shipment(order);
        LocalDateTime deliveredAt = LocalDateTime.of(2026, 8, 20, 9, 30);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(deliveredAt);
        when(shipmentRepository.findForDeliveryConfirmation(10, 6))
                .thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                10, PaymentStatus.PENDING, "CASH_ON_DELIVERY"))
                .thenReturn(Optional.empty());

        var response = shipmentService.confirmDelivery(10, 6);

        assertEquals(ShipmentStatus.DELIVERED, response.status());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(deliveredAt, order.getCompletedAt());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void deliveredShipmentRejectsIncompatibleOrderStatus() {
        Shipment shipment = shipment(order(10, 6, 30, OrderStatus.CANCELLED));
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findForDeliveryConfirmation(10, 6))
                .thenReturn(Optional.of(shipment));

        assertThrows(
                InvalidOrderException.class,
                () -> shipmentService.confirmDelivery(10, 6));

        verify(paymentRepository, never())
                .findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                        any(), any(), any());
    }

    @Test
    void unrelatedBuyerCannotConfirmDelivery() {
        when(shipmentRepository.findForDeliveryConfirmation(10, 9))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> shipmentService.confirmDelivery(10, 9));
    }

    private Order order(Integer orderId, Integer buyerId, Integer sellerId, OrderStatus status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setStatus(status);
        return order;
    }

    private Shipment shipment(Order order) {
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PREPARING);
        return shipment;
    }
}
