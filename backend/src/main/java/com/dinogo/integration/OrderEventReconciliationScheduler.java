package com.dinogo.integration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dinogo.review.service.ReviewHistoryReconciliationService;
import com.dinogo.salesii.service.RecentOrderUpdateQueryService;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;
import com.dinogo.sysmsg.service.OrderMessageService;

//rev+msg-start，總共1次修改，第1次//
/**
 * 功能：每分鐘分頁自檢最近 24 小時更新的訂單聚合。
 * 應用：補正 AFTER_COMMIT 非同步事件因程式關閉或短暫失敗而遺漏的 Review/sysmsg 資料。
 */
@Component
@ConditionalOnProperty(
        name = "app.sysmsg.reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderEventReconciliationScheduler {
    private static final Logger LOG = Logger.getLogger(OrderEventReconciliationScheduler.class.getName());

    private final RecentOrderUpdateQueryService recentOrders;
    private final OrderSysmsgProviderService snapshots;
    private final ReviewHistoryReconciliationService reviews;
    private final OrderMessageService messages;
    private final Clock clock;
    private final int pageSize;

    @Autowired
    public OrderEventReconciliationScheduler(
            RecentOrderUpdateQueryService recentOrders,
            OrderSysmsgProviderService snapshots,
            ReviewHistoryReconciliationService reviews,
            OrderMessageService messages,
            @Value("${sysmsg.reconciliation.page-size:100}") int pageSize) {
        this(recentOrders, snapshots, reviews, messages, Clock.systemDefaultZone(), pageSize);
    }

    OrderEventReconciliationScheduler(
            RecentOrderUpdateQueryService recentOrders,
            OrderSysmsgProviderService snapshots,
            ReviewHistoryReconciliationService reviews,
            OrderMessageService messages,
            Clock clock,
            int pageSize) {
        this.recentOrders = recentOrders;
        this.snapshots = snapshots;
        this.reviews = reviews;
        this.messages = messages;
        this.clock = clock;
        this.pageSize = Math.max(1, pageSize);
    }

    @Scheduled(fixedDelayString = "${sysmsg.reconciliation.fixed-delay-ms:60000}")
    public void reconcileRecentlyUpdatedOrders() {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(1);
        int pageNumber = 0;
        Page<Integer> page;
        do {
            page = recentOrders.findOrderIds(
                    since, PageRequest.of(pageNumber, pageSize));
            page.getContent().forEach(this::reconcileOne);
            pageNumber++;
        } while (page.hasNext());
    }

    private void reconcileOne(Integer orderId) {
        try {
            reviews.reconcile(snapshots.getOrder(orderId));
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Review reconciliation failed for order " + orderId, exception);
        }
        try {
            OrderEventRequest request = new OrderEventRequest();
            request.setOrderId(orderId);
            messages.createOrderEventMessages(request);
        } catch (RuntimeException exception) {
            LOG.log(Level.WARNING, "Sysmsg reconciliation failed for order " + orderId, exception);
        }
    }
}
//rev+msg-end，總共1次修改，第1次//
