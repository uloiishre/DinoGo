<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main class="container py-5">
    <h1 class="h3">商品詳情</h1>
    <p class="text-muted">商品編號：{{ route.params.id }}</p>
  </main>
</template>
