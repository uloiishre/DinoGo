package com.dinogo.salesii.service;

//rev+msg-start，總共1次修改，第1次//
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.salesii.dto.OrderReviewItemResponse;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.ShipmentRepository;
/**
 * 單體應用內提供 review 與 sysmsg 所需訂單資料；不是前端 HTTP API。
 * 從正式 Order/Payment/Shipment 資料取得權威唯讀快照，呼叫端不得自行提供狀態。
 * Review 保留 Order 原始狀態；Sysmsg 可將成功付款投影為 PAID，
 * 或將 Shipment DELIVERED 投影為 DELIVERED，但不修改 Sales 狀態機。
 */
@Service
public class OrderSysmsgProviderService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    public OrderSysmsgProviderService(
            OrderRepository orderRepository,
            ShipmentRepository shipmentRepository) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional(readOnly = true)
    public OrderSysmsgResponse getOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));
        return toResponse(order, order.getStatus().name());
    }

    /**
     * Sysmsg 專用唯讀投影：成功付款後的 PROCESSING 回傳 PAID，
     * Shipment 已送達時回傳 DELIVERED，其餘維持 Order.status；不寫回 Entity。
     */
    @Transactional(readOnly = true)
    public OrderSysmsgResponse getOrderForSysmsg(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));
        String notificationStatus = effectiveNotificationStatus(order);
        return toResponse(order, notificationStatus);
    }

    private String effectiveNotificationStatus(Order order) {
        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            return order.getStatus().name();
        }
        boolean delivered = shipmentRepository.findByOrderOrderId(order.getOrderId())
                .filter(shipment -> shipment.getStatus() == ShipmentStatus.DELIVERED)
                .isPresent();
        if (delivered) {
            return "DELIVERED";
        }
        boolean paymentSucceeded = order.getPayments().stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.SUCCESS);
        if (paymentSucceeded && order.getStatus() == OrderStatus.PROCESSING) {
            return "PAID";
        }
        return order.getStatus().name();
    }

    private OrderSysmsgResponse toResponse(Order order, String status) {
        java.util.stream.Stream<Payment> paymentCandidates = order.getPayments().stream();
        if ("PAID".equals(status)) {
            paymentCandidates = paymentCandidates
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS);
        }
        Payment payment = paymentCandidates
                .max(java.util.Comparator.comparing(Payment::getPaymentId,
                        java.util.Comparator.nullsFirst(Integer::compareTo)))
                .orElse(null);

        return new OrderSysmsgResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                status,
                order.getOrderItems().stream()
                        .map(item -> new OrderReviewItemResponse(
                                item.getOrderItemId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getProductImageUrl(),
                                item.getUnitPrice()))
                        .toList(),
                order.getTotalAmount(),
                payment == null ? null : payment.getPaymentMethod().getPaymentMethodId(),
                payment == null ? null : payment.getPaymentMethod().getMethodName(),
                order.getCreatedAt(),
                order.getCancelReason(),
                order.getCancelledAt());
    }
}
//rev+msg-end，總共1次修改，第1次//
