package com.dinogo.salesii.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

//rev+msg-start，總共1次修改，第1次//
/**
 * 功能：由 salesii 唯讀查詢最近更新的訂單聚合，不擴充 sales 的 Repository。
 * 應用：供 Review 與 sysmsg 每分鐘補償自檢，涵蓋 Order、Payment、Shipment 更新。
 */
@Service
public class RecentOrderUpdateQueryService {
    private static final String FILTER = """
            FROM Order orders
            WHERE orders.updatedAt >= :since
               OR EXISTS (SELECT payment.paymentId FROM Payment payment
                          WHERE payment.order = orders AND payment.updatedAt >= :since)
               OR EXISTS (SELECT shipment.shipmentId FROM Shipment shipment
                          WHERE shipment.order = orders AND shipment.updatedAt >= :since)
            """;

    private final EntityManager entityManager;

    public RecentOrderUpdateQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<Integer> findOrderIds(LocalDateTime since, Pageable pageable) {
        List<Integer> ids = entityManager.createQuery(
                        "SELECT orders.orderId " + FILTER + " ORDER BY orders.orderId", Integer.class)
                .setParameter("since", since)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        long total = entityManager.createQuery(
                        "SELECT COUNT(orders.orderId) " + FILTER, Long.class)
                .setParameter("since", since)
                .getSingleResult();
        return new PageImpl<>(ids, pageable, total);
    }
}
//rev+msg-end，總共1次修改，第1次//
