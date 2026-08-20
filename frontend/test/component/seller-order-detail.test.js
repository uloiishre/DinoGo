import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import SellerOrderDetailView from '../../src/views/seller/SellerOrderDetailView.vue'
import {
  acceptSellerOrder,
  createSellerShipment,
  getSellerOrder,
  updateSellerShipmentStatus,
} from '../../src/api/sellerOrderApi.js'

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRoute: () => ({ params: { id: '10' } }),
}))

vi.mock('../../src/api/sellerOrderApi.js', () => ({
  acceptSellerOrder: vi.fn(),
  createSellerShipment: vi.fn(),
  getSellerOrder: vi.fn(),
  updateSellerShipmentStatus: vi.fn(),
}))

const orderFixture = (overrides = {}) => ({
  orderId: 10,
  orderNo: 'ORD-10',
  status: 'PAID',
  createdAt: '2026-08-19T08:00:00Z',
  receiverName: '王小明',
  receiverPhone: '0912345678',
  shippingPostalCode: '100',
  shippingCity: '台北市',
  shippingDistrict: '中正區',
  shippingDetailAddress: '測試路 1 號',
  buyerRemark: null,
  subtotalAmount: 1000,
  shippingFee: 60,
  discountAmount: 0,
  totalAmount: 1060,
  items: [{ orderItemId: 1, productName: '測試商品', skuSpec: '標準', quantity: 1, subtotal: 1000 }],
  payment: { status: 'PAID', paymentMethodCode: 'CREDIT_CARD', paymentMethodName: '信用卡' },
  shipment: null,
  ...overrides,
})

async function mountView(order = orderFixture()) {
  getSellerOrder.mockResolvedValue({ data: order })
  const wrapper = mount(SellerOrderDetailView)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(window, 'confirm').mockReturnValue(true)
})

describe('seller shipment operation flow', () => {
  test('loads the order, accepts form input, and replaces the form with created shipment data', async () => {
    createSellerShipment.mockResolvedValue({
      data: { shipmentId: 3, status: 'PREPARING', carrierName: '黑貓宅急便', trackingNo: 'TRACK-001' },
    })
    const wrapper = await mountView()

    expect(getSellerOrder).toHaveBeenCalledWith(10)
    await wrapper.get('[name="carrierName"]').setValue('  黑貓宅急便  ')
    await wrapper.get('[name="trackingNo"]').setValue(' TRACK-001 ')
    await wrapper.get('form.shipment-form').trigger('submit')
    await flushPromises()

    expect(createSellerShipment).toHaveBeenCalledWith(10, {
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    })
    expect(wrapper.find('form.shipment-form').exists()).toBe(false)
    expect(wrapper.text()).toContain('TRACK-001')
  })

  test('rejects blank shipment fields before calling the API', async () => {
    const wrapper = await mountView()

    await wrapper.get('[name="carrierName"]').setValue('   ')
    await wrapper.get('[name="trackingNo"]').setValue('')
    await wrapper.get('form.shipment-form').trigger('submit')
    await flushPromises()

    expect(createSellerShipment).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toBe('物流商與物流單號皆為必填。')
    expect(wrapper.get('[name="carrierName"]').attributes('aria-invalid')).toBe('true')
    expect(wrapper.get('[name="trackingNo"]').attributes('aria-invalid')).toBe('true')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  test('accepts a paid order and then confirms shipment through the page controls', async () => {
    const preparingShipment = {
      shipmentId: 3,
      status: 'PREPARING',
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    }
    acceptSellerOrder.mockResolvedValue({ data: orderFixture({ status: 'PROCESSING', shipment: preparingShipment }) })
    updateSellerShipmentStatus.mockResolvedValue({
      data: { ...preparingShipment, status: 'SHIPPED', shippedAt: '2026-08-19T10:00:00Z' },
    })
    const wrapper = await mountView(orderFixture({ shipment: preparingShipment }))

    await wrapper.get('button.accept-button').trigger('click')
    await flushPromises()
    expect(acceptSellerOrder).toHaveBeenCalledWith(10)

    const shipmentButton = wrapper.get('button.shipment-submit')
    expect(shipmentButton.text()).toBe('確認出貨')
    await shipmentButton.trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledOnce()
    expect(updateSellerShipmentStatus).toHaveBeenCalledWith(10, 'SHIPPED')
    expect(wrapper.text()).toContain('已出貨')
  })

  test('advances a cash-on-delivery order to delivered after marking it available for pickup', async () => {
    const shippedShipment = {
      shipmentId: 3,
      status: 'SHIPPED',
      carrierName: '黑貓宅急便',
      trackingNo: 'TRACK-001',
    }
    updateSellerShipmentStatus.mockResolvedValue({
      data: {
        ...shippedShipment,
        status: 'AVAILABLE_FOR_PICKUP',
        availablePickupAt: '2026-08-20T08:16:00Z',
      },
    })
    const wrapper = await mountView(orderFixture({
      status: 'SHIPPED',
      payment: {
        status: 'PENDING',
        paymentMethodCode: 'CASH_ON_DELIVERY',
        paymentMethodName: '貨到付款',
      },
      shipment: shippedShipment,
    }))

    let progressItems = wrapper.findAll('.progress-item')
    expect(progressItems[3].classes()).not.toContain('completed')
    expect(wrapper.get('button.shipment-submit').text()).toBe('標記可取貨')

    await wrapper.get('button.shipment-submit').trigger('click')
    await flushPromises()

    expect(updateSellerShipmentStatus).toHaveBeenCalledWith(10, 'AVAILABLE_FOR_PICKUP')
    progressItems = wrapper.findAll('.progress-item')
    expect(progressItems.map((item) => item.text())).toEqual([
      '訂單成立',
      '備貨中',
      '已出貨',
      '已送達',
      '已完成',
    ])
    expect(progressItems.slice(0, 4).every((item) => item.classes().includes('completed'))).toBe(true)
    expect(progressItems[4].classes()).not.toContain('completed')
  })

  test('does not update shipment when the confirmation dialog is cancelled', async () => {
    window.confirm.mockReturnValue(false)
    const wrapper = await mountView(orderFixture({
      status: 'PROCESSING',
      shipment: { shipmentId: 3, status: 'PREPARING' },
    }))

    await wrapper.get('button.shipment-submit').trigger('click')
    await flushPromises()

    expect(updateSellerShipmentStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('確認出貨')
  })
})
