package com.dinogo.chat.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductImageRepository;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.chat.dto.ChatContextRequest;
import com.dinogo.chat.dto.ChatConversationResponse;
import com.dinogo.chat.dto.ChatMessageRequest;
import com.dinogo.chat.dto.ChatMessageResponse;
import com.dinogo.chat.entity.ChatConversation;
import com.dinogo.chat.entity.ChatMessage;
import com.dinogo.chat.entity.ChatMessageType;
import com.dinogo.chat.entity.ChatSenderRole;
import com.dinogo.chat.repository.ChatConversationRepository;
import com.dinogo.chat.repository.ChatMessageRepository;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
@Transactional(readOnly = true)
public class DinoChatService {

    private static final String CLOUDINARY_HOST = "res.cloudinary.com";

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSkuRepository skuRepository;
    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final MemberRepository memberRepository;

    public DinoChatService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductSkuRepository skuRepository,
            OrderRepository orderRepository,
            SellerRepository sellerRepository,
            MemberRepository memberRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.skuRepository = skuRepository;
        this.orderRepository = orderRepository;
        this.sellerRepository = sellerRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ChatConversationResponse getOrCreateConversation(Integer memberId, ChatContextRequest request) {
        Integer sellerId = resolveSellerId(memberId, request);
        ChatConversation conversation = getOrCreatePersistentConversation(memberId, sellerId);
        return toConversationResponse(conversation, ChatSenderRole.BUYER);
    }

    public List<ChatConversationResponse> listConversations(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId).orElse(null);
        if (seller != null && "ACTIVE".equals(seller.getStatus())) {
            return conversationRepository.findBySellerIdOrderByUpdatedAtDescConversationIdDesc(seller.getSellerId()).stream()
                    .map(conversation -> toConversationResponse(conversation, ChatSenderRole.SELLER))
                    .toList();
        }
        return conversationRepository.findByBuyerIdOrderByUpdatedAtDescConversationIdDesc(memberId).stream()
                .map(conversation -> toConversationResponse(conversation, ChatSenderRole.BUYER))
                .toList();
    }

    public List<ChatMessageResponse> listMessages(Integer memberId, Integer conversationId) {
        requireParticipant(memberId, conversationId);
        return messageRepository.findByConversationConversationIdOrderByCreatedAtAscMessageIdAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatConversationResponse openConversation(Integer memberId, Integer conversationId) {
        ChatConversation conversation = requireConversationForUpdate(conversationId);
        ChatSenderRole role = requireParticipant(memberId, conversation);
        if (role == ChatSenderRole.BUYER) {
            conversation.setBuyerUnreadCount(0);
        } else {
            conversation.setSellerUnreadCount(0);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return toConversationResponse(conversation, role);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Integer memberId, Integer conversationId, ChatMessageRequest request) {
        ChatConversation conversation = requireConversationForUpdate(conversationId);
        ChatSenderRole senderRole = requireParticipant(memberId, conversation);
        ChatMessageType messageType = request.messageType() == null ? ChatMessageType.TEXT : request.messageType();
        validateMessageRequest(memberId, conversation, messageType, request);

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderMemberId(memberId);
        message.setSenderRole(senderRole);
        message.setMessageType(messageType);
        message.setContent(normalizeBlankToNull(request.content()));
        message.setImageUrl(normalizeBlankToNull(request.imageUrl()));
        message.setImagePublicId(normalizeBlankToNull(request.imagePublicId()));
        message.setProductId(request.productId());
        message.setSkuId(request.skuId());
        message.setOrderId(request.orderId());
        message.setCreatedAt(now);

        ChatMessage saved = messageRepository.save(message);
        conversation.setLastMessage(saved);
        conversation.setLatestMessageAt(now);
        conversation.setUpdatedAt(now);
        if (senderRole == ChatSenderRole.BUYER) {
            conversation.setSellerUnreadCount(conversation.getSellerUnreadCount() + 1);
        } else {
            conversation.setBuyerUnreadCount(conversation.getBuyerUnreadCount() + 1);
        }
        conversationRepository.save(conversation);
        return toMessageResponse(saved);
    }

    public Integer getTotalUnread(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId).orElse(null);
        if (seller != null && "ACTIVE".equals(seller.getStatus())) {
            return conversationRepository.findBySellerIdOrderByUpdatedAtDescConversationIdDesc(seller.getSellerId()).stream()
                    .map(ChatConversation::getSellerUnreadCount)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);
        }
        return conversationRepository.findByBuyerIdOrderByUpdatedAtDescConversationIdDesc(memberId).stream()
                .map(ChatConversation::getBuyerUnreadCount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    public List<Integer> participantMemberIds(Integer conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        Seller seller = sellerRepository.findBySellerId(conversation.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
        return List.of(conversation.getBuyerId(), seller.getMemberId());
    }

    private ChatConversation getOrCreatePersistentConversation(Integer buyerId, Integer sellerId) {
        return conversationRepository.findByBuyerIdAndSellerIdForUpdate(buyerId, sellerId)
                .orElseGet(() -> createConversation(buyerId, sellerId));
    }

    private ChatConversation createConversation(Integer buyerId, Integer sellerId) {
        LocalDateTime now = LocalDateTime.now();
        ChatConversation created = new ChatConversation();
        created.setBuyerId(buyerId);
        created.setSellerId(sellerId);
        created.setBuyerUnreadCount(0);
        created.setSellerUnreadCount(0);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        return conversationRepository.save(created);
    }

    private Integer resolveSellerId(Integer memberId, ChatContextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Chat context is required.");
        }
        if (request.orderId() != null) {
            Order order = orderRepository.findByOrderIdAndBuyerId(request.orderId(), memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));
            return order.getSellerId();
        }
        if (request.productId() != null) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found."));
            if (request.skuId() != null) {
                ProductSku sku = skuRepository.findById(request.skuId())
                        .orElseThrow(() -> new IllegalArgumentException("SKU not found."));
                if (!product.getProductId().equals(sku.getProduct().getProductId())) {
                    throw new IllegalArgumentException("SKU does not belong to product.");
                }
            }
            return product.getSeller().getSellerId();
        }
        if (request.sellerId() != null) {
            sellerRepository.findBySellerIdAndStatusIgnoreCase(request.sellerId(), "ACTIVE")
                    .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
            return request.sellerId();
        }
        throw new IllegalArgumentException("sellerId, productId, or orderId is required.");
    }

    private ChatConversation requireConversation(Integer conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
    }

    private ChatConversation requireConversationForUpdate(Integer conversationId) {
        return conversationRepository.findByConversationIdForUpdate(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
    }

    private ChatConversation requireParticipant(Integer memberId, Integer conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        requireParticipant(memberId, conversation);
        return conversation;
    }

    private ChatSenderRole requireParticipant(Integer memberId, ChatConversation conversation) {
        if (conversation.getBuyerId().equals(memberId)) {
            return ChatSenderRole.BUYER;
        }
        Seller seller = sellerRepository.findBySellerId(conversation.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));
        if (memberId.equals(seller.getMemberId())) {
            return ChatSenderRole.SELLER;
        }
        throw new IllegalArgumentException("No permission to access this conversation.");
    }

    private void validateMessageRequest(
            Integer memberId,
            ChatConversation conversation,
            ChatMessageType messageType,
            ChatMessageRequest request) {
        boolean hasText = StringUtils.hasText(request.content());
        boolean hasImage = StringUtils.hasText(request.imageUrl());
        boolean hasProduct = request.productId() != null;
        boolean hasOrder = request.orderId() != null;

        if (!hasText && !hasImage && !hasProduct && !hasOrder) {
            throw new IllegalArgumentException("Message content is required.");
        }
        if (messageType == ChatMessageType.IMAGE && !hasImage) {
            throw new IllegalArgumentException("Image URL is required.");
        }
        if (hasImage) {
            validateCloudinaryUrl(request.imageUrl());
        }
        if (hasProduct) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found."));
            if (!conversation.getSellerId().equals(product.getSeller().getSellerId())) {
                throw new IllegalArgumentException("Product does not belong to this seller.");
            }
            if (request.skuId() != null) {
                ProductSku sku = skuRepository.findById(request.skuId())
                        .orElseThrow(() -> new IllegalArgumentException("SKU not found."));
                if (!request.productId().equals(sku.getProduct().getProductId())) {
                    throw new IllegalArgumentException("SKU does not belong to product.");
                }
            }
        }
        if (hasOrder) {
            Order order = orderRepository.findByOrderIdAndBuyerId(request.orderId(), conversation.getBuyerId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));
            if (!conversation.getSellerId().equals(order.getSellerId())) {
                throw new IllegalArgumentException("Order does not belong to this seller.");
            }
            if (!conversation.getBuyerId().equals(memberId) && requireParticipant(memberId, conversation) != ChatSenderRole.SELLER) {
                throw new IllegalArgumentException("No permission to use this order.");
            }
        }
    }

    private ChatConversationResponse toConversationResponse(ChatConversation conversation, ChatSenderRole role) {
        Seller seller = sellerRepository.findBySellerId(conversation.getSellerId()).orElse(null);
        Member buyer = memberRepository.findById(conversation.getBuyerId()).orElse(null);
        ChatMessage lastMessage = conversation.getLastMessage();
        return new ChatConversationResponse(
                conversation.getConversationId(),
                conversation.getBuyerId(),
                conversation.getSellerId(),
                seller == null ? "DINO-GO 店鋪" : seller.getStoreName(),
                seller == null ? null : seller.getStoreLogoUrl(),
                buyer == null ? "買家 #" + conversation.getBuyerId() : formatMemberName(buyer),
                latestText(lastMessage),
                conversation.getLatestMessageAt(),
                role == ChatSenderRole.SELLER
                        ? conversation.getSellerUnreadCount()
                        : conversation.getBuyerUnreadCount());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getConversation().getConversationId(),
                message.getSenderMemberId(),
                message.getSenderRole(),
                message.getMessageType(),
                message.getContent(),
                message.getImageUrl(),
                message.getProductId(),
                message.getSkuId(),
                productCard(message.getProductId(), message.getSkuId()),
                message.getOrderId(),
                orderCard(message.getOrderId()),
                message.getCreatedAt());
    }

    private ChatMessageResponse.ProductCard productCard(Integer productId, Integer skuId) {
        if (productId == null) return null;
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return null;
        ProductSku sku = skuId == null ? null : skuRepository.findById(skuId).orElse(null);
        BigDecimal price = sku == null ? product.getBasePrice() : sku.getPrice();
        String skuText = sku == null ? null : formatSku(sku);
        return new ChatMessageResponse.ProductCard(
                product.getProductId(),
                skuId,
                product.getProductName(),
                skuText,
                price,
                findMainImageUrl(product));
    }

    private ChatMessageResponse.OrderCard orderCard(Integer orderId) {
        if (orderId == null) return null;
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return null;
        return new ChatMessageResponse.OrderCard(
                order.getOrderId(),
                order.getOrderNo(),
                order.getStatus().name(),
                order.getTotalAmount());
    }

    private String latestText(ChatMessage message) {
        if (message == null) return "尚未開始聊天";
        if (StringUtils.hasText(message.getContent())) return message.getContent();
        return switch (message.getMessageType()) {
            case IMAGE -> "傳送了一張圖片";
            case PRODUCT -> "正在詢問商品";
            case ORDER -> "正在詢問訂單";
            case TEXT -> "新訊息";
        };
    }

    private String formatSku(ProductSku sku) {
        String first = formatSpec(sku.getSpec1Name(), sku.getSpec1Value());
        String second = formatSpec(sku.getSpec2Name(), sku.getSpec2Value());
        if (first == null) return second;
        if (second == null) return first;
        return first + " / " + second;
    }

    private String formatSpec(String name, String value) {
        if (!StringUtils.hasText(value)) return null;
        return StringUtils.hasText(name) ? name + ": " + value : value;
    }

    private String formatMemberName(Member member) {
        String name = (StringUtils.hasText(member.getLastName()) ? member.getLastName() : "")
                + (StringUtils.hasText(member.getFirstName()) ? member.getFirstName() : "");
        return StringUtils.hasText(name) ? name : member.getEmail();
    }

    private String findMainImageUrl(Product product) {
        return productImageRepository.findFirstByProductProductIdAndIsMainTrue(product.getProductId())
                .or(() -> productImageRepository.findByProductProductIdOrderBySortOrderAsc(product.getProductId()).stream()
                        .findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateCloudinaryUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !CLOUDINARY_HOST.equalsIgnoreCase(uri.getHost())) {
                throw new IllegalArgumentException("圖片必須是 Cloudinary HTTPS 圖片網址");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("圖片必須是 Cloudinary HTTPS 圖片網址");
        }
    }
}
