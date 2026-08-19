import api from './axios.js'

export const getSellerOrders = () => {
  return api.get('/seller/orders')
}

export const getSellerOrder = (orderId) => {
  return api.get(`/seller/orders/${orderId}`)
}

export const acceptSellerOrder = (orderId) => {
  return api.patch(`/orders/${orderId}/status`, {
    status: 'PROCESSING',
    reason: null,
  })
}
