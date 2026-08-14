package com.dinogo.sales.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentMethodRepository;
import com.dinogo.sales.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final DateTimeFormatter PAYMENT_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository,
            OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PaymentResponse createPayment(
            Integer orderId,
            Integer buyerId,
            CreatePaymentRequest request) {

        Order order = orderRepository
                .findForPaymentCreation(orderId, buyerId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order does not exist"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderException(
                    "Only pending-payment orders can create a payment");
        }

        boolean pendingExists =
                paymentRepository.existsByOrderOrderIdAndStatus(
                        orderId,
                        PaymentStatus.PENDING);

        if (pendingExists) {
            throw new InvalidOrderException(
                    "A pending payment already exists for this order");
        }

        PaymentMethod method = paymentMethodRepository
                .findByMethodCode(request.paymentMethodCode())
                .orElseThrow(() ->
                        new InvalidOrderException(
                                "Payment method does not exist: "
                                        + request.paymentMethodCode()));

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrder(order);
        payment.setPaymentMethod(method);

        // 金額只能使用後端訂單總金額。
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);
        return toResponse(savedPayment);
    }

    private String generatePaymentNo() {
        return "PAY"
                + LocalDateTime.now().format(PAYMENT_NO_FORMAT)
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getPaymentNo(),
                payment.getOrder().getOrderId(),
                payment.getPaymentMethod().getMethodCode(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionNo(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }
}