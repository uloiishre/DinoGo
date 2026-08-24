import api from './axios'

export const getSellerWallet = () => {
  return api.get('/seller/wallet')
}

export const getSellerWalletTransactions = () => {
  return api.get('/seller/wallet/transactions')
}
