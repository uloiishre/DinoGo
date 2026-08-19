import assert from 'node:assert/strict'
import test from 'node:test'
import api from '../src/api/axios.js'
import {
  getSellerMessages,
  markAllSellerMessagesRead,
  markSellerMessageRead,
} from '../src/api/sellerMessageApi.js'

test('seller message API uses the module F contract endpoints', async (t) => {
  const originalGet = api.get
  const originalPatch = api.patch
  const calls = []

  api.get = async (...args) => calls.push(['get', ...args])
  api.patch = async (...args) => calls.push(['patch', ...args])
  t.after(() => {
    api.get = originalGet
    api.patch = originalPatch
  })

  await getSellerMessages({ category: 'ORDER', page: 1, size: 10 })
  await markSellerMessageRead(42)
  await markAllSellerMessagesRead()

  assert.deepEqual(calls, [
    ['get', '/seller/messages', { params: { category: 'ORDER', page: 1, size: 10 } }],
    ['patch', '/seller/messages/42/read'],
    ['patch', '/seller/messages/read-all'],
  ])
})
