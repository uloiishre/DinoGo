<script setup>
import { useRoute, RouterLink } from 'vue-router'

const route = useRoute()

const order = {
  orderId: route.params.id,
  orderNo: `ORD-20260814-${String(route.params.id).padStart(3, '0')}`,
  buyerName: '王小明',
  receiverName: '王小明',
  receiverPhone: '0912-345-678',
  shippingAddress: '台北市大安區測試路 100 號',
  totalAmount: 1280,
  orderStatus: '已付款',
  shippingStatus: '待出貨',
  createdAt: '2026-08-14 10:30',
  items: [
    {
      productName: 'Apple 手機',
      skuName: '黑色 / 128GB',
      quantity: 1,
      unitPrice: 1280,
    },
  ],
}

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(amount)
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
      </div>
    </header>
  </section>
</template>
