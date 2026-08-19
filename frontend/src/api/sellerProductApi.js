import api from './axios'

//查詢目前賣家商品列表
//sellerId 目前由前端傳入，之後可改成登入狀態取得
export const getSellerProducts = (sellerId) => {
  return api.get('/seller/products', {
    params: { sellerId },
  })
}

//指定商品下架
//後端依照sellerId與productId判斷是否為該賣家商品，若是則下架，若否則回傳錯誤
export const disableSellerProduct = (sellerId, productId) => {
  return api.patch(`/seller/products/${productId}/disable`, null, {
    params: { sellerId },
  })
}

// 取得賣家商品詳情，包含已停用 SKU，編輯商品頁用
export const getProductDetail = (productId) => {
  return api.get(`/seller/products/${productId}`)
}

//建立新商品
///TODO: 等待B模組Product create API完成後再整合
export const createSellerProduct = (payload) => {
  return api.post('/products', payload)
}

// TODO: 等 B 模組提供正式商品修改 API 後，確認 method 與欄位格式。
export const updateSellerProduct = (productId, payload) => {
  return api.put(`/products/${productId}`, payload)
}

// 修改既有 SKU
export const updateSellerProductSku = (productId, skuId, payload) => {
  return api.put(`/products/${productId}/skus/${skuId}`, payload)
}

// 批次新增 SKU
export const createSellerProductSkus = (productId, payload) => {
  return api.post(`/products/${productId}/skus/batch`, payload)
}

// 停用既有 SKU
export const disableSellerProductSku = (productId, skuId) => {
  return api.patch(`/products/${productId}/skus/${skuId}/disable`)
}