package com.dinogo.salesii.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.salesii.dto.OrderReviewItemResponse;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;

//rev+msg-start，總共1次修改，第1次//
/**
 * 單體應用內提供 review 與 sysmsg 所需訂單資料；不是前端 HTTP API。
 * 從正式 Order/Payment 資料取得權威狀態及發訊快照，呼叫端不得自行提供狀態。
 * 僅以 order.getStatus().name() 輸出字串，不引用 OrderStatus.DELIVERED 常數。
 */
@Service
public class OrderSysmsgProviderService {

    private final OrderRepository orderRepository;

    public OrderSysmsgProviderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderSysmsgResponse getOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));
        Payment payment = order.getPayments().stream()
                .max(java.util.Comparator.comparing(Payment::getPaymentId,
                        java.util.Comparator.nullsFirst(Integer::compareTo)))
                .orElse(null);

        return new OrderSysmsgResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getStatus().name(),
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
