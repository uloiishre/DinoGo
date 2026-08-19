import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ acceptSellerOrder }, { default: api }] = await Promise.all([
  import('../src/api/sellerOrderApi.js'),
  import('../src/api/axios.js'),
])

test('acceptSellerOrder updates a paid order to processing', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { orderId: 10, status: 'PROCESSING' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const response = await acceptSellerOrder(10)

  assert.equal(request.method, 'patch')
  assert.equal(request.url, '/orders/10/status')
  assert.deepEqual(JSON.parse(request.data), { status: 'PROCESSING', reason: null })
  assert.equal(response.data.status, 'PROCESSING')
})
