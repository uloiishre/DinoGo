<script setup>
import { RouterView } from 'vue-router'
import SellerNav from '@/components/seller/SellerNav.vue'
</script>

<template>
  <div class="seller-layout d-flex min-vh-100">
    <aside class="seller-layout__sidebar flex-shrink-0"><SellerNav /></aside>
    <main class="flex-grow-1 p-4"><RouterView /></main>
  </div>
</template>

<style scoped>
.seller-layout {
  background: var(--color-bg);
}
.seller-layout__sidebar {
  width: 240px;
}
@media (max-width: 767.98px) {
  .seller-layout__sidebar {
    width: 190px;
  }
}
</style>
