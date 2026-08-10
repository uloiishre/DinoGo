import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import OrderList from '@/views/OrderList.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/orders',
      name: 'order-list',
      component: OrderList,
    },
    {
      path: '/orders/:orderId',
      name: 'order-detail',
      component: OrderDetail,
      props: true,
    },
  ],
})

export default router
