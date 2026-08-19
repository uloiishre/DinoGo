package com.dinogo.sales.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sales.dto.shipment.CreateShipmentRequest;
import com.dinogo.sales.dto.shipment.ShipmentResponse;
import com.dinogo.sales.dto.shipment.UpdateShipmentStatusRequest;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
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

@Service
public class ShipmentService {

    private static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
    private static final Set<OrderStatus> SHIPMENT_CREATION_STATUSES =
            Set.of(OrderStatus.PAID, OrderStatus.PROCESSING);
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ShipmentStatus.PREPARING, Set.of(ShipmentStatus.SHIPPED),
            ShipmentStatus.SHIPPED, Set.of(ShipmentStatus.AVAILABLE_FOR_PICKUP));

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SellerRepository sellerRepository;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            SellerRepository sellerRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public ShipmentResponse createShipment(
            Integer orderId,
            Integer memberId,
            CreateShipmentRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Order order = orderRepository
                .findForShipmentCreation(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        if (!SHIPMENT_CREATION_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderException(
                    "Only paid or processing orders can create a shipment");
        }
        if (shipmentRepository.existsByOrderOrderId(orderId)) {
            throw new InvalidOrderException("Shipment already exists for this order");
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCarrierName(normalize(request.carrierName()));
        shipment.setTrackingNo(normalize(request.trackingNo()));
        shipment.setStatus(ShipmentStatus.PREPARING);

        return toResponse(shipmentRepository.save(shipment));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(Integer orderId, Integer memberId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        Order order = shipment.getOrder();

        if (!memberId.equals(order.getBuyerId()) && !isOrderSeller(order, memberId)) {
            throw new OrderNotFoundException("Shipment does not exist");
        }

        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(
            Integer orderId,
            Integer memberId,
            UpdateShipmentStatusRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Shipment shipment = shipmentRepository
                .findForStatusUpdate(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return toResponse(shipment);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new InvalidOrderException(
                    "Invalid shipment status transition: "
                            + currentStatus + " -> " + targetStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = shipment.getOrder();
        if (targetStatus == ShipmentStatus.SHIPPED) {
            if (order.getStatus() != OrderStatus.PROCESSING) {
                throw new InvalidOrderException(
                        "Order must be processing before shipment can be shipped");
            }
            shipment.setShippedAt(now);
            order.setStatus(OrderStatus.SHIPPED);
        } else if (targetStatus == ShipmentStatus.AVAILABLE_FOR_PICKUP) {
            shipment.setAvailablePickupAt(now);
        }

        shipment.setStatus(targetStatus);
        return toResponse(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse confirmDelivery(Integer orderId, Integer buyerId) {
        Shipment shipment = shipmentRepository
                .findForDeliveryConfirmation(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            completeCashOnDeliveryPayment(orderId, LocalDateTime.now());
            return toResponse(shipment);
        }
        if (shipment.getStatus() != ShipmentStatus.SHIPPED
                && shipment.getStatus() != ShipmentStatus.AVAILABLE_FOR_PICKUP) {
            throw new InvalidOrderException(
                    "Only shipped shipments can be confirmed as delivered");
        }

        Order order = shipment.getOrder();
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderException(
                    "Order must be shipped before delivery can be confirmed");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(now);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);
        completeCashOnDeliveryPayment(orderId, now);

        return toResponse(shipmentRepository.save(shipment));
    }

    private void completeCashOnDeliveryPayment(Integer orderId, LocalDateTime paidAt) {
        paymentRepository
                .findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                        orderId, PaymentStatus.PENDING, CASH_ON_DELIVERY)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(paidAt);
                });
    }

    private boolean isOrderSeller(Order order, Integer memberId) {
        return sellerRepository.findByMember_MemberId(memberId)
                .filter(seller -> "ACTIVE".equals(seller.getStatus()))
                .map(Seller::getSellerId)
                .filter(order.getSellerId()::equals)
                .isPresent();
    }

    private Seller getActiveSellerByMemberId(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new OrderNotFoundException("Seller does not exist"));
        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new OrderNotFoundException("Seller is inactive");
        }
        return seller;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getShipmentId(),
                shipment.getOrder().getOrderId(),
                shipment.getCarrierName(),
                shipment.getTrackingNo(),
                shipment.getStatus(),
                shipment.getShippedAt(),
                shipment.getAvailablePickupAt(),
                shipment.getDeliveredAt(),
                shipment.getDeliveryPhotoUrl(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt());
    }
}
