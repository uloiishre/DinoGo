<script setup>
import { onMounted, ref } from 'vue'
import { getMemberProfile, updateMemberProfile } from '@/api/member'
import { useAuthStore } from '@/stores/auth'

const form = ref({
  email: '',
  lastName: '',
  firstName: '',
  birthDate: '',
  phone: '',
})
const isLoading = ref(true)
const hasLoadedProfile = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const authStore = useAuthStore()

function applyProfile(profile) {
  form.value = {
    email: profile.email ?? '',
    lastName: profile.lastName ?? '',
    firstName: profile.firstName ?? '',
    birthDate: profile.birthDate ?? '',
    phone: profile.phone ?? '',
  }
}

function getErrorMessage(error, fallback) {
  if (!error.response) return '目前無法連線，請稍後再試。'
  if (typeof error.response.data === 'string' && error.response.data.trim()) {
    return error.response.data
  }
  return error.response.data?.message || fallback
}

async function loadProfile() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const { data } = await getMemberProfile()
    applyProfile(data)
    hasLoadedProfile.value = true
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '會員資料載入失敗。')
  } finally {
    isLoading.value = false
  }
}

async function saveProfile() {
  errorMessage.value = ''
  successMessage.value = ''
  isSaving.value = true
  try {
    const { data } = await updateMemberProfile({
      lastName: form.value.lastName,
      firstName: form.value.firstName,
      birthDate: form.value.birthDate || null,
      phone: form.value.phone || null,
    })
    applyProfile(data)
    // 更新成功後同步 authStore，Header 等共用元件會立即拿到新資料。
    authStore.updateMember(data)
    successMessage.value = '會員資料已更新。'
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '會員資料更新失敗。')
  } finally {
    isSaving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <main class="container py-5">
    <section class="dg-card mx-auto p-4 p-md-5" aria-labelledby="profile-title">
      <p class="mb-2 text-muted">MEMBER PROFILE</p>
      <h1 id="profile-title" class="h3 mb-4">會員資料</h1>

      <div v-if="isLoading" class="text-muted" role="status">載入會員資料中...</div>
      <div v-else-if="hasLoadedProfile">
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
        <div v-if="successMessage" class="alert alert-success" role="status">{{ successMessage }}</div>

        <form @submit.prevent="saveProfile">
          <div class="row g-3">
            <div class="col-12">
              <label class="form-label" for="profile-email">Email</label>
              <input id="profile-email" v-model="form.email" class="form-control" type="email" readonly />
            </div>
            <div class="col-md-6">
              <label class="form-label" for="profile-last-name">姓氏</label>
              <input id="profile-last-name" v-model.trim="form.lastName" class="form-control" required maxlength="50" />
            </div>
            <div class="col-md-6">
              <label class="form-label" for="profile-first-name">名字</label>
              <input id="profile-first-name" v-model.trim="form.firstName" class="form-control" required maxlength="50" />
            </div>
            <div class="col-md-6">
              <label class="form-label" for="profile-birth-date">生日</label>
              <input id="profile-birth-date" v-model="form.birthDate" class="form-control" type="date" />
            </div>
            <div class="col-md-6">
              <label class="form-label" for="profile-phone">電話</label>
              <input id="profile-phone" v-model.trim="form.phone" class="form-control" maxlength="20" />
            </div>
          </div>
          <button class="btn btn-primary mt-4" type="submit" :disabled="isSaving">
            {{ isSaving ? '儲存中...' : '儲存變更' }}
          </button>
        </form>
      </div>
      <div v-else>
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
        <button class="btn btn-outline-primary" type="button" @click="loadProfile">重新載入</button>
      </div>
    </section>
  </main>
</template>
