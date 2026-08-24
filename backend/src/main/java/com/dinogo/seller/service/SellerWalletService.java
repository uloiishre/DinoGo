package com.dinogo.seller.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.dto.SellerWalletResponse;
import com.dinogo.seller.dto.SellerWalletTransactionResponse;

@Service
public class SellerWalletService {

    private static final Set<OrderStatus> PENDING_BALANCE_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED);

    private final CurrentSellerService currentSellerService;
    private final OrderRepository orderRepository;

    public SellerWalletService(
            CurrentSellerService currentSellerService,
            OrderRepository orderRepository) {
        this.currentSellerService = currentSellerService;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SellerWalletResponse getWallet(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        BigDecimal availableBalance = sumOrdersByStatus(orders, OrderStatus.COMPLETED);
        BigDecimal pendingBalance = orders.stream()
                .filter(order -> PENDING_BALANCE_STATUSES.contains(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerWalletResponse(
                availableBalance,
                pendingBalance,
                BigDecimal.ZERO,
                "台新銀行",
                "0908",
                true);
    }

    @Transactional(readOnly = true)
    public List<SellerWalletTransactionResponse> getTransactions(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);

        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(order -> order.getStatus() != OrderStatus.PENDING_PAYMENT)
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toTransactionResponse)
                .toList();
    }

    private BigDecimal sumOrdersByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(order -> order.getStatus() == status)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SellerWalletTransactionResponse toTransactionResponse(Order order) {
        boolean completed = order.getStatus() == OrderStatus.COMPLETED;

        return new SellerWalletTransactionResponse(
                order.getOrderId(),
                order.getOrderNo(),
                "ORDER_INCOME",
                "income",
                order.getTotalAmount(),
                completed ? "AVAILABLE" : "PENDING",
                completed && order.getCompletedAt() != null
                        ? order.getCompletedAt()
                        : order.getCreatedAt());
    }
}
