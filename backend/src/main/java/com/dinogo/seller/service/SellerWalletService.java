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
import com.dinogo.seller.dto.SellerWithdrawalResponse;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.entity.WithdrawalRequest;
import com.dinogo.seller.entity.WithdrawalStatus;
import com.dinogo.seller.repository.SellerRepository;
import com.dinogo.seller.repository.WithdrawalRequestRepository;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.repository.RecordRepository;
import com.dinogo.sysmsg.repository.SendRepository;

@Service
public class SellerWalletService {

    private static final Integer PLATFORM_SELLER_ID = 1;
    private static final String WITHDRAWAL_NOTIFICATION_FUNCTION = "OS-001";
    private static final Set<WithdrawalStatus> DEDUCTED_WITHDRAWAL_STATUSES = Set.of(
            WithdrawalStatus.PROCESSING,
            WithdrawalStatus.PAID);

    private static final Set<OrderStatus> PENDING_BALANCE_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED);

    private final CurrentSellerService currentSellerService;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final SendRepository sendRepository;
    private final RecordRepository recordRepository;

    public SellerWalletService(
            CurrentSellerService currentSellerService,
            OrderRepository orderRepository,
            SellerRepository sellerRepository,
            WithdrawalRequestRepository withdrawalRequestRepository,
            SendRepository sendRepository,
            RecordRepository recordRepository) {
        this.currentSellerService = currentSellerService;
        this.orderRepository = orderRepository;
        this.sellerRepository = sellerRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.sendRepository = sendRepository;
        this.recordRepository = recordRepository;
    }

    @Transactional(readOnly = true)
    public SellerWalletResponse getWallet(Integer memberId) {
        Integer sellerId = currentSellerService.requireActiveSellerId(memberId);
        return buildWallet(sellerId);
    }

    @Transactional
    public SellerWithdrawalResponse withdraw(Integer memberId) {
        Seller seller = sellerRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new IllegalArgumentException("Seller is inactive.");
        }

        SellerWalletResponse wallet = buildWallet(seller.getSellerId());
        BigDecimal amount = wallet.availableBalance();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("目前沒有可提領金額。");
        }

        WithdrawalRequest withdrawal = withdrawalRequestRepository.save(new WithdrawalRequest(seller, amount));
        createWithdrawalNotification(seller.getSellerId(), amount);

        return new SellerWithdrawalResponse(
                withdrawal.getWithdrawalId(),
                withdrawal.getAmount(),
                withdrawal.getStatus(),
                withdrawal.getRequestedAt(),
                buildWallet(seller.getSellerId()));
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

    private SellerWalletResponse buildWallet(Integer sellerId) {
        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);

        BigDecimal completedBalance = sumOrdersByStatus(orders, OrderStatus.COMPLETED);
        BigDecimal withdrawnBalance = withdrawalRequestRepository.sumAmountBySellerIdAndStatusIn(
                sellerId,
                DEDUCTED_WITHDRAWAL_STATUSES);
        if (withdrawnBalance == null) {
            withdrawnBalance = BigDecimal.ZERO;
        }
        BigDecimal availableBalance = completedBalance.subtract(withdrawnBalance).max(BigDecimal.ZERO);
        BigDecimal pendingBalance = orders.stream()
                .filter(order -> PENDING_BALANCE_STATUSES.contains(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerWalletResponse(
                availableBalance,
                pendingBalance,
                withdrawnBalance,
                "台新銀行",
                "0908",
                true);
    }

    private BigDecimal sumOrdersByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(order -> order.getStatus() == status)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createWithdrawalNotification(Integer sellerId, BigDecimal amount) {
        String formattedAmount = amount.stripTrailingZeros().toPlainString();
        SendEntity send = sendRepository.save(new SendEntity(
                PLATFORM_SELLER_ID,
                WITHDRAWAL_NOTIFICATION_FUNCTION,
                "提款申請通知",
                "提款申請已送出",
                "您的提款申請已送出，提款金額 NT$" + formattedAmount + "，平台將進行審核與撥款作業。",
                SendStatus.SEND));

        recordRepository.save(new RecordEntity(send, null, sellerId));
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
