<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="container py-5">
    <section class="dg-card mx-auto p-4 p-md-5 text-center" aria-labelledby="not-found-title">
      <p class="mb-2 text-muted">ERROR 404</p>
      <h1 id="not-found-title" class="h3 mb-3">找不到此頁面</h1>
      <p class="mb-4 text-muted">你要前往的頁面不存在，或可能已經移動。</p>
      <RouterLink class="btn btn-primary" :to="{ name: 'Home' }">回到首頁</RouterLink>
    </section>
  </main>
</template>
