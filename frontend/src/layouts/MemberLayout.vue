<script setup>
import { RouterView } from 'vue-router'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import PrimaryNav from '@/components/layout/PrimaryNav.vue'
import UtilityBar from '@/components/layout/UtilityBar.vue'
import MemberNav from '@/components/member/MemberNav.vue'
</script>

<template>
  <div class="layout-shell d-flex min-vh-100 flex-column">
    <UtilityBar />
    <AppHeader />
    <PrimaryNav />
    <main class="container flex-grow-1 py-5">
      <div class="row g-4">
        <aside class="col-12 col-lg-3"><MemberNav /></aside>
        <section class="col-12 col-lg-9"><RouterView /></section>
      </div>
    </main>
    <AppFooter />
  </div>
</template>
