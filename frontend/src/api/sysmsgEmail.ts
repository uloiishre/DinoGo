import type { BackendInboxMessage } from '../types/email'

const API_BASE = import.meta.env.VITE_SYSMSG_API_BASE_URL ?? ''

async function request<T>(path: string): Promise<T> {
  const token = sessionStorage.getItem('accessToken')
  const response = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new Error(error?.message ?? `訊息 API 呼叫失敗 (${response.status})`)
  }
  return response.json() as Promise<T>
}

export function getMemberOrderMessages(): Promise<BackendInboxMessage[]> {
  return request('/api/sysmsg/member/inbox?category=ORDER_INBOX')
}

export function getSellerNewOrderMessages(): Promise<BackendInboxMessage[]> {
  return request('/api/sysmsg/seller/inbox?category=NEW_ORDER')
}

export function getSellerCancelledMessages(): Promise<BackendInboxMessage[]> {
  return request('/api/sysmsg/seller/inbox?category=CANCELLED_ORDER')
}

