<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '../../components/common/EmptyState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import PageContainer from '../../components/common/PageContainer.vue'
import PostCard from '../../components/community/PostCard.vue'
import { postApi } from '../../api'
import type { PostResponse } from '../../types'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const sort = ref('recommend')
const domain = ref('')
const posts = ref<PostResponse[]>([])
const loading = ref(true)
const error = ref('')

const domains = ['', 'FOOD', 'TRAVEL', 'SOCIAL', 'OUTFIT', 'CAREER', 'OTHER']
const domainLabels: Record<string, string> = {
  '': '全部领域', FOOD: '饮食', TRAVEL: '旅行', SOCIAL: '社交', OUTFIT: '穿搭', CAREER: '生涯', OTHER: '其他'
}

const tabs = [
  { value: 'food', label: '饮食' },
  { value: 'travel', label: '旅行' },
  { value: 'social', label: '社交' },
  { value: 'outfit', label: '穿搭' },
  { value: 'career', label: '生涯' },
  { value: 'community', label: '社区' }
]

function switchTab(tab: string) {
  if (tab === 'community') return
  router.push({ path: '/recommendations', query: { tab } })
}

function canDelete(post: PostResponse): boolean {
  return auth.isAdmin || post.author.id === auth.user?.id
}

async function handleDelete(id: number) {
  try {
    await postApi.delete(id)
    posts.value = posts.value.filter(p => p.id !== id)
  } catch (err) {
    error.value = (err as Error).message || '删除失败'
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await postApi.list(sort.value, domain.value || undefined, 0)
    posts.value = result.items
  } catch (err) {
    error.value = (err as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageContainer eyebrow="个性化社区" title="发现与你契合的动态" description="基于人格向量匹配推荐的UGC内容社区。">
    <template #actions>
      <RouterLink class="button" to="/community/create">发布动态</RouterLink>
    </template>

    <!-- Unified 6-tab navigation bar -->
    <div class="segmented community-tabs-bar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="{ active: tab.value === 'community' }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="toolbar filter-bar">
      <div class="segmented">
        <button :class="{ active: sort === 'recommend' }" @click="sort = 'recommend'; load()">推荐</button>
        <button :class="{ active: sort === 'latest' }" @click="sort = 'latest'; load()">最新</button>
      </div>
      <select v-model="domain" class="filter-select" @change="load()">
        <option v-for="d in domains" :key="d" :value="d">{{ domainLabels[d] }}</option>
      </select>
    </div>

    <LoadingState v-if="loading" message="正在加载社区动态..." />

    <EmptyState
      v-else-if="!posts.length"
      title="暂无社区动态"
      description="还没有人发布帖子，成为第一个分享的人吧！"
      action-label="发布动态"
      action-to="/community/create"
    />

    <section v-else class="grid compact">
      <PostCard
        v-for="post in posts" :key="post.id"
        :id="post.id"
        :author-name="post.author.displayName"
        :content="post.content"
        :domain-tag="post.domainTag"
        :style-tags="post.styleTags"
        :like-count="post.likeCount"
        :comment-count="post.commentCount"
        :favorite-count="post.favoriteCount"
        :compatibility="post.compatibility"
        :show-compatibility="post.showCompatibility"
        :created-at="post.createdAt"
        :can-delete="canDelete(post)"
        @delete="handleDelete"
      />
    </section>
  </PageContainer>
</template>

<style scoped>
.community-tabs-bar {
  margin-bottom: 16px;
}
</style>
