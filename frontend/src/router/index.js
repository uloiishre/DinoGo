import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import OrderList from '@/views/OrderList.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import DefaultStorefrontLayout from '@/layouts/DefaultStorefrontLayout.vue'
import MemberLayout from '@/layouts/MemberLayout.vue'
import SellerLayout from '@/layouts/SellerLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: DefaultStorefrontLayout,
      children: [
        { path: '', name: 'Home', component: () => import('@/views/HomeView.vue') },
        {
          path: 'products',
          name: 'ProductList',
          component: () => import('@/views/ProductListView.vue'),
        },
        {
          path: 'products/:id',
          name: 'ProductDetail',
          component: () => import('@/views/ProductDetailView.vue'),
        },
        { path: 'cart', name: 'Cart', component: () => import('@/views/CartView.vue') },
        {
          path: 'checkout',
          name: 'Checkout',
          component: () => import('@/views/CheckoutView.vue'),
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
          component: () => import('@/views/member/MemberOrdersView.vue'),
        },
        {
          path: 'orders/:id',
          name: 'MemberOrderDetail',
          component: () => import('@/views/member/MemberOrderDetailView.vue'),
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
      children: [
        { path: '', redirect: '/seller/dashboard' },
        {
          path: 'dashboard',
          name: 'SellerDashboard',
          component: () => import('@/views/seller/SellerDashboardView.vue'),
        },
        {
          path: 'products',
          name: 'SellerProducts',
          component: () => import('@/views/seller/SellerProductsView.vue'),
        },
        {
          path: 'orders',
          name: 'SellerOrders',
          component: () => import('@/views/seller/SellerOrdersView.vue'),
        },
      ],
    },
  ],
})

export default router
