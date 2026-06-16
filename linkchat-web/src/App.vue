<template>
  <LcToast />
  <router-view />
</template>

<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
import { onMounted } from 'vue'
import LcToast from '@/components/design/LcToast.vue'

const themeStore = useThemeStore()

onMounted(() => {
  themeStore.applyTheme()
})
</script>
