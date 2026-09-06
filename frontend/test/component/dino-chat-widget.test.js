import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, test, vi } from 'vitest'

const { apiMock, getMemberOrdersMock } = vi.hoisted(() => ({
  apiMock: {
    get: vi.fn(),
    post: vi.fn(),
  },
  getMemberOrdersMock: vi.fn(),
}))

vi.mock('../../src/api/axios.js', () => ({ default: apiMock }))
vi.mock('../../src/api/order.js', () => ({
  getMemberOrders: getMemberOrdersMock,
}))

import DinoChatWidget from '../../src/components/chat/DinoChatWidget.vue'
import { useAuthStore } from '../../src/stores/auth.js'

function mountWidget({ authenticated = false } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)

  if (authenticated) {
    const authStore = useAuthStore(pinia)
    authStore.token = 'demo-token'
    authStore.member = { email: 'demo@example.com' }
    authStore.roles = ['member']
  }

  return mount(DinoChatWidget, {
    attachTo: document.body,
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a class="router-link-stub" :data-to="typeof to === \'string\' ? to : JSON.stringify(to)"><slot /></a>',
        },
      },
    },
  })
}

async function openChat(wrapper) {
  await wrapper.get('.dino-chat__launcher').trigger('click')
}

beforeEach(() => {
  vi.clearAllMocks()
  document.body.innerHTML = ''
})

describe('DinoChatWidget', () => {
  test('searches products with the entered keyword and keeps the result link available', async () => {
    apiMock.get.mockResolvedValueOnce({
      data: {
        content: [
          {
            productName: '金莎巧克力',
            basePrice: 199,
          },
        ],
      },
    })
    const wrapper = mountWidget()
    await openChat(wrapper)

    await wrapper.get('#dino-chat-input').setValue('巧克力')
    await wrapper.findAll('.dino-chat__template')[0].trigger('click')
    await flushPromises()

    expect(apiMock.get).toHaveBeenCalledWith('/products', {
      params: {
        keyword: '巧克力',
        page: 0,
        size: 3,
      },
    })
    expect(wrapper.text()).toContain('金莎巧克力')
    expect(wrapper.text()).toContain('查看更多商品')
    expect(wrapper.text()).toContain('巧克力')
  })

  test('focuses the input after opening chat and treats casual shopping text as product search', async () => {
    apiMock.get.mockResolvedValueOnce({
      data: {
        content: [
          {
            productName: '購物袋',
            basePrice: 120,
          },
        ],
      },
    })
    const wrapper = mountWidget()
    await openChat(wrapper)

    expect(document.activeElement).toBe(wrapper.get('#dino-chat-input').element)

    await wrapper.get('#dino-chat-input').setValue('想購物')
    await wrapper.get('.dino-chat__composer').trigger('submit')
    await flushPromises()

    expect(apiMock.get).toHaveBeenCalledWith('/products', {
      params: {
        keyword: '想購物',
        page: 0,
        size: 3,
      },
    })
    expect(wrapper.text()).toContain('購物袋')
  })

  test('shows the latest order with a link to all orders', async () => {
    getMemberOrdersMock.mockResolvedValueOnce({
      data: [
        {
          orderNo: 'DGD-DEMO-20260908-004',
          createdAt: '2026-09-08T15:08:00',
          status: 'PENDING_PAYMENT',
          totalAmount: 12174,
        },
      ],
    })
    const wrapper = mountWidget({ authenticated: true })
    await openChat(wrapper)

    await wrapper.findAll('.dino-chat__template')[1].trigger('click')
    await flushPromises()

    expect(getMemberOrdersMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('DGD-DEMO-20260908-004')
    expect(wrapper.text()).toContain('待付款')
    expect(wrapper.text()).toContain('查看所有訂單')
    expect(wrapper.get('.router-link-stub').attributes('data-to')).toBe('/member/orders')
  })

  test('does not call protected APIs when the visitor is not logged in', async () => {
    const wrapper = mountWidget()
    await openChat(wrapper)

    await wrapper.findAll('.dino-chat__template')[1].trigger('click')
    await wrapper.findAll('.dino-chat__template')[3].trigger('click')
    await flushPromises()

    expect(getMemberOrdersMock).not.toHaveBeenCalled()
    expect(apiMock.get).not.toHaveBeenCalledWith('/cart')
    expect(wrapper.text()).toContain('登入後即可查詢訂單。')
    expect(wrapper.text()).toContain('登入後即可查看購物車。')
  })

  test('shows member coupons with coupon-center discount text formats', async () => {
    apiMock.get.mockResolvedValueOnce({
      data: [
        {
          couponName: '九月開學精選九折',
          discountType: 'PERCENT',
          discountValue: 10,
          status: 'AVAILABLE',
          endAt: '2026-09-30T23:59:59',
        },
        {
          couponName: '全店滿500折50元',
          discountType: 'AMOUNT',
          discountValue: 50,
          status: 'AVAILABLE',
          endAt: '2026-09-30T23:59:59',
        },
      ],
    })
    const wrapper = mountWidget({ authenticated: true })
    await openChat(wrapper)

    await wrapper.findAll('.dino-chat__template')[2].trigger('click')
    await flushPromises()

    expect(apiMock.get).toHaveBeenCalledWith('/member/coupons')
    expect(wrapper.text()).toContain('九月開學精選九折')
    expect(wrapper.text()).toContain('10% 折扣')
    expect(wrapper.text()).toContain('全店滿500折50元')
    expect(wrapper.text()).toContain('折 NT$ 50')
  })

  test('shows cart items and calculates the total amount on the client', async () => {
    apiMock.get.mockResolvedValueOnce({
      data: {
        items: [
          {
            productName: '輕巧手提戶外露營燈',
            quantity: 7,
            price: 799,
          },
          {
            productName: '復古LED露營燈｜三段亮度調整・USB充電・戶外帳篷燈',
            quantity: 3,
            price: 650,
          },
        ],
      },
    })
    const wrapper = mountWidget({ authenticated: true })
    await openChat(wrapper)

    await wrapper.findAll('.dino-chat__template')[3].trigger('click')
    await flushPromises()

    expect(apiMock.get).toHaveBeenCalledWith('/cart')
    expect(wrapper.text()).toContain('購物車共 2 種商品')
    expect(wrapper.text()).toContain('輕巧手提戶外露營燈')
    expect(wrapper.text()).toContain('復古LED露營燈')
    expect(wrapper.text()).toContain('7,543')
    expect(wrapper.text()).toContain('前往購物車')
  })
})
