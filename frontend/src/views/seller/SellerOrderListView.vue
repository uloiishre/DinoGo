<script setup>
const orders = []
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">訂單管理</p>
        <h1>賣家訂單列表</h1>
      </div>
    </header>

    <section class="empty-panel">
      <h2>尚未載入訂單資料</h2>
      <p>下一步會串接 D 模組訂單 API，依 sellerId 顯示賣家的訂單。</p>
    </section>
  </section>
</template>

<style scoped>
.seller-page {
  display: grid;
  gap: var(--space-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.eyebrow {
  margin: 0 0 var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.empty-panel {
  display: grid;
  gap: var(--space-2);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.empty-panel p {
  color: var(--color-text-muted);
}
</style>
