import api from './axios'

export const getSellerOrders = () => {
  return api.get('/seller/orders')
}

export const getSellerOrder = (orderId) => {
  return api.get(`/seller/orders/${orderId}`)
}
