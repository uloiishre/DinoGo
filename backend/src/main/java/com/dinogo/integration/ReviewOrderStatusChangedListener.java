package com.dinogo.integration;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import com.dinogo.review.service.ReviewService;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.salesii.event.OrderStatusChangedEvent;
import com.dinogo.salesii.service.OrderSysmsgProviderService;

//review-start，總共1次修改，第1次//
/**
 * 單體部署的 Sales → Review 接線。
 * salesii bridge 只在 Sales 方法成功返回後發布；單次讀取權威快照後建立／清除評論。
 * 因不修改 Sales，本 listener 不承諾與原 Sales 寫入共用同一交易。
 */
@Component
public class ReviewOrderStatusChangedListener {
    private final OrderSysmsgProviderService orders;
    private final ReviewService reviews;

    public ReviewOrderStatusChangedListener(
            OrderSysmsgProviderService orders,
            ReviewService reviews) {
        this.orders = orders;
        this.reviews = reviews;
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        OrderSysmsgResponse order = orders.getOrder(event.orderId());
        if ("COMPLETED".equals(order.status())) {
            reviews.createHistoryFromCompletedOrder(order);
        } else if ("CANCELLED".equals(order.status())) {
            reviews.deleteHistoryForCancelledOrder(order);
        }
    }
}
//review-end，總共1次修改，第1次//

