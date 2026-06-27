<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Radar } from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const navItems = computed(() => [
  { path: '/', label: '首页' },
  { path: '/tests/personality', label: '测试' },
  { path: '/report', label: '报告' },
  { path: '/recommendations', label: '推荐' },
  { path: '/community', label: '社区' },
  { path: '/match', label: '匹配' },
  { path: '/profile', label: '我的' },
  ...(auth.isAdmin ? [{ path: '/admin', label: '后台', admin: true }] : [])
])

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path.split('/')[1] ? `/${path.split('/')[1]}` : path)
}

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <header class="app-header">
    <RouterLink class="brand" to="/">
      <span class="brand-mark"><Radar :size="20" /></span>
      <span>
        <strong>性格雷达</strong>
        <small>生活指南</small>
      </span>
    </RouterLink>

    <nav class="desktop-nav" aria-label="主导航">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        :class="{ active: isActive(item.path) }"
      >
        {{ item.label }}
        <span v-if="item.admin" class="admin-badge">管理</span>
      </RouterLink>
    </nav>

    <div class="account-actions">
      <RouterLink v-if="!auth.isAuthed" class="button secondary" to="/login">登录</RouterLink>
      <button v-else class="ghost small" type="button" @click="logout">退出</button>
    </div>
  </header>

  <nav class="mobile-tabs" aria-label="移动端导航">
    <RouterLink
      v-for="item in navItems.slice(0, 6)"
      :key="item.path"
      :to="item.path"
      :class="{ active: isActive(item.path) }"
    >
      {{ item.label }}
    </RouterLink>
  </nav>
</template>
