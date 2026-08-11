import api from './axios'

export const getSellerProducts = (sellerId) => {
  return api.get('/seller/products', {
    params: { sellerId },
  })
}

export const disableSellerProduct = (sellerId, productId) => {
  return api.patch(`/seller/products/${productId}/disable`, null, {
    params: { sellerId },
  })
}
