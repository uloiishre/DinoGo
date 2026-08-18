import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { googleLogin, linkGoogleAccount, login } from '@/api/auth'
import { AUTH_STORAGE_KEY } from '@/utils/auth-session'

export const useAuthStore = defineStore('auth', () => {
  // The persistence plugin restores these values from sessionStorage.
  const token = ref('')
  const member = ref(null)
  const roles = ref([])

  // Components use this getter instead of reading localStorage directly.
  const isAuthenticated = computed(() => Boolean(token.value))
  const hasRole = (role) => roles.value.includes(role)
  const isSeller = computed(() => hasRole('seller'))

  function setSession(sessionToken, sessionMember, sessionRoles = []) {
    token.value = sessionToken || ''
    member.value = sessionMember || null
    roles.value = Array.isArray(sessionRoles) ? [...sessionRoles] : []
  }

  async function signIn(credentials) {
    // Keep API and persistence logic in the store; the view handles UI only.
    const { data } = await login(credentials)
    setSession(data.token, data.member, data.roles)
    return data
  }

  async function signInWithGoogle(credential) {
    const { data } = await googleLogin({ credential })
    setSession(data.token, data.member, data.roles)
    return data
  }

  async function linkGoogleSignIn(credential, password) {
    const { data } = await linkGoogleAccount({ credential, password })
    setSession(data.token, data.member, data.roles)
    return data
  }

  function updateMember(updatedMember) {
    // Keep the Header and other components synchronized after profile updates.
    member.value = updatedMember || null
  }

  function signOut() {
    setSession('', null, [])
  }

  return {
    token,
    member,
    roles,
    isAuthenticated,
    isSeller,
    hasRole,
    signIn,
    signInWithGoogle,
    linkGoogleSignIn,
    signOut,
    updateMember,
  }
}, {
  // Keep only authentication data; page state and cart data are not persisted here.
  persist: {
    key: AUTH_STORAGE_KEY,
    storage: sessionStorage,
    pick: ['token', 'member', 'roles'],
  },
})
