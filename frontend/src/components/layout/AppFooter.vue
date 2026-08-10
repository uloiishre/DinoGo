<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <footer class="app-footer">
    <div class="container py-5">
      <div class="row g-4">
        <div class="col-12 col-md-4">
          <RouterLink class="footer-brand" to="/">DinoGo</RouterLink>
          <p class="mt-3 mb-0 footer-copy">探索日常好物，連結值得信賴的品牌與商家。</p>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">購物協助</h2>
          <a href="#shipping" class="footer-link">配送說明</a
          ><a href="#payment" class="footer-link">付款方式</a
          ><a href="#returns" class="footer-link">退換貨政策</a>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">客服與平台</h2>
          <a href="#contact" class="footer-link">聯絡客服</a
          ><a href="#about" class="footer-link">關於 DinoGo</a
          ><a href="#terms" class="footer-link">服務條款</a>
        </div>
        <div class="col-6 col-md-2">
          <h2 class="footer-title">商家服務</h2>
          <RouterLink to="/seller/dashboard" class="footer-link">商家中心</RouterLink
          ><a href="#seller-help" class="footer-link">商家說明</a
          ><a href="#seller-contact" class="footer-link">商家客服</a>
        </div>
      </div>
      <div class="footer-bottom mt-5 pt-3">© 2026 DinoGo. All rights reserved.</div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  color: var(--color-secondary-100);
  background: var(--color-primary-900);
}
.footer-brand {
  color: var(--color-surface);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  text-decoration: none;
}
.footer-copy,
.footer-bottom {
  color: var(--color-secondary-400);
  font-size: var(--font-size-sm);
}
.footer-title {
  margin-bottom: var(--space-3);
  color: var(--color-surface);
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: 700;
}
.footer-link {
  display: block;
  width: fit-content;
  margin-bottom: var(--space-2);
  color: var(--color-secondary-100);
  font-size: var(--font-size-sm);
  text-decoration: none;
}
.footer-link:hover,
.footer-link:focus-visible,
.footer-brand:hover,
.footer-brand:focus-visible {
  color: var(--color-surface);
  text-decoration: underline;
}
.footer-bottom {
  border-top: 1px solid var(--color-primary-700);
}
</style>
