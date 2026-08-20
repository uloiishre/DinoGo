import { computed, ref } from 'vue'

export function useSellerShipmentStatus({ order, updateStatus, confirmAction }) {
  const updatingShipment = ref(false)
  const shipmentActionError = ref('')

  const shipmentAction = computed(() => {
    if (order.value?.shipment?.status === 'PREPARING' && order.value.status === 'PROCESSING') {
      return {
        status: 'SHIPPED',
        label: '確認出貨',
        pendingLabel: '出貨確認中…',
        confirmMessage: '確認商品已交付物流商並完成出貨？此操作無法復原。',
      }
    }
    if (order.value?.shipment?.status === 'SHIPPED') {
      return {
        status: 'AVAILABLE_FOR_PICKUP',
        label: '標記可取貨',
        pendingLabel: '更新中…',
        confirmMessage: '確認商品已送達取貨地點？此操作無法復原。',
      }
    }
    return null
  })

  async function updateShipmentStatus() {
    if (!shipmentAction.value || updatingShipment.value) return
    if (!confirmAction(shipmentAction.value.confirmMessage)) return

    const targetStatus = shipmentAction.value.status
    updatingShipment.value = true
    shipmentActionError.value = ''
    try {
      order.value.shipment = await updateStatus(targetStatus)
      if (targetStatus === 'SHIPPED') order.value.status = 'SHIPPED'
    } catch (error) {
      shipmentActionError.value = error.response?.data?.message ?? '更新物流狀態失敗，請稍後再試。'
    } finally {
      updatingShipment.value = false
    }
  }

  return { shipmentAction, shipmentActionError, updatingShipment, updateShipmentStatus }
}
