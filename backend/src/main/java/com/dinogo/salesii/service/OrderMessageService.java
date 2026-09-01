package com.dinogo.salesii.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.entity.MsgFunctionSequenceEntity;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendDisorderEntity;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendOrderEntity;
import com.dinogo.sysmsg.entity.SendStatus;
import com.dinogo.sysmsg.exception.SysmsgConflictException;
import com.dinogo.sysmsg.repository.MsgFunctionSequenceRepository;
import com.dinogo.sysmsg.repository.RecordRepository;
import com.dinogo.sysmsg.repository.SendRepository;

//rev+msg-start，總共1次修改，第1次//
/**
 * Sales 狀態事件與 sysmsg 資料表間的單體部署接合服務。
 * 僅透過 {@link OrderSysmsgProviderService} 讀取權威訂單，不修改既有 sales 模組。
 */
@Service
public class OrderMessageService {
    private static final Set<String> SUPPORTED = Set.of("PAID", "SHIPPED", "DELIVERED", "COMPLETED");
    private static final int SYSTEM_SELLER_ID = 1;

    private final OrderSysmsgProviderService orders;
    private final SendRepository sends;
    private final RecordRepository records;
    private final MsgFunctionSequenceRepository sequences;

    public OrderMessageService(OrderSysmsgProviderService orders, SendRepository sends,
            RecordRepository records, MsgFunctionSequenceRepository sequences) {
        this.orders = orders;
        this.sends = sends;
        this.records = records;
        this.sequences = sequences;
    }

    /** 同一訂單狀態的會員與商家通知在同一交易中建立，失敗時一併回滾。 */
    @Transactional(rollbackFor = Exception.class)
    public List<SendResponse> createOrderEventMessages(OrderEventRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new IllegalArgumentException("orderId 不可為空");
        }
        OrderSysmsgResponse order = requireComplete(orders.getOrder(request.getOrderId()));
        String status = normalize(order.status());
        if (!SUPPORTED.contains(status) && !"CANCELLED".equals(status)) {
            throw new IllegalStateException("不支援訂單狀態：" + status);
        }

        List<SendResponse> result = new ArrayList<>(2);
        result.add(create(order, status, true));
        result.add(create(order, status, false));
        return result;
    }

    private SendResponse create(OrderSysmsgResponse order, String status, boolean customer) {
        rejectDuplicate(order, status, customer);
        String prefix = customer ? "AC" : "AS";
        String title = title(status);
        String content = content(order, status, customer);
        SendEntity send;
        if ("CANCELLED".equals(status)) {
            if (order.cancelledAt() == null) {
                throw new IllegalStateException("取消訂單缺少 cancelledAt");
            }
            send = new SendDisorderEntity(SYSTEM_SELLER_ID, nextFunction(prefix), order.orderNo(),
                    title, content, SendStatus.SEND, order.orderId(), order.orderNo(),
                    order.totalAmount(), order.paymentMethodId(), order.methodName(),
                    order.cancelReason(), order.cancelledAt(), status);
        } else {
            send = new SendOrderEntity(SYSTEM_SELLER_ID, nextFunction(prefix), order.orderNo(),
                    title, content, SendStatus.SEND, order.orderId(), order.orderNo(),
                    order.totalAmount(), order.paymentMethodId(), order.methodName(),
                    order.createdAt(), status);
        }
        sends.save(send);

        RecordEntity record = new RecordEntity(send,
                customer ? order.buyerId() : null,
                customer ? null : order.sellerId());
        record.setOrderId(order.orderId());
        record.setOrderStatus(status);
        records.save(record);
        return toResponse(send);
    }

    private String nextFunction(String prefix) {
        MsgFunctionSequenceEntity sequence = sequences.findById(prefix)
                .orElseThrow(() -> new IllegalStateException("缺少訊息流水號設定：" + prefix));
        int value = sequence.getCurrentValue();
        if (value < 1 || value > 999) {
            throw new IllegalStateException("訊息流水號超出範圍：" + prefix);
        }
        sequence.setCurrentValue(value == 999 ? 1 : value + 1);
        sequences.save(sequence);
        return "%s-%03d".formatted(prefix, value);
    }

    private void rejectDuplicate(OrderSysmsgResponse order, String status, boolean customer) {
        boolean exists = customer
                ? records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(order.orderId(), status, order.buyerId())
                : records.existsByOrderIdAndOrderStatusAndMsgtoSellerId(order.orderId(), status, order.sellerId());
        if (exists) {
            throw new SysmsgConflictException("訂單通知已存在：" + order.orderId() + "/" + status);
        }
    }

    private OrderSysmsgResponse requireComplete(OrderSysmsgResponse order) {
        if (order == null || order.orderId() == null || order.orderNo() == null
                || order.buyerId() == null || order.sellerId() == null
                || order.totalAmount() == null || order.createdAt() == null) {
            throw new IllegalStateException("訂單通知資料不完整");
        }
        return order;
    }

    private String normalize(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalStateException("訂單狀態不可為空");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String title(String status) {
        return switch (status) {
            case "PAID" -> "訂單付款完成";
            case "SHIPPED" -> "訂單已出貨";
            case "DELIVERED" -> "訂單已送達";
            case "COMPLETED" -> "訂單已完成";
            case "CANCELLED" -> "訂單已取消";
            default -> throw new IllegalStateException("不支援訂單狀態：" + status);
        };
    }

    private String content(OrderSysmsgResponse order, String status, boolean customer) {
        String recipient = customer ? "會員" : "商家";
        return "%s通知：訂單 %s 狀態已更新為 %s。".formatted(recipient, order.orderNo(), status);
    }

    private SendResponse toResponse(SendEntity send) {
        SendResponse response = new SendResponse();
        response.setSendId(send.getSendId());
        response.setMsgFunction(send.getMsgFunction());
        response.setMsgfromSellerId(send.getMsgfromSellerId());
        response.setMsgLabel(send.getMsgLabel());
        response.setSendTitle(send.getSendTitle());
        response.setSendContent(send.getSendContent());
        response.setSendStatus(send.getSendStatus());
        response.setSendUpdAt(send.getSendUpdAt());
        if (send instanceof SendOrderEntity order) response.setOrderNo(order.getOrderNo());
        if (send instanceof SendDisorderEntity order) response.setOrderNo(order.getOrderNo());
        return response;
    }
}
//rev+msg-end，總共1次修改，第1次//
