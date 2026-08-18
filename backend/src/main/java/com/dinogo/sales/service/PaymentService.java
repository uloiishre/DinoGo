package com.dinogo.sales.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.dto.payment.CreatePaymentRequest;
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.dto.payment.SimulatePaymentRequest;
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

    private static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
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

        Payment pendingPayment = paymentRepository
                .findFirstByOrderOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .orElse(null);

        boolean cashOnDeliveryRetry = order.getStatus() == OrderStatus.PROCESSING
                && CASH_ON_DELIVERY.equals(request.paymentMethodCode())
                && pendingPayment != null
                && CASH_ON_DELIVERY.equals(
                        pendingPayment.getPaymentMethod().getMethodCode());

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && !cashOnDeliveryRetry) {
            throw new InvalidOrderException(
                    "Only pending-payment orders can create a payment");
        }

        if (pendingPayment != null) {
            if (pendingPayment.getPaymentMethod().getMethodCode()
                    .equals(request.paymentMethodCode())) {
                return toResponse(pendingPayment);
            }
            throw new InvalidOrderException(
                    "A pending payment with a different method already exists for this order");
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

        if (CASH_ON_DELIVERY.equals(method.getMethodCode())) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        Payment savedPayment = paymentRepository.save(payment);
        return toResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse simulatePaymentResult(
            Integer orderId,
            Integer paymentId,
            Integer buyerId,
            SimulatePaymentRequest request) {

        Payment payment = paymentRepository
                .findForSimulation(paymentId, orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Payment does not exist"));

        if (request.status() != PaymentStatus.SUCCESS
                && request.status() != PaymentStatus.FAILED) {
            throw new InvalidOrderException("Simulated payment result must be SUCCESS or FAILED");
        }

        if (payment.getStatus() == request.status()) {
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOrderException("Payment already has a different result");
        }

        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderException("Order is not awaiting payment");
        }

        if (request.status() == PaymentStatus.SUCCESS) {
            boolean successExists = paymentRepository.existsByOrderOrderIdAndStatus(
                    orderId,
                    PaymentStatus.SUCCESS);
            if (successExists) {
                throw new InvalidOrderException("A successful payment already exists for this order");
            }

            LocalDateTime paidAt = LocalDateTime.now();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionNo(generateTransactionNo());
            payment.setFailureReason(null);
            payment.setPaidAt(paidAt);
            order.setStatus(OrderStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(normalizeFailureReason(request.failureReason()));
        }

        return toResponse(paymentRepository.save(payment));
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

    private String generateTransactionNo() {
        return "SIM-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "Simulated payment failed";
        }
        String normalized = failureReason.trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
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
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }
}
