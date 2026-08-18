export type RecipientKind = 'MEMBER' | 'SELLER'

export interface BackendInboxMessage {
  recordId: number
  sendId: number
  msgFunction: string
  msgfromSellerId: number
  msgtoMemberId: number | null
  msgtoSellerId: number | null
  orderId: number | null
  orderStatus: 'PAID' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | null
  msgLabel: string
  sendTitle: string
  sendContent: string
  recordStatus: 'UNREAD' | 'READ' | 'DELETE'
  recordCreatedAt: string
  memberInbox: 'SYSTEM_INBOX' | 'ORDER_INBOX' | 'SELLER_INBOX' | null
  sellerInbox: 'SYSTEM_NOTICE' | 'NEW_ORDER' | 'CANCELLED_ORDER' | null
}

export interface EmailTemplateModel extends BackendInboxMessage {
  recipientKind: RecipientKind
  recipientEmail: string
  recipientName?: string
  shopName?: string
}

