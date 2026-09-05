<script setup>
import { RouterView } from 'vue-router'
import DinoChatWidget from '@/components/chat/DinoChatWidget.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import PrimaryNav from '@/components/layout/PrimaryNav.vue'
import UtilityBar from '@/components/layout/UtilityBar.vue'
</script>

<template>
  <div class="layout-shell d-flex min-vh-100 flex-column">
    <UtilityBar />
    <AppHeader />
    <PrimaryNav />
    <main class="flex-grow-1"><RouterView /></main>
    <DinoChatWidget />
    <AppFooter />
  </div>
</template>
