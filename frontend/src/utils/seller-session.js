import { AUTH_STORAGE_KEY } from '@/utils/auth-session'

export const getCurrentSellerId = () => {
  // sessionStorage.getItem(AUTH_STORAGE_KEY)讀登入後存在瀏覽器的資料
  const persistedAuth = sessionStorage.getItem(AUTH_STORAGE_KEY)

  if (!persistedAuth) {
    return null
  }

  try {
    const auth = JSON.parse(persistedAuth)
    const sellerId = auth?.member?.sellerId ?? auth?.sellerId ?? null

    // 優先從會員資料內找 sellerId
    return sellerId ? Number(sellerId) : null
  } catch (error) {
    console.error('Read seller session failed:', error)
    // 找不到就回傳 null
    return null
  }
}
