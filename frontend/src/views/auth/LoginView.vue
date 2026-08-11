<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const form = ref({ email: '', password: '' })
const fieldErrors = ref({})
const apiError = ref('')
const isSubmitting = ref(false)

const registeredMessage = route.query.registered ? '註冊成功，請使用新帳號登入。' : ''

function validate() {
  const errors = {}
  if (!form.value.email.trim()) errors.email = '請輸入 Email。'
  else if (!/^\S+@\S+\.\S+$/.test(form.value.email)) errors.email = '請輸入有效的 Email。'
  if (!form.value.password) errors.password = '請輸入密碼。'
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function getErrorMessage(error) {
  if (!error.response) {
    return '無法連線到伺服器，請確認後端是否已啟動。'
  }

  const { data, status } = error.response
  if (status === 401) return 'Email 或密碼錯誤。'
  if (typeof data === 'string' && data.trim()) return data
  return data?.message || '登入失敗，請稍後再試。'
}

async function submit() {
  apiError.value = ''
  if (!validate()) return

  isSubmitting.value = true
  try {
    const { data } = await login(form.value)
    if (data.token) localStorage.setItem('token', data.token)
    if (data.member) localStorage.setItem('member', JSON.stringify(data.member))
    await router.push('/member/overview')
  } catch (error) {
    apiError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page container py-5">
    <section class="auth-card dg-card mx-auto p-4 p-md-5" aria-labelledby="login-title">
      <p class="auth-eyebrow mb-2">DINO-GO MEMBER</p>
      <h1 id="login-title" class="dg-heading mb-2">登入頁</h1>
      <p class="text-muted mb-4">登入後查看訂單、收藏與會員資訊。</p>

      <div v-if="registeredMessage" class="alert alert-success" role="status">
        {{ registeredMessage }}
      </div>
      <div v-if="apiError" class="alert alert-danger" role="alert">{{ apiError }}</div>

      <form novalidate @submit.prevent="submit">
        <div class="mb-3">
          <label class="form-label" for="login-email">電子信箱</label>
          <input
            id="login-email"
            v-model="form.email"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.email }"
            type="email"
            autocomplete="email"
            placeholder="name@example.com"
            @input="delete fieldErrors.email"
          />
          <div v-if="fieldErrors.email" class="invalid-feedback">{{ fieldErrors.email }}</div>
        </div>

        <div class="mb-4">
          <label class="form-label" for="login-password">密碼</label>
          <input
            id="login-password"
            v-model="form.password"
            class="form-control"
            :class="{ 'is-invalid': fieldErrors.password }"
            type="password"
            autocomplete="current-password"
            placeholder="請輸入密碼"
            @input="delete fieldErrors.password"
          />
          <div v-if="fieldErrors.password" class="invalid-feedback">{{ fieldErrors.password }}</div>
        </div>

        <button
          class="btn dg-btn-primary login-submit w-100"
          type="submit"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '登入中…' : '登入' }}
        </button>
      </form>

      <div class="login-divider" aria-hidden="true"><span>或</span></div>

      <button class="btn login-google w-100" type="button">
        <i class="bi bi-google" aria-hidden="true"></i>
        使用 Google 登入
      </button>

      <p class="mt-4 mb-0 text-muted">
        還沒有帳號？
        <RouterLink to="/register" class="auth-link">前往註冊</RouterLink>
      </p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 640px;
  display: grid;
  place-items: center;
}
.auth-card {
  width: min(100%, 440px);
  box-shadow: var(--shadow-card);
}
.auth-eyebrow {
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.04em;
}
.auth-link {
  color: var(--color-primary-active);
  font-weight: 600;
  text-decoration: none;
}
.auth-link:hover {
  text-decoration: underline;
}
.login-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-4) 0;
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm);
}
.login-divider::before,
.login-divider::after {
  height: 1px;
  flex: 1;
  content: '';
  background: var(--color-border);
}
.login-google {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--color-text);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
}
.login-google:hover,
.login-google:focus-visible {
  color: var(--color-primary-700);
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}
.login-google:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
</style>
