package com.dinogo.sales.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.dto.order.OrderDetailResponse;
import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.dto.order.OrderSummaryResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.exception.ResourceNotFoundException;
import com.dinogo.port.cart.CheckoutCartPort;
import com.dinogo.port.coupon.CouponPort;
import com.dinogo.port.coupon.DiscountResult;
import com.dinogo.port.inventory.OrderSkuSnapshot;
import com.dinogo.port.inventory.ProductInventoryPort;
import com.dinogo.port.member.CurrentMemberProvider;
import com.dinogo.sales.repository.OrderRepository;

/**
 * 訂單應用服務，集中處理下單、庫存、優惠券、購物車清除與訂單狀態轉換。
 * 跨模組依賴皆透過 port 介面存取，避免直接耦合其他模組的 Entity 或 Repository。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            Set.of(OrderStatus.PENDING_PAYMENT);
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID),
            OrderStatus.PAID, Set.of(OrderStatus.PROCESSING),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final ProductInventoryPort productInventoryPort;
    private final CurrentMemberProvider currentMemberProvider;
    private final CouponPort couponPort;
    private final Optional<CheckoutCartPort> checkoutCartPort;

    public OrderService(
            OrderRepository orderRepository,
            ProductInventoryPort productInventoryPort,
            CurrentMemberProvider currentMemberProvider,
            CouponPort couponPort,
            Optional<CheckoutCartPort> checkoutCartPort) {
        this.orderRepository = orderRepository;
        this.productInventoryPort = productInventoryPort;
        this.currentMemberProvider = currentMemberProvider;
        this.couponPort = couponPort;
        this.checkoutCartPort = checkoutCartPort;
    }

    /**
     * 建立訂單Transactional；會先驗證並扣除庫存、計算優惠，再保存訂單及清除已結帳購物車項目。
     * 任一步驟失敗時由交易機制回滾本模組的資料異動。
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validateRequest(request);
        CheckoutCartPort cartPort = requireCheckoutCartPort(request.cartItemIds());

        Order order = createOrderHeader(request);
        Integer sellerId = null;
        BigDecimal subtotalAmount = BigDecimal.ZERO;
        Map<Integer, Integer> quantitiesBySku = aggregateItems(request.items());
        List<OrderSkuSnapshot> skuSnapshots = productInventoryPort.validateAndDeduct(quantitiesBySku);

        for (OrderSkuSnapshot sku : skuSnapshots) {
            sellerId = sku.sellerId();
            OrderItem orderItem = createOrderItem(sku, quantitiesBySku.get(sku.skuId()));
            order.addOrderItem(orderItem);
            subtotalAmount = subtotalAmount.add(orderItem.getSubtotal());
        }

        order.setSellerId(sellerId);
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingFee(BigDecimal.ZERO);
        DiscountResult discountResult = couponPort.validateAndReserve(
                request.buyerId(),
                request.couponId(),
                sellerId,
                subtotalAmount);
        BigDecimal amountBeforeDiscount = subtotalAmount.add(order.getShippingFee());
        BigDecimal discountAmount = discountResult.discountAmount();
        if (discountAmount.compareTo(amountBeforeDiscount) > 0) {
            throw new IllegalArgumentException("Discount amount must not exceed order amount");
        }
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(amountBeforeDiscount.subtract(discountAmount));

        Order savedOrder = orderRepository.save(order);
        if (cartPort != null) {
            cartPort.clearCheckedOutItems(
                    request.buyerId(),
                    List.copyOf(request.cartItemIds()),
                    savedOrder.getOrderId());
        }
        return toResponse(savedOrder);
    }

    /** 依主鍵查詢訂單；供已完成額外授權判斷的呼叫端使用。 */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist: " + orderId));
        return toDetailResponse(order);
    }

    /** 查詢會員自己的訂單；非訂單買家時以不存在回應，避免洩漏訂單資訊。 */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderForMember(Integer orderId, Integer authenticatedMemberId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
        if (authenticatedMemberId == null || authenticatedMemberId <= 0) {
            throw new IllegalArgumentException("Authenticated member ID must be positive");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist: " + orderId));
        if (!authenticatedMemberId.equals(order.getBuyerId())) {
            throw new ResourceNotFoundException("Order does not exist: " + orderId);
        }
        return toDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getCurrentMemberOrder(Integer orderId) {
        return getOrderForMember(orderId, currentMemberProvider.requireMemberId());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMemberOrders(Integer memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Member ID must be positive");
        }

        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getCurrentMemberOrders() {
        return getMemberOrders(currentMemberProvider.requireMemberId());
    }

    /** 按照允許的單向狀態轉換規則推進訂單。 */
    @Transactional
    public OrderDetailResponse updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New order status is required");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Use cancelOrder to cancel an order");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist: " + orderId));
        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedStatuses.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid order status transition: " + currentStatus + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        return toDetailResponse(order);
    }

    /** 取消待付款訂單、回補庫存，並保存取消稽核資訊。 */
    @Transactional
    public OrderDetailResponse cancelOrder(Integer orderId, String cancelReason, String cancelledBy) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new IllegalArgumentException("Cancel reason is required");
        }
        if (cancelReason.length() > 500) {
            throw new IllegalArgumentException("Cancel reason must not exceed 500 characters");
        }
        if (!Set.of("BUYER", "SELLER", "SYSTEM").contains(cancelledBy)) {
            throw new IllegalArgumentException("Cancelled by must be BUYER, SELLER, or SYSTEM");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist: " + orderId));
        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new IllegalArgumentException(
                    "Order cannot be cancelled when status is: " + order.getStatus());
        }

        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(cancelReason.trim());
        order.setCancelledBy(cancelledBy);
        order.setCancelledAt(LocalDateTime.now());
        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse cancelCurrentMemberOrder(Integer orderId, String cancelReason) {
        Integer memberId = currentMemberProvider.requireMemberId();
        getOrderForMember(orderId, memberId);
        return cancelOrder(orderId, cancelReason, "BUYER");
    }

    private void restoreStock(Order order) {
        // 同一 SKU 可能出現在多筆明細中，回補前先合併數量。
        Map<Integer, Integer> quantitiesBySku = new LinkedHashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            try {
                quantitiesBySku.merge(item.getSkuId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalStateException(
                        "Quantity overflow while restoring SKU: " + item.getSkuId(), exception);
            }
        }
        productInventoryPort.restore(quantitiesBySku);
    }

    private Map<Integer, Integer> aggregateItems(List<CreateOrderItemRequest> items) {
        // 保留輸入順序，讓庫存驗證與錯誤結果維持穩定且容易追蹤。
        Map<Integer, Integer> quantitiesBySku = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : items) {
            try {
                quantitiesBySku.merge(item.skuId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Quantity is too large for SKU: " + item.skuId(), exception);
            }
        }
        return quantitiesBySku;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request is required");
        }
        if (request.buyerId() == null) {
            throw new IllegalArgumentException("Buyer is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (request.items().stream().anyMatch(item -> item == null
                || item.skuId() == null
                || item.quantity() == null
                || item.quantity() <= 0)) {
            throw new IllegalArgumentException("Each order item requires a SKU and a positive quantity");
        }
        if (request.cartItemIds() != null
                && request.cartItemIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Cart item IDs must be positive");
        }
    }

    private CheckoutCartPort requireCheckoutCartPort(List<Integer> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return null;
        }
        return checkoutCartPort.orElseThrow(() -> new IllegalStateException(
                "Checkout cart integration is not configured"));
    }

    private Order createOrderHeader(CreateOrderRequest request) {
        // 收件資料存成訂單快照，避免會員日後修改地址影響歷史訂單。
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(request.buyerId());
        order.setAddressId(request.addressId());
        order.setReceiverName(request.receiverName());
        order.setReceiverPhone(request.receiverPhone());
        order.setShippingPostalCode(request.shippingPostalCode());
        order.setShippingCity(request.shippingCity());
        order.setShippingDistrict(request.shippingDistrict());
        order.setShippingDetailAddress(request.shippingDetailAddress());
        order.setBuyerRemark(request.buyerRemark());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        return order;
    }

    private OrderItem createOrderItem(OrderSkuSnapshot sku, int quantity) {
        // 商品名稱、規格、圖片及價格皆為下單當下快照。
        BigDecimal subtotal = sku.unitPrice().multiply(BigDecimal.valueOf(quantity));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(sku.productId());
        orderItem.setSkuId(sku.skuId());
        orderItem.setProductName(sku.productName());
        orderItem.setSkuSpec(sku.skuSpec());
        orderItem.setProductImageUrl(sku.productImageUrl());
        orderItem.setUnitPrice(sku.unitPrice());
        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(subtotal);
        orderItem.setIsReviewed(false);
        return orderItem;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + timestamp + suffix;
    }

    private CreateOrderResponse toResponse(Order order) {
        return new CreateOrderResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount());
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderDetailResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getAddressId(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingPostalCode(),
                order.getShippingCity(),
                order.getShippingDistrict(),
                order.getShippingDetailAddress(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getBuyerRemark(),
                order.getCancelReason(),
                order.getCancelledBy(),
                order.getCancelledAt(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getProductId(),
                item.getSkuId(),
                item.getProductName(),
                item.getSkuSpec(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getIsReviewed(),
                item.getCreatedAt());
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getSellerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items);
    }
}
