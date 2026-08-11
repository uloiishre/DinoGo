<script setup>
// TODO: 等待 B 模組提供分類、品牌與 SKU API 後，改為由 API 載入選項並送出正式 SKU 結構。
const form = {
  productName: '',
  subcategoryId: '',
  brandId: '',
  basePrice: '',
  stock: '',
  status: 'ACTIVE',
}
</script>

<template>
  <section class="seller-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">商品管理</p>
        <h1>新增商品</h1>
      </div>
    </header>

    <form class="product-form">
      <label>
        商品名稱
        <input v-model="form.productName" type="text" placeholder="請輸入商品名稱" />
      </label>
      <label>
        商品分類
        <select v-model="form.subcategoryId">
          <option value="">請選擇分類</option>
          <option value="1">手機</option>
          <option value="2">筆電</option>
          <option value="3">周邊配件</option>
        </select>
      </label>

      <label>
        品牌
        <select v-model="form.brandId">
          <option value="">請選擇品牌</option>
          <option value="1">Apple</option>
          <option value="2">Samsung</option>
          <option value="3">ASUS</option>
        </select>
      </label>

      <label>
        基本售價
        <input v-model="form.basePrice" type="number" min="0" placeholder="0" />
      </label>

      <label>
        SKU 名稱
        <input v-model="form.skuName" type="text" placeholder="例如：黑色 / 128GB" />
      </label>

      <label>
        庫存
        <input v-model="form.stock" type="number" min="0" placeholder="0" />
      </label>

      <label>
        狀態
        <select v-model="form.status">
          <option value="ACTIVE">上架</option>
          <option value="INACTIVE">下架</option>
        </select>
      </label>

      <div class="form-actions">
        <button type="button">儲存草稿</button>
        <button class="primary-button" type="button">送出商品</button>
      </div>
    </form>
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

h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
}

.product-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  max-width: 860px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

label {
  display: grid;
  gap: var(--space-2);
  color: var(--color-text-700);
  font-weight: 600;
}

input,
select {
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  color: var(--color-text);
  background: var(--color-surface);
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

button {
  min-height: 40px;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: var(--color-surface);
  color: var(--color-text-700);
  font-weight: 600;
}

.primary-button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

@media (max-width: 680px) {
  .product-form {
    grid-template-columns: 1fr;
  }
}
</style>
