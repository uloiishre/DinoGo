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

export const createSellerShipment = (orderId, shipment) => {
  return api.post(`/orders/${orderId}/shipment`, shipment)
}

export const updateSellerShipmentStatus = (orderId, status) => {
  return api.patch(`/orders/${orderId}/shipment/status`, { status })
}

export const updateSellerShipmentTrackingInfo = (orderId, shipment) => {
  return api.patch(`/orders/${orderId}/shipment/tracking-info`, shipment)
}
