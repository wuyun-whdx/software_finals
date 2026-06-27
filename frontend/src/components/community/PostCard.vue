<script setup lang="ts">
import { Heart, MessageCircle, Star, Trash2 } from 'lucide-vue-next'

defineProps<{
  id: number
  authorName: string
  content: string
  domainTag: string
  styleTags: string[]
  likeCount: number
  commentCount: number
  favoriteCount: number
  compatibility: number
  showCompatibility: boolean
  createdAt: string
  canDelete?: boolean
}>()

const emit = defineEmits<{
  (e: 'delete', id: number): void
}>()

function formatTime(value: string) {
  return new Date(value).toLocaleDateString()
}
</script>

<template>
  <div class="card post-card-wrapper">
    <RouterLink :to="`/community/post/${id}`" class="post-card-link">
      <div class="split">
        <div>
          <p class="eyebrow">{{ authorName }} · {{ formatTime(createdAt) }}</p>
          <p class="post-excerpt">{{ content.slice(0, 120) }}{{ content.length > 120 ? '...' : '' }}</p>
        </div>
        <span v-if="showCompatibility" class="score-pill" :title="`你的契合度：${compatibility}%`">{{ compatibility }}%</span>
      </div>
      <div class="tag-row">
        <span>{{ domainTag }}</span>
        <span v-for="tag in styleTags" :key="tag">{{ tag }}</span>
      </div>
      <div class="post-meta">
        <span><Heart :size="14" class="icon-sm" /> {{ likeCount }}</span>
        <span><MessageCircle :size="14" class="icon-sm" /> {{ commentCount }}</span>
        <span><Star :size="14" class="icon-sm" /> {{ favoriteCount }}</span>
      </div>
    </RouterLink>
    <button
      v-if="canDelete"
      class="post-delete-btn"
      type="button"
      title="删除帖子"
      @click.stop.prevent="emit('delete', id)"
    >
      <Trash2 :size="14" />
    </button>
  </div>
</template>

<style scoped>
.post-card-wrapper {
  position: relative;
  padding: 0;
}
.post-card-link {
  display: block;
  padding: 18px;
  color: inherit;
  text-decoration: none;
}
.post-delete-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 6px;
  border: none;
  background: none;
  color: var(--muted);
  cursor: pointer;
  border-radius: 6px;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}
.post-card-wrapper:hover .post-delete-btn {
  opacity: 1;
}
.post-delete-btn:hover {
  color: var(--danger);
  background: var(--error-bg);
}
</style>
