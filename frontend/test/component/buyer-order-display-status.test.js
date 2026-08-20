import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import OrderDetail from '../../src/views/sales/OrderDetail.vue'
import OrderList from '../../src/views/sales/OrderList.vue'
import { confirmDelivery, getMemberOrders, getOrder } from '../../src/api/order.js'

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
    expect(wrapper.get('button.btn-primary').text()).toBe('確認收貨')
    wrapper.unmount()
  })

  test('refreshes an active order and advances the progress after the seller ships it', async () => {
    getOrder
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'PROCESSING',
          shipment: null,
        }),
      })
      .mockResolvedValueOnce({
        data: orderFixture({
          status: 'SHIPPED',
          shipment: {
            status: 'SHIPPED',
            carrierName: '黑貓宅急便',
            trackingNo: 'TRACK-001',
          },
        }),
      })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    let progressSteps = wrapper.findAll('.progress-step')
    expect(progressSteps[2].classes()).not.toContain('complete')
    expect(wrapper.text()).toContain('尚未建立物流資料')

    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(getOrder).toHaveBeenCalledTimes(2)
    progressSteps = wrapper.findAll('.progress-step')
    expect(progressSteps[2].text()).toBe('商品出貨')
    expect(progressSteps[2].classes()).toContain('complete')
    expect(wrapper.get('.status-badge').text()).toBe('運送中')
    expect(wrapper.text()).toContain('黑貓宅急便')
    expect(wrapper.text()).toContain('TRACK-001')
    expect(wrapper.find('button.btn-primary').exists()).toBe(false)
    wrapper.unmount()
  })

  test('keeps the confirmed state when an older silent refresh resolves later', async () => {
    let resolveStaleRefresh
    const availableOrder = orderFixture({
      shipment: { status: 'AVAILABLE_FOR_PICKUP' },
    })
    const completedOrder = orderFixture({
      status: 'COMPLETED',
      shipment: { status: 'DELIVERED' },
    })
    getOrder
      .mockResolvedValueOnce({ data: availableOrder })
      .mockReturnValueOnce(new Promise((resolve) => { resolveStaleRefresh = resolve }))
      .mockResolvedValueOnce({ data: completedOrder })
    confirmDelivery.mockResolvedValue({ data: null })

    const wrapper = mount(OrderDetail)
    await flushPromises()

    window.dispatchEvent(new Event('focus'))
    await Promise.resolve()
    await wrapper.get('button.btn-primary').trigger('click')
    await flushPromises()

    expect(confirmDelivery).toHaveBeenCalledWith(10)
    expect(getOrder).toHaveBeenCalledTimes(3)
    expect(wrapper.find('button.btn-primary').exists()).toBe(false)

    resolveStaleRefresh({ data: availableOrder })
    await flushPromises()

    expect(wrapper.find('button.btn-primary').exists()).toBe(false)
    expect(wrapper.findAll('.progress-step').every((step) => step.classes().includes('complete'))).toBe(true)
    wrapper.unmount()
  })
})
