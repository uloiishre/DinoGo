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
export const uploadSellerLogo = (file) => {
  const formData = new FormData()
  formData.append('file', file)

  return api.post('/seller/profile/logo', formData)
}

//如果後端回傳 /uploads/xxx.png，前端會轉成：http://localhost:8080/uploads/xxx.png
export const resolveSellerLogoUrl = (url) => {
  if (!url) {
    return ''
  }

  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url
  }

  const baseUrl = import.meta.env?.VITE_API_URL || 'http://localhost:8080/api'
  const origin = baseUrl.replace(/\/api\/?$/, '')

  return `${origin}${url}`
}
