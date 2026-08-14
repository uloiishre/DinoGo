import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login } from '@/api/auth'
import { AUTH_STORAGE_KEY } from '@/utils/auth-session'

export const useAuthStore = defineStore('auth', () => {
  // The persistence plugin restores these values from sessionStorage.
  const token = ref('')
  const member = ref(null)

  // Components use this getter instead of reading localStorage directly.
  const isAuthenticated = computed(() => Boolean(token.value))

  function setSession(sessionToken, sessionMember) {
    token.value = sessionToken || ''
    member.value = sessionMember || null
  }

  async function signIn(credentials) {
    // Keep API and persistence logic in the store; the view handles UI only.
    const { data } = await login(credentials)
    setSession(data.token, data.member)
    return data
  }

  function updateMember(updatedMember) {
    // Keep the Header and other components synchronized after profile updates.
    member.value = updatedMember || null
  }

  function signOut() {
    setSession('', null)
  }

  return {
    token,
    member,
    isAuthenticated,
    signIn,
    signOut,
    updateMember,
  }
}, {
  // Keep only authentication data; page state and cart data are not persisted here.
  persist: {
    key: AUTH_STORAGE_KEY,
    storage: sessionStorage,
    pick: ['token', 'member'],
  },
})
