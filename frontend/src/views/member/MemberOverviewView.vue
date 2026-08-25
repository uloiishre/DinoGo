<script setup>
import { RouterLink } from 'vue-router'
import MemberOrderProgress from '@/components/member/MemberOrderProgress.vue'
import MemberQuickActions from '@/components/member/MemberQuickActions.vue'
import MemberSummaryCards from '@/components/member/MemberSummaryCards.vue'

const overviewDate = '2026 / 08 / 09'
</script>

<template>
  <section class="member-overview" aria-labelledby="member-overview-title">
    <div class="container member-overview__container">
      <header class="member-overview__header">
        <div>
          <p>MEMBER OVERVIEW · {{ overviewDate }}</p>
          <h1 id="member-overview-title">今天的會員摘要</h1>
        </div>
        <div class="member-overview__profile-action">
          <span>資料更新於 10:24</span>
          <RouterLink :to="{ name: 'MemberProfile' }">編輯個人資料</RouterLink>
        </div>
      </header>

      <MemberSummaryCards />

      <div class="member-overview__content">
        <MemberOrderProgress />
        <MemberQuickActions />
      </div>
    </div>
  </section>
</template>

<style scoped>
.member-overview {
  min-height: 720px;
  background: var(--color-bg);
}
.member-overview__container {
  max-width: 1440px;
  padding-top: 28px;
  padding-bottom: var(--space-8);
}
.member-overview__header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}
.member-overview__header p,
.member-overview__header h1 {
  margin: 0;
}
.member-overview__header p {
  color: var(--color-primary-active);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.member-overview__header h1 {
  margin-top: 5px;
  color: var(--color-text);
  font-family: var(--font-heading);
  font-size: var(--font-size-xl);
  font-weight: 700;
  line-height: var(--line-height-heading);
}
.member-overview__profile-action {
  display: flex;
  align-items: center;
  gap: 10px;
}
.member-overview__profile-action span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}
.member-overview__profile-action a {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  padding: 0 var(--space-3);
  color: var(--color-primary-active);
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}
.member-overview__profile-action a:hover {
  background: var(--color-primary-soft);
}
.member-overview__profile-action a:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus);
}
.member-overview__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 336px;
  gap: var(--space-5);
  margin-top: var(--space-5);
}
@media (max-width: 991.98px) {
  .member-overview__content {
    grid-template-columns: 1fr;
  }
  .member-overview__container {
    padding-inline: var(--space-5);
  }
}
@media (max-width: 767.98px) {
  .member-overview__container {
    padding: var(--space-5) var(--space-4) var(--space-7);
  }
  .member-overview__header {
    align-items: flex-start;
    flex-direction: column;
  }
  .member-overview__profile-action {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
