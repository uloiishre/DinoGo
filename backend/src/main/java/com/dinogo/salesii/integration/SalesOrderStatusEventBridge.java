package com.dinogo.salesii.integration;

import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.dinogo.sales.service.OrderService;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.salesii.event.OrderStatusChangedEvent;

//rev+msg-start，總共1次修改，第1次//
/**
 * 功能：在既有 Sales 寫入方法成功返回後發布補充事件。
 * 應用：不修改 com.dinogo.sales 原始碼，Review/Sysmsg 由 salesii 接收狀態變動。
 */
@Component
public class SalesOrderStatusEventBridge implements BeanPostProcessor, Ordered {

    private static final Map<Class<?>, Set<String>> OBSERVED_METHODS = Map.of(
            OrderService.class, Set.of("updateStatusBySeller", "cancelOrder"),
            PaymentService.class, Set.of("simulatePaymentResult"),
            ShipmentService.class, Set.of("updateShipmentStatus", "confirmDelivery", "simulateTcatEvent"));

    private final ApplicationEventPublisher events;

    public SalesOrderStatusEventBridge(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Set<String> methods = OBSERVED_METHODS.get(targetClass);
        if (methods == null) return bean;

        MethodInterceptor advice = invocation -> {
            Object result = invocation.proceed();
            if (methods.contains(invocation.getMethod().getName())
                    && invocation.getArguments().length > 0
                    && invocation.getArguments()[0] instanceof Integer orderId) {
                events.publishEvent(new OrderStatusChangedEvent(orderId));
            }
            return result;
        };

        if (bean instanceof Advised advised) {
            advised.addAdvice(advice);
            return bean;
        }
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(advice);
        return proxyFactory.getProxy();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
//rev+msg-end，總共1次修改，第1次//
