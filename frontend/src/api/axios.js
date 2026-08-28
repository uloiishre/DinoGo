import axios from 'axios'
import { clearPersistedAuth, getPersistedToken } from '../utils/auth-session.js'

const api = axios.create({
  baseURL: import.meta.env?.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = getPersistedToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || ''
    const status = error.response?.status

    console.error('========== API ERROR ==========')
    console.error('URL:', requestUrl)
    console.error('METHOD:', error.config?.method)
    console.error('STATUS:', status)
    console.error('DATA:', error.response?.data)
    console.error('TOKEN:', getPersistedToken())
    console.error('================================')

    if (status === 401 && !requestUrl.startsWith('/auth/')) {
      console.error('⚠️ 發生 401，但暫時不自動登出')

      // ⭐ 暫時註解掉
      // clearPersistedAuth()
      // window.location.href = '/login'
    }

    return Promise.reject(error)
  },
)

export default api
