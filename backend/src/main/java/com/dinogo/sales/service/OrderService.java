package com.dinogo.sales.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.member.entity.Address;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.dto.OrderListResponse;
import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.exception.OrderNotFoundException;

/** 訂單應用服務；建立訂單時保存商品與收件資料快照，暫不建立付款紀錄。 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final ProductSkuRepository productSkuRepository;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT,
            Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),

            OrderStatus.PAID,
            Set.of(OrderStatus.PROCESSING),

            OrderStatus.PROCESSING,
            Set.of(OrderStatus.SHIPPED),

            OrderStatus.SHIPPED,
            Set.of(OrderStatus.COMPLETED));

    public OrderService(
            OrderRepository orderRepository,
            AddressRepository addressRepository,
            ProductSkuRepository productSkuRepository) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.productSkuRepository = productSkuRepository;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new InvalidOrderException(
                        "Address does not exist: " + request.addressId()));
        if (!request.buyerId().equals(address.getMember().getMemberId())) {
            throw new InvalidOrderException("Address does not belong to buyer");
        }

        Order order = createOrderHeader(request, address);
        Set<Integer> skuIds = new HashSet<>();
        Integer sellerId = null;
        BigDecimal subtotalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            if (!skuIds.add(itemRequest.skuId())) {
                throw new InvalidOrderException("Duplicate SKU in order: " + itemRequest.skuId());
            }

            ProductSku sku = productSkuRepository.findById(itemRequest.skuId())
                    .orElseThrow(() -> new InvalidOrderException(
                            "SKU does not exist: " + itemRequest.skuId()));
            if (sku.getStatus() == null || sku.getStatus() != (byte) 1) {
                throw new InvalidOrderException("SKU is not available: " + itemRequest.skuId());
            }
            if (sku.getStock() < itemRequest.quantity()) {
                throw new InvalidOrderException("Insufficient stock for SKU: " + itemRequest.skuId());
            }

            Product product = sku.getProduct();
            Integer itemSellerId = product.getSeller().getSellerId();
            if (sellerId == null) {
                sellerId = itemSellerId;
            } else if (!sellerId.equals(itemSellerId)) {
                throw new InvalidOrderException("One order can only contain products from one seller");
            }

            BigDecimal itemSubtotal = sku.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            order.addOrderItem(createOrderItem(sku, itemRequest.quantity(), itemSubtotal));
            subtotalAmount = subtotalAmount.add(itemSubtotal);
            sku.setStock(sku.getStock() - itemRequest.quantity());
        }

        BigDecimal shippingFee = request.shippingFee() == null ? BigDecimal.ZERO : request.shippingFee();
        order.setSellerId(sellerId);
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotalAmount.add(shippingFee));

        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getMemberOrders(Integer buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMemberOrder(
            Integer orderId,
            Integer buyerId) {

        Order order = orderRepository
                .findByOrderIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse updateStatus(
            Integer orderId,
            OrderStatus targetStatus,
            String reason) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        validateStatusTransition(order.getStatus(), targetStatus);

        order.setStatus(targetStatus);

        if (targetStatus == OrderStatus.CANCELLED) {
            order.setCancelReason(reason);
            order.setCancelledAt(LocalDateTime.now());
        }

        if (targetStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse cancelOrder(
            Integer orderId,
            Integer buyerId,
            String reason) {

        Order order = orderRepository
                .findByOrderIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        validateStatusTransition(
                order.getStatus(),
                OrderStatus.CANCELLED);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledBy("BUYER");
        order.setCancelledAt(LocalDateTime.now());

        return toDetailResponse(order);
    }

    private void validateStatusTransition(
            OrderStatus current,
            OrderStatus target) {

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target)) {
            throw new InvalidOrderException(
                    "Invalid order status transition: "
                            + current + " -> " + target);
        }
    }

    private Order createOrderHeader(CreateOrderRequest request, Address address) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(request.buyerId());
        order.setAddressId(address.getAddressId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCity(address.getCity());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingDetailAddress(address.getDetailAddress());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setBuyerRemark(request.buyerRemark());
        return order;
    }

    private OrderItem createOrderItem(ProductSku sku, Integer quantity, BigDecimal subtotal) {
        Product product = sku.getProduct();
        OrderItem item = new OrderItem();
        item.setProductId(product.getProductId());
        item.setSkuId(sku.getSkuId());
        item.setProductName(product.getProductName());
        item.setSkuSpec(buildSkuSpec(sku));
        item.setProductImageUrl(findMainImageUrl(product));
        item.setUnitPrice(sku.getPrice());
        item.setQuantity(quantity);
        item.setSubtotal(subtotal);
        item.setIsReviewed(false);
        return item;
    }

    private String buildSkuSpec(ProductSku sku) {
        String first = formatSpec(sku.getSpec1Name(), sku.getSpec1Value());
        String second = formatSpec(sku.getSpec2Name(), sku.getSpec2Value());
        if (first == null)
            return second;
        if (second == null)
            return first;
        return first + " / " + second;
    }

    private String formatSpec(String name, String value) {
        if (value == null || value.isBlank())
            return null;
        return name == null || name.isBlank() ? value : name + ": " + value;
    }

    private String findMainImageUrl(Product product) {
        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_NO_TIME_FORMAT)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private CreateOrderResponse toResponse(Order order) {
        return new CreateOrderResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    private OrderListResponse toListResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderListResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getSellerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items);
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
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
                item.getIsReviewed());
    }
}
