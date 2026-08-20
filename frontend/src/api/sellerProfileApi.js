import api from './axios'

export const getSellerProfile = () => {
  return api.get('/seller/profile')
}

export const updateSellerProfile = (payload) => {
  return api.put('/seller/profile', payload)
}

export const getPublicStore = (sellerId) => {
  return api.get(`/stores/${sellerId}`)
}
