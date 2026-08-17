import api from './axios'

//TODO: 等待D模組提供正是賣家訂單API後，確認URL與欄位格式

export const getSellerOrders = (sellerId) => {
  return api.get('/seller/orders', {
    params: { sellerId },
  })
}

export const getSellerOrder = (orderId) => {
  return api.get(`/seller/orders/${orderId}`)
}
