//sysmsg-start，總共1次修改，第1次//
/** 無後端階段的會員訊息展示資料；保留與正式 API 相同的 Promise response 介面。 */
const messages = [
  { recordId: 101, sendId: 1001, msgFunction: 'SY-ANNOUNCE', msgfromSellerId: 0, msgtoMemberId: 1, orderId: null, orderStatus: null, msgLabel: '平台公告', sendTitle: 'DinoGo 會員服務上線', sendContent: '歡迎使用 DinoGo！目前畫面採用前端模擬資料展示。', recordStatus: 'UNREAD', recordCreatedAt: '2026-08-24T09:30:00', memberInbox: 'SYSTEM_INBOX' },
  { recordId: 102, sendId: 1002, msgFunction: 'OR-SHIPPED', msgfromSellerId: 0, msgtoMemberId: 1, orderId: 20260824001, orderStatus: 'SHIPPED', msgLabel: '訂單通知', sendTitle: '您的商品已出貨', sendContent: '訂單 #20260824001 已由商家出貨，請留意物流進度。', recordStatus: 'UNREAD', recordCreatedAt: '2026-08-24T08:15:00', memberInbox: 'ORDER_INBOX' },
  { recordId: 103, sendId: 1003, msgFunction: 'SC-REPLY', msgfromSellerId: 27, msgtoMemberId: 1, orderId: null, orderStatus: null, msgLabel: '賣家通知', sendTitle: '商家已回覆您的詢問', sendContent: '您好，商品目前有現貨，今天完成下單即可安排出貨。', recordStatus: 'READ', recordCreatedAt: '2026-08-23T16:40:00', memberInbox: 'SELLER_INBOX' },
]

const response = (data) => Promise.resolve({ data })

export function getMemberInboxPreview(category, size = 4) {
  return response({ items: messages.filter((item) => item.memberInbox === category).slice(0, size), nextCursor: null, hasNext: false })
}

export function getMemberUnreadCount() {
  return response({ unreadCount: messages.filter((item) => item.recordStatus === 'UNREAD').length })
}

export function getMemberInbox(category) {
  return response({ items: messages.filter((item) => item.memberInbox === category), nextCursor: null, hasNext: false })
}

export function markMemberMessageRead(recordId) {
  const item = messages.find((message) => message.recordId === recordId)
  if (item) item.recordStatus = 'READ'
  return response(item)
}

export function getMemberMessageDetail(recordId) {
  return response(messages.find((message) => message.recordId === recordId))
}

export function deleteMemberMessage(recordId) {
  const index = messages.findIndex((message) => message.recordId === recordId)
  if (index >= 0) messages.splice(index, 1)
  return response(null)
}
//sysmsg-end，總共1次修改，第1次//
