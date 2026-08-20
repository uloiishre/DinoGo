import assert from 'node:assert/strict'
import test from 'node:test'

globalThis.sessionStorage = {
  getItem: () => null,
  removeItem: () => {},
}

const [{ acceptSellerOrder, createSellerShipment, updateSellerShipmentStatus }, { default: api }] = await Promise.all([
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

test('createSellerShipment posts shipment details for an order', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { shipmentId: 3, status: 'PREPARING' },
      status: 201,
      statusText: 'Created',
      headers: {},
      config,
    }
  }

  const shipment = { carrierName: '黑貓宅急便', trackingNo: 'TRACK-1' }
  const response = await createSellerShipment(10, shipment)

  assert.equal(request.method, 'post')
  assert.equal(request.url, '/orders/10/shipment')
  assert.deepEqual(JSON.parse(request.data), shipment)
  assert.equal(response.data.status, 'PREPARING')
})

test('updateSellerShipmentStatus patches the next shipment status', async (context) => {
  const originalAdapter = api.defaults.adapter
  context.after(() => {
    api.defaults.adapter = originalAdapter
  })

  let request
  api.defaults.adapter = async (config) => {
    request = config
    return {
      data: { shipmentId: 3, status: 'SHIPPED' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    }
  }

  const response = await updateSellerShipmentStatus(10, 'SHIPPED')

  assert.equal(request.method, 'patch')
  assert.equal(request.url, '/orders/10/shipment/status')
  assert.deepEqual(JSON.parse(request.data), { status: 'SHIPPED' })
  assert.equal(response.data.status, 'SHIPPED')
})
