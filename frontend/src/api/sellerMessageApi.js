import api from './axios.js'

export const getSellerMessages = ({ category, page = 0, size = 20 } = {}) => {
  return api.get('/seller/messages', {
    params: {
      ...(category && category !== 'ALL' ? { category } : {}),
      page,
      size,
    },
  })
}

export const markSellerMessageRead = (messageId) => {
  return api.patch(`/seller/messages/${messageId}/read`)
}

export const markAllSellerMessagesRead = () => {
  return api.patch('/seller/messages/read-all')
}
