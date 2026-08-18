<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

defineProps({
  compact: { type: Boolean, default: false },
})

const router = useRouter()

const keyword = ref('')

const searchProducts = () => {
  const value = keyword.value.trim()

  if (!value) {
    router.push({
      name: 'ProductList',
    })
    return
  }

  router.push({
    name: 'ProductList',
    query: {
      keyword: value,
    },
  })
}
</script>

<template>
  <form
    class="search-bar"
    :class="{ 'search-bar--compact': compact }"
    role="search"
    @submit.prevent="searchProducts"
  >
    <label class="visually-hidden" for="site-search"> 搜尋商品 </label>

    <select class="form-select search-category" aria-label="搜尋分類">
      <option>全部分類</option>
      <option>商品</option>
      <option>品牌</option>
    </select>

    <input
      id="site-search"
      v-model="keyword"
      class="form-control search-input"
      type="search"
      placeholder="搜尋商品、品牌或關鍵字"
    />

    <button class="btn search-submit" type="submit" aria-label="搜尋">
      <i class="bi bi-search" aria-hidden="true"></i>
    </button>
  </form>
</template>

<style scoped>
.search-bar {
  display: flex;
  width: 100%;
  max-width: 840px;
  min-height: 48px;
  border: 1px solid var(--color-primary-500);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.search-category {
  width: 112px;
  height: 46px;
  flex: 0 0 112px;
  padding-inline: var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  border: 0;
  border-right: 1px solid var(--color-border);
  border-radius: 0;
  background-color: var(--color-secondary-100);
}
.search-input {
  min-width: 0;
  height: 46px;
  padding-inline: var(--space-4);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  border: 0;
  border-left: 0;
  border-radius: 0;
}
.search-input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}
.search-input:focus,
.search-category:focus {
  border-color: transparent;
  box-shadow: var(--shadow-focus);
}
.search-submit {
  width: 48px;
  flex: 0 0 48px;
  min-height: 46px;
  font-size: 20px;
  color: var(--color-surface);
  border: 0;
  border-radius: 0;
  background: var(--color-primary-500);
}
.search-submit:hover,
.search-submit:focus-visible {
  color: var(--color-surface);
  border-color: var(--color-primary-hover);
  background: var(--color-primary-hover);
}
.search-bar--compact .search-category {
  display: none;
}
.search-bar--compact .search-input {
  border-left: 0;
  border-radius: 0;
}

@media (max-width: 575.98px) {
  .search-bar {
    max-width: none;
  }
}
</style>
