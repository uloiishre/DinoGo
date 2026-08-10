import { createRouter, createWebHistory } from 'vue-router'
import SellerLayout from '../layouts/SellerLayout.vue'
import SellerDashboardView from '../views/seller/SellerDashboardView.vue'
import SellerOrderListView from '../views/seller/SellerOrderListView.vue'
import SellerProductFormView from '../views/seller/SellerProductFormView.vue'
import SellerProductListView from '../views/seller/SellerProductListView.vue'
import SellerProfileView from '../views/seller/SellerProfileView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/seller/dashboard',
    },
    {
      path: '/seller',
      component: SellerLayout,
      children: [
        {
          path: '',
          redirect: '/seller/dashboard',
        },
        {
          path: 'dashboard',
          name: 'seller-dashboard',
          component: SellerDashboardView,
        },
        {
          path: 'products',
          name: 'seller-products',
          component: SellerProductListView,
        },
        {
          path: 'products/new',
          name: 'seller-product-new',
          component: SellerProductFormView,
        },
        {
          path: 'products/:productId/edit',
          name: 'seller-product-edit',
          component: SellerProductFormView,
        },
        {
          path: 'orders',
          name: 'seller-orders',
          component: SellerOrderListView,
        },
        {
          path: 'profile',
          name: 'seller-profile',
          component: SellerProfileView,
        },
      ],
    },
  ],
})

export default router
