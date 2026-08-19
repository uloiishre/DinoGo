import { createRouter, createWebHistory } from 'vue-router'

import AuthLayout from '@/layouts/AuthLayout.vue'
import DefaultStorefrontLayout from '@/layouts/DefaultStorefrontLayout.vue'
import MemberLayout from '@/layouts/MemberLayout.vue'
import SellerLayout from '@/layouts/SellerLayout.vue'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    component: DefaultStorefrontLayout,
    children: [
      { path: '', name: 'Home', component: () => import('@/views/HomeView.vue') },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/product/ProductListView.vue'),
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/ProductDetailView.vue'),
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/cart/CartView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/cart/CheckoutView.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'coupons',
        name: 'CouponCenter',
        component: () => import('@/views/coupon/CouponCenterView.vue'),
      },
    ],
  },
  {
    path: '/',
    component: AuthLayout,
    children: [
      { path: 'login', name: 'Login', component: () => import('@/views/auth/LoginView.vue') },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/RegisterView.vue'),
      },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
      },
    ],
  },
  {
    path: '/member',
    component: MemberLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/member/overview' },
      {
        path: 'overview',
        name: 'MemberOverview',
        component: () => import('@/views/member/MemberOverviewView.vue'),
      },
      {
        path: 'profile',
        name: 'MemberProfile',
        component: () => import('@/views/member/MemberProfileView.vue'),
      },
      {
        path: 'addresses',
        name: 'MemberAddresses',
        component: () => import('@/views/member/MemberAddressesView.vue'),
      },
      {
        path: 'orders',
        name: 'MemberOrders',
        component: () => import('@/views/sales/OrderList.vue'),
      },
      {
        path: 'orders/:id',
        name: 'MemberOrderDetail',
        component: () => import('@/views/sales/OrderDetail.vue'),
      },
      {
        path: 'favorites',
        name: 'MemberFavorites',
        component: () => import('@/views/member/MemberFavoritesView.vue'),
      },
      {
        path: 'coupons',
        name: 'MemberCoupons',
        component: () => import('@/views/member/MemberCouponsView.vue'),
      },
      {
        path: 'messages',
        name: 'MemberMessages',
        component: () => import('@/views/member/MemberMessagesView.vue'),
      },
      {
        path: 'password',
        name: 'MemberPassword',
        component: () => import('@/views/member/MemberPasswordView.vue'),
      },
    ],
  },
  {
    path: '/seller',
    component: SellerLayout,
    meta: { requiresAuth: true, requiresRole: 'seller' },
    children: [
      { path: '', redirect: '/seller/dashboard' },
      {
        path: 'dashboard',
        name: 'SellerDashboard',
        component: () => import('@/views/seller/SellerDashboardView.vue'),
      },
      {
        path: 'products',
        name: 'SellerProductList',
        component: () => import('@/views/seller/SellerProductListView.vue'),
      },
      {
        path: 'products/new',
        name: 'SellerProductCreate',
        component: () => import('@/views/seller/SellerProductFormView.vue'),
      },
      {
        path: 'products/:id/edit',
        name: 'SellerProductEdit',
        component: () => import('@/views/seller/SellerProductFormView.vue'),
      },
      {
        path: 'orders',
        name: 'SellerOrders',
        component: () => import('@/views/seller/SellerOrderListView.vue'),
      },
      {
        path: 'orders/:id',
        name: 'SellerOrderDetail',
        component: () => import('@/views/seller/SellerOrderDetailView.vue'),
      },
      {
        path: 'coupons',
        name: 'SellerCoupons',
        component: () => import('@/views/seller/SellerCouponsView.vue'),
      },
      {
        path: 'profile',
        name: 'SellerProfile',
        component: () => import('@/views/seller/SellerProfileView.vue'),
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  // Router 不在 component setup 內，必須明確傳入共用 Pinia 實例。
  const authStore = useAuthStore(pinia)
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'Login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.requiresRole && !authStore.hasRole(to.meta.requiresRole)) {
    return { name: 'MemberOverview' }
  }
})

export default router
