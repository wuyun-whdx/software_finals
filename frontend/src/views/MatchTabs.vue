<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

function isActive(path: string) {
  if (path === '/match') return route.path === '/match'
  return route.path.startsWith(path)
}
</script>

<template>
  <div class="match-shell">
    <nav class="match-subnav">
      <RouterLink to="/match" :class="{ active: isActive('/match') && !route.path.startsWith('/match/friends') && !route.path.startsWith('/match/chat') }">
        双人适配
      </RouterLink>
      <RouterLink to="/match/friends" :class="{ active: isActive('/match/friends') }">
        好友
      </RouterLink>
      <RouterLink to="/match/chat" :class="{ active: isActive('/match/chat') }">
        聊天
      </RouterLink>
    </nav>
    <RouterView />
  </div>
</template>

<style scoped>
.match-shell {
  min-height: 100vh;
}
.match-subnav {
  /* 映射到全局设计系统变量 */
  --accent: var(--blip);
  --text: var(--ink);
  --border: var(--line);

  display: flex;
  gap: 0;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
}
.match-subnav a {
  padding: 12px 20px;
  text-decoration: none;
  color: var(--muted);
  font-size: 0.95rem;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;
}
.match-subnav a:hover {
  color: var(--text);
}
.match-subnav a.active {
  color: var(--text);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
</style>
