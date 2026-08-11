<script setup>
const store = {
  name: '咬日雜物',
  status: '營運中',
  description: '商家資料將於下一步串接 Seller API。',
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">店鋪資料</p>
        <h1>商家基本資料</h1>
      </div>
    </header>

    <section class="profile-panel">
      <div>
        <span>店鋪名稱</span>
        <strong>{{ store.name }}</strong>
      </div>
      <div>
        <span>目前狀態</span>
        <strong>{{ store.status }}</strong>
      </div>
      <p>{{ store.description }}</p>
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
p {
  margin: 0;
}

h1 {
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.profile-panel {
  display: grid;
  gap: var(--space-4);
  max-width: 640px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.profile-panel div {
  display: grid;
  gap: var(--space-1);
}

.profile-panel span,
.profile-panel p {
  color: var(--color-text-muted);
}
</style>
