import api from './axios.js'
import { retryPaymentCreationOnce } from './payment-retry.js'

export function getMemberOrders() {
  return api.get('/orders/member')
}

export function getOrder(orderId) {
  return api.get(`/orders/${orderId}`)
}

export function cancelOrder(orderId, request) {
  return api.patch(`/orders/${orderId}/cancel`, request)
}

export function createPayment(orderId, paymentMethodCode) {
  return retryPaymentCreationOnce(() =>
    api.post(`/orders/${orderId}/payments`, { paymentMethodCode }),
  )
}

export function confirmDelivery(orderId) {
  return api.patch(`/orders/${orderId}/shipment/confirm-delivery`)
}
