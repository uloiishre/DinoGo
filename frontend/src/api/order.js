import api from './axios'

export function getMemberOrders() {
  return api.get('/orders/member')
}

export function getOrder(orderId) {
  return api.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId, request) {
  return api.patch(`/orders/${orderId}/cancel`, request)
}
