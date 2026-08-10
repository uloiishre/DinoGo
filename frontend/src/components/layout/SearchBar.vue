<script setup>
defineProps({
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <form
    class="search-bar"
    :class="{ 'search-bar--compact': compact }"
    role="search"
    @submit.prevent
  >
    <label class="visually-hidden" for="site-search">搜尋商品</label>
    <select class="form-select search-category" aria-label="搜尋分類">
      <option>全部分類</option>
      <option>商品</option>
      <option>品牌</option>
    </select>
    <input
      id="site-search"
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
  min-height: 42px;
}
.search-category {
  max-width: 120px;
  color: var(--color-text-muted);
  border-color: var(--color-border);
  border-radius: var(--radius-md) 0 0 var(--radius-md);
  background-color: var(--color-surface-soft);
}
.search-input {
  min-width: 0;
  color: var(--color-text);
  border-color: var(--color-border);
  border-left: 0;
  border-radius: 0;
}
.search-input:focus,
.search-category:focus {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus);
}
.search-submit {
  min-width: 46px;
  color: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  background: var(--color-primary);
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
  border-left: 1px solid var(--color-border);
  border-radius: var(--radius-md) 0 0 var(--radius-md);
}
</style>
