import api from './axios'

export function getMemberOrders(memberId) {
  return api.get(`/orders/member/${memberId}`)
}

export function getOrder(orderId) {
  return api.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId, request) {
  return api.patch(`/orders/${orderId}/cancel`, request)
}
