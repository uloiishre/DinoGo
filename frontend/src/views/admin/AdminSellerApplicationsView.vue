<script setup>
import { computed, onMounted, ref } from 'vue'
import { listSellerApplications } from '@/api/sellerApplicationApi'

const searchQuery = ref('')
const feedback = ref('')

const applications = ref([])

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-TW') : '-')

const loadApplications = async () => {
  try {
    const response = await listSellerApplications('PENDING')
    applications.value = response.data.map((application) => ({
      id: application.applicationId,
      applicant: `會員 #${application.memberId}`,
      email: '-',
      storeName: application.storeName,
      submittedAt: formatDate(application.createdAt),
    }))
  } catch (error) {
    feedback.value = error.response?.data?.message || '申請列表載入失敗。'
  }
}

onMounted(loadApplications)

const filteredApplications = computed(() => {
  const keyword = searchQuery.value.trim().toLocaleLowerCase()
  if (!keyword) return applications.value

  return applications.value.filter((application) =>
    [application.applicant, application.email, application.storeName].some((value) =>
      value.toLocaleLowerCase().includes(keyword),
    ),
  )
})
</script>

<template>
  <section class="admin-seller-applications" aria-labelledby="admin-seller-applications-title">
    <header class="admin-seller-applications__header">
      <div>
        <p class="admin-seller-applications__eyebrow">商家會員</p>
        <h1 id="admin-seller-applications-title">商家申請審核</h1>
        <p>依申請資料與會員資訊完成核准或駁回。</p>
      </div>
      <strong>待處理 {{ applications.length }} 件</strong>
    </header>

    <div class="admin-seller-applications__tabs" role="tablist" aria-label="申請狀態">
      <button class="is-active" type="button" role="tab" aria-selected="true">待審核（12）</button>
      <button type="button" role="tab" aria-selected="false">已核准</button>
      <button type="button" role="tab" aria-selected="false">已駁回</button>
    </div>

    <div class="admin-seller-applications__filters">
      <label class="admin-seller-applications__search" for="seller-application-search">
        <i class="bi bi-search" aria-hidden="true"></i>
        <input
          id="seller-application-search"
          v-model="searchQuery"
          type="search"
          placeholder="搜尋店鋪名稱、會員姓名或 Email"
        />
      </label>
      <div class="admin-seller-applications__filter-actions">
        <button type="button">依送出時間排序</button>
        <button type="button">匯出清單</button>
      </div>
    </div>

    <div class="admin-seller-applications__table-card">
      <div
        class="admin-seller-applications__table admin-seller-applications__table--header"
        role="row"
      >
        <span role="columnheader">申請人</span>
        <span role="columnheader">店鋪名稱</span>
        <span role="columnheader">送出時間</span>
        <span role="columnheader">狀態</span>
        <span role="columnheader">操作</span>
      </div>

      <div
        v-for="application in filteredApplications"
        :key="application.id"
        class="admin-seller-applications__table admin-seller-applications__table--row"
        role="row"
      >
        <div class="admin-seller-applications__applicant" role="cell">
          <strong>{{ application.applicant }}</strong>
          <span>{{ application.email }}</span>
        </div>
        <span role="cell">{{ application.storeName }}</span>
        <span role="cell">{{ application.submittedAt }}</span>
        <span role="cell"><b>待審核</b></span>
        <span class="admin-seller-applications__review" role="cell">
          <RouterLink
            class="admin-seller-applications__review-link dg-btn-primary dg-focus-ring"
            :to="{ name: 'AdminSellerApplicationDetail', params: { id: application.id } }"
          >
            審核申請
          </RouterLink>
        </span>
      </div>

      <p v-if="feedback" class="admin-seller-applications__empty" role="status">{{ feedback }}</p>
      <p v-else-if="!filteredApplications.length" class="admin-seller-applications__empty" role="status">
        找不到符合條件的申請。
      </p>
    </div>

    <footer class="admin-seller-applications__footer">
      <p>顯示 {{ filteredApplications.length }} 筆，共 {{ applications.length }} 筆待審核申請</p>
      <span aria-label="分頁"
        >‹&nbsp;&nbsp;1&nbsp;&nbsp;2&nbsp;&nbsp;3&nbsp;&nbsp;4&nbsp;&nbsp;›</span
      >
    </footer>
  </section>
</template>

<style scoped>
.admin-seller-applications {
  display: grid;
  gap: var(--space-5);
  color: var(--color-text);
  font-family: var(--font-body);
}

.admin-seller-applications__header,
.admin-seller-applications__filters,
.admin-seller-applications__footer {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-5);
}

.admin-seller-applications__header > div {
  display: grid;
  gap: 6px;
}

.admin-seller-applications p,
h1 {
  margin: 0;
}

.admin-seller-applications__eyebrow,
.admin-seller-applications__header > strong {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

h1 {
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}

.admin-seller-applications__header > div > p:last-child,
.admin-seller-applications__footer p {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.admin-seller-applications__tabs,
.admin-seller-applications__filter-actions {
  display: flex;
  gap: var(--space-2);
}

.admin-seller-applications__tabs button,
.admin-seller-applications__filter-actions button {
  min-height: 38px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  color: var(--color-text-muted);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
}

.admin-seller-applications__tabs button.is-active {
  border-color: var(--color-primary-active);
  color: var(--color-surface);
  background: var(--color-primary-active);
  font-weight: 700;
}

.admin-seller-applications__filter-actions button {
  min-height: 44px;
  padding-inline: 18px;
  color: var(--color-text);
  font-weight: 600;
}

.admin-seller-applications button:hover,
.admin-seller-applications button:focus-visible {
  border-color: var(--color-primary);
}

.admin-seller-applications button:focus-visible,
.admin-seller-applications__search input:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}

.admin-seller-applications__search {
  display: flex;
  width: 360px;
  min-height: 42px;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  color: var(--color-text-muted);
  background: var(--color-surface);
}

.admin-seller-applications__search i {
  font-size: 18px;
}

.admin-seller-applications__search input {
  width: 100%;
  border: 0;
  color: var(--color-text);
  background: transparent;
  font: inherit;
  font-size: 13px;
}

.admin-seller-applications__search input::placeholder {
  color: var(--color-text-muted);
  opacity: 1;
}

.admin-seller-applications__table-card {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.admin-seller-applications__table {
  display: grid;
  grid-template-columns: 260px 220px 180px 140px minmax(136px, 1fr);
  align-items: center;
  padding: 0 var(--space-5);
}

.admin-seller-applications__table--header {
  min-height: 48px;
  color: var(--color-text);
  background: var(--color-bg);
  font-size: 13px;
  font-weight: 700;
}

.admin-seller-applications__table--row {
  min-height: 72px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 13px;
}

.admin-seller-applications__table--row:last-of-type {
  border-bottom: 0;
}

.admin-seller-applications__applicant {
  display: grid;
  gap: 2px;
}

.admin-seller-applications__applicant strong {
  color: var(--color-text);
  font-weight: 700;
}

.admin-seller-applications__applicant span {
  color: var(--color-text-muted);
}

.admin-seller-applications__table--row b {
  display: inline-flex;
  border-radius: 14px;
  padding: 5px 10px;
  color: var(--color-primary-active);
  background: var(--color-bg);
  font-weight: 700;
}

.admin-seller-applications__review {
  display: flex;
  justify-content: end;
}

.admin-seller-applications__review-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: 0 18px;
  color: var(--color-surface);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: 600;
  text-decoration: none;
}

.admin-seller-applications__empty {
  padding: var(--space-6);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}

.admin-seller-applications__footer {
  align-items: center;
}

.admin-seller-applications__footer > span {
  color: var(--color-primary-active);
  font-size: var(--font-size-sm);
}

@media (max-width: 1100px) {
  .admin-seller-applications__table-card {
    overflow-x: auto;
  }

  .admin-seller-applications__table {
    min-width: 976px;
  }
}

@media (max-width: 760px) {
  .admin-seller-applications__header,
  .admin-seller-applications__filters {
    align-items: start;
    flex-direction: column;
  }

  .admin-seller-applications__search {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .admin-seller-applications__tabs,
  .admin-seller-applications__filter-actions {
    width: 100%;
    overflow-x: auto;
  }

  .admin-seller-applications__tabs button,
  .admin-seller-applications__filter-actions button {
    flex: 0 0 auto;
  }

  .admin-seller-applications__footer {
    align-items: start;
    flex-direction: column;
  }
}
</style>
