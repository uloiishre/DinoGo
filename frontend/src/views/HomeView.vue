<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'
import { getImageUrl } from '@/utils/imageUrl'

const categories = [
  { name: '包袋', icon: 'bi-handbag' },
  { name: '皮夾', icon: 'bi-wallet2' },
  { name: '配件', icon: 'bi-gem' },
  { name: '生活選物', icon: 'bi-house-heart' },
  { name: '旅行用品', icon: 'bi-luggage' },
]

const products = ref([])
const productLoading = ref(false)

const loadHotProducts = async () => {
  try {
    productLoading.value = true

    const response = await api.get('/products', {
      params: {
        page: 0,
        size: 5,
        sort: 'salesDesc',
      },
    })

    products.value = response.data.content ?? []
  } catch (error) {
    console.error('取得熱門商品失敗：', error)
    products.value = []
  } finally {
    productLoading.value = false
  }
}

const trustItems = [
  { label: '平台保障交易', icon: 'bi-shield-check' },
  { label: '清楚配送進度', icon: 'bi-truck' },
  { label: '客服協助', icon: 'bi-headset' },
]

onMounted(() => {
  loadHotProducts()
})
</script>

<template>
  <main class="home-page">
    <div class="container home-page__container">
      <section class="home-hero" aria-labelledby="hero-title">
        <div class="home-hero__copy">
          <p class="home-hero__eyebrow">CURATED MARKETPLACE</p>
          <h1 id="hero-title" class="home-hero__title">好物，慢慢挑</h1>
          <p class="home-hero__description">低調、耐用、值得信任的日常選物。</p>
          <RouterLink class="home-hero__cta dg-focus-ring" to="/products">探索新品</RouterLink>
        </div>
        <div class="home-hero__visual" aria-hidden="true">
          <i class="bi bi-bag home-hero__bag"></i>
        </div>
      </section>

      <section class="home-section" aria-labelledby="category-title">
        <div class="home-section__heading">
          <h2 id="category-title">商品分類</h2>
          <RouterLink class="home-section__link dg-focus-ring" to="/products"
            >全部分類 <span aria-hidden="true">→</span></RouterLink
          >
        </div>
        <div class="category-grid">
          <RouterLink
            v-for="category in categories"
            :key="category.name"
            class="category-card dg-focus-ring"
            to="/products"
          >
            <i :class="['bi', category.icon]" aria-hidden="true"></i
            ><span>{{ category.name }}</span>
          </RouterLink>
        </div>
      </section>

      <section class="home-section" aria-labelledby="product-title">
        <div class="home-section__heading">
          <h2 id="product-title">新品與熱門</h2>
          <RouterLink
            class="home-section__link dg-focus-ring"
            :to="{
              name: 'ProductList',
              query: { sort: 'salesDesc' },
            }"
            >更多商品 <span aria-hidden="true">→</span></RouterLink
          >
        </div>
        <div v-if="productLoading">熱門商品載入中...</div>

        <div v-else class="product-grid">
          <RouterLink
            v-for="product in products"
            :key="product.productId"
            class="product-card dg-focus-ring"
            :to="{
              name: 'ProductDetail',
              params: { id: product.productId },
            }"
          >
            <div class="product-image">
              <img
                v-if="product.imageUrl"
                :src="getImageUrl(product.imageUrl)"
                :alt="product.productName"
              />

              <div v-else class="product-image-placeholder">暫無圖片</div>
            </div>

            <span class="product-card__name">
              {{ product.productName }}
            </span>

            <div class="product-card__meta">
              <span class="product-card__price">
                <template v-if="product.minPrice === product.maxPrice">
                  NT$ {{ product.minPrice }}
                </template>

                <template v-else> NT$ {{ product.minPrice }} ~ {{ product.maxPrice }} </template>
              </span>

              <span class="product-card__sold"> 已售出 {{ product.soldCount ?? 0 }} 件 </span>
            </div>
          </RouterLink>
        </div>
      </section>

      <section class="trust-grid" aria-label="DinoGo 平台服務保障">
        <div v-for="item in trustItems" :key="item.label" class="trust-card">
          <i :class="['bi', item.icon]" aria-hidden="true"></i><span>{{ item.label }}</span>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.home-page {
  padding: 20px 0 64px;
}
.home-page__container {
  display: grid;
  max-width: 1440px;
  gap: var(--space-4);
}
.home-hero {
  display: flex;
  min-height: 230px;
  align-items: center;
  gap: var(--space-5);
  padding: 28px;
  background: var(--color-primary-soft);
  border-radius: var(--radius-lg);
}
.home-hero__copy {
  flex: 1 1 auto;
}
.home-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--color-primary-700);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.home-hero__title {
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.home-hero__description {
  margin: 10px 0;
  color: var(--color-text-muted);
  font-size: 13px;
}
.home-hero__cta {
  display: inline-grid;
  width: 120px;
  height: 40px;
  margin-top: 2px;
  color: var(--color-surface);
  font-size: 12px;
  font-weight: 600;
  place-items: center;
  border: 1px solid var(--color-primary-700);
  border-radius: var(--radius-md);
  background: var(--color-primary-700);
  text-decoration: none;
}
.home-hero__cta:hover {
  color: var(--color-surface);
  background: var(--color-primary-800);
  border-color: var(--color-primary-800);
}
.home-hero__visual {
  display: grid;
  width: min(36%, 430px);
  height: 174px;
  flex: 0 1 430px;
  color: var(--color-primary);
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}
.home-hero__bag {
  font-size: 48px;
}
.home-section {
  display: grid;
  gap: var(--space-3);
}
.home-section__heading {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.home-section__heading h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 19px;
  font-weight: 700;
}
.home-section__link {
  color: var(--color-primary-700);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}
.home-section__link:hover {
  color: var(--color-primary-800);
  text-decoration: underline;
}
.category-grid,
.product-grid,
.trust-grid {
  display: grid;
  gap: var(--space-3);
}
.category-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}
.category-card,
.trust-card {
  display: flex;
  min-height: 82px;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--color-text);
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  text-decoration: none;
}
.category-card i,
.trust-card i {
  color: var(--color-primary-700);
  font-size: 18px;
}
.category-card:hover {
  color: var(--color-primary-800);
  border-color: var(--color-primary-300);
  background: var(--color-primary-50);
}
.product-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}
.product-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
  padding: 9px;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  text-decoration: none;
}
.product-card:hover {
  color: var(--color-text);
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}
.product-image {
  display: grid;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-bg-muted);
}
.product-card__name {
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-card__price {
  font-size: 11px;
  font-weight: 700;
}
.trust-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.trust-card {
  min-height: 74px;
  gap: 10px;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.product-image-placeholder {
  color: var(--color-text-subtle);
  font-size: var(--font-size-xs);
}

.product-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.product-card__price {
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 700;
}

.product-card__sold {
  flex-shrink: 0;
  color: var(--color-text-muted);
  font-size: 10px;
}
@media (max-width: 767.98px) {
  .home-page {
    padding: var(--space-4) 0 var(--space-7);
  }
  .home-hero {
    min-height: auto;
    padding: var(--space-5);
  }
  .home-hero__visual {
    display: none;
  }
  .category-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .product-card:nth-child(n + 4) {
    display: none;
  }
}
@media (max-width: 575.98px) {
  .home-page__container {
    gap: var(--space-5);
  }
  .home-hero__title {
    font-size: var(--font-size-xl);
  }
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .category-card {
    min-height: 70px;
  }
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .product-card:nth-child(n + 3) {
    display: none;
  }
  .trust-grid {
    grid-template-columns: 1fr;
  }
  .trust-card {
    min-height: 58px;
  }
  .home-section__heading h2 {
    font-size: var(--font-size-md);
  }
}
</style>
