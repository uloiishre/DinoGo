import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import OrderDetail from '../../src/views/sales/OrderDetail.vue'
import OrderList from '../../src/views/sales/OrderList.vue'
import { getMemberOrders, getOrder } from '../../src/api/order.js'

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ params: { id: '10' } }),
}))

vi.mock('../../src/api/order.js', () => ({
  cancelOrder: vi.fn(),
  confirmDelivery: vi.fn(),
  getMemberOrders: vi.fn(),
  getOrder: vi.fn(),
}))

const itemFixture = {
  orderItemId: 1,
  productName: '測試商品',
  skuSpec: '標準規格',
  quantity: 1,
  unitPrice: 500,
  subtotal: 500,
}

const orderFixture = (overrides = {}) => ({
  orderId: 10,
  orderNo: 'ORD-10',
  sellerId: 30,
  status: 'SHIPPED',
  createdAt: '2026-08-20T02:00:00Z',
  receiverName: '王小明',
  receiverPhone: '0912345678',
  shippingPostalCode: '100',
  shippingCity: '台北市',
  shippingDistrict: '中正區',
  shippingDetailAddress: '測試路 1 號',
  buyerRemark: null,
  subtotalAmount: 500,
  shippingFee: 0,
  discountAmount: 0,
  totalAmount: 500,
  items: [itemFixture],
  payment: null,
  shipment: { status: 'SHIPPED' },
  ...overrides,
})

beforeEach(() => {
  vi.clearAllMocks()
})

describe('buyer order list aggregate display status', () => {
  test('renders aggregate badges and filters the pending-receipt tab by aggregate group', async () => {
    getMemberOrders.mockResolvedValue({
      data: [
        orderFixture({
          orderId: 10,
          orderNo: 'ORD-IN-TRANSIT',
          shipment: { status: 'SHIPPED' },
        }),
        orderFixture({
          orderId: 11,
          orderNo: 'ORD-PICKUP',
          shipment: { status: 'AVAILABLE_FOR_PICKUP' },
        }),
        orderFixture({
          orderId: 12,
          orderNo: 'ORD-PROCESSING',
          status: 'PROCESSING',
          shipment: null,
        }),
      ],
    })

    const wrapper = mount(OrderList)
    await flushPromises()

    const badges = wrapper.findAll('.status-badge')
    expect(badges.map((badge) => badge.text())).toEqual([
      '運送中',
      '待取貨',
      '待出貨',
    ])
    expect(badges[0].classes()).toContain('status-in_transit')
    expect(badges[1].classes()).toContain('status-pending_pickup')

    const pendingReceiptTab = wrapper
      .findAll('.order-tabs button')
      .find((button) => button.text() === '待收貨')
    await pendingReceiptTab.trigger('click')

    const visibleText = wrapper.find('.order-list').text()
    expect(wrapper.findAll('.order-card')).toHaveLength(2)
    expect(visibleText).toContain('ORD-IN-TRANSIT')
    expect(visibleText).toContain('ORD-PICKUP')
    expect(visibleText).not.toContain('ORD-PROCESSING')
  })
})

describe('buyer order detail aggregate display status', () => {
  test('renders the detail badge from order and shipment status together', async () => {
    getOrder.mockResolvedValue({
      data: orderFixture({
        shipment: {
          status: 'AVAILABLE_FOR_PICKUP',
          carrierName: '黑貓宅急便',
          trackingNo: 'TRACK-001',
        },
      }),
    })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    expect(getOrder).toHaveBeenCalledWith(10)
    const badge = wrapper.get('.status-badge')
    expect(badge.text()).toBe('待取貨')
    expect(badge.classes()).toContain('status-pending_pickup')
    expect(wrapper.text()).toContain('可取貨')
  })
})
