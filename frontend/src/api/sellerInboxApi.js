//sysmsg-start，總共1次修改，第1次//
/** 無後端階段的商家訊息展示資料；操作只影響本次瀏覽器記憶體狀態。 */
const messages = [
  { recordId: 201, sendId: 2001, msgFunction: 'SY-SELLER', msgfromSellerId: 0, msgtoSellerId: 27, orderId: null, orderStatus: null, msgLabel: '平台公告', sendTitle: '商家中心功能提醒', sendContent: '訊息中心目前以靜態資料展示，正式串接後將顯示即時通知。', recordStatus: 'UNREAD', recordCreatedAt: '2026-08-24T10:00:00', sellerInbox: 'SYSTEM_NOTICE' },
  { recordId: 202, sendId: 2002, msgFunction: 'OR-NEW', msgfromSellerId: 0, msgtoSellerId: 27, orderId: 20260824001, orderStatus: 'PAID', msgLabel: '新訂單', sendTitle: '您有一筆新訂單', sendContent: '訂單 #20260824001 已付款，請儘快確認並安排出貨。', recordStatus: 'UNREAD', recordCreatedAt: '2026-08-24T09:10:00', sellerInbox: 'NEW_ORDER' },
  { recordId: 203, sendId: 2003, msgFunction: 'OR-CANCELLED', msgfromSellerId: 0, msgtoSellerId: 27, orderId: 20260823008, orderStatus: 'CANCELLED', msgLabel: '取消訂單', sendTitle: '訂單已取消', sendContent: '訂單 #20260823008 已由會員取消，請勿安排出貨。', recordStatus: 'READ', recordCreatedAt: '2026-08-23T14:20:00', sellerInbox: 'CANCELLED_ORDER' },
]

const response = (data) => Promise.resolve({ data })

export function getSellerInbox(category) {
  return response({ items: messages.filter((item) => item.sellerInbox === category), nextCursor: null, hasNext: false })
}

export function markSellerInboxRead(recordId) {
  const item = messages.find((message) => message.recordId === recordId)
  if (item) item.recordStatus = 'READ'
  return response(item)
}

export function deleteSellerInboxMessage(recordId) {
  const index = messages.findIndex((message) => message.recordId === recordId)
  if (index >= 0) messages.splice(index, 1)
  return response(null)
}
//sysmsg-end，總共1次修改，第1次//
