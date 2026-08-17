import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ createPayment }, { default: api }] = await Promise.all([
  import('../src/api/order.js'),
  import('../src/api/axios.js'),
])

test('createPayment retries once after timeout with the same request', async (context) => {
  const originalAdapter = api.defaults.adapter
  const requests = []
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async (config) => {
    requests.push(config)
    if (requests.length === 1) {
      throw {
        code: 'ECONNABORTED',
        config,
        request: {},
      }
    }

    return {
      data: { paymentId: 20, status: 'PENDING' },
      status: 201,
      statusText: 'Created',
      headers: {},
      config,
    }
  }

  const response = await createPayment(10, 'CREDIT_CARD')

  assert.equal(requests.length, 2)
  for (const request of requests) {
    assert.equal(request.method, 'post')
    assert.equal(request.url, '/orders/10/payments')
    assert.deepEqual(JSON.parse(request.data), { paymentMethodCode: 'CREDIT_CARD' })
  }
  assert.equal(response.data.paymentId, 20)
})
