<script setup>
defineProps({
  product: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <RouterLink
    :to="{
      name: 'ProductDetail',
      params: { id: product.productId },
    }"
    class="product-card d-block h-100 text-decoration-none"
  >
    <div class="product-image-wrapper">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.productName"
        class="product-image"
      />

      <div v-else class="product-image-placeholder">暫無圖片</div>
    </div>

    <div class="product-info">
      <h2 class="product-name">
        {{ product.productName }}
      </h2>

      <p class="product-price mb-0">NT$ {{ product.basePrice }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.product-card {
  overflow: hidden;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-card:hover {
  color: var(--color-text);
  transform: translateY(-2px);
}

.product-card:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.product-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--color-surface-soft);
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;

  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
}

.product-info {
  padding: var(--space-4);
}

.product-name {
  margin-bottom: var(--space-2);
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-base);
  font-weight: 500;
}

.product-price {
  color: var(--color-primary);
  font-size: var(--font-size-md);
  font-weight: 700;
}
</style>
