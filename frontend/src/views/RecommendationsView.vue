<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Sparkles } from 'lucide-vue-next'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import RegionSelector from '../components/RegionSelector.vue'
import type { LocationRecommendation } from '../types'
import { listRecommendations, submitFeedback } from '../services/recommendationService'
import { getMyRegion } from '../services/regionService'
import { postApi } from '../api'

const route = useRoute()

const tabs: Array<{ value: string; label: string }> = [
  { value: 'food', label: '饮食' },
  { value: 'travel', label: '旅行' },
  { value: 'social', label: '社交' },
  { value: 'outfit', label: '穿搭' },
  { value: 'career', label: '生涯' }
]

const active = ref('food')
const items = ref<LocationRecommendation[]>([])
const submitted = ref<Record<number, string>>({})
const loading = ref(true)
const error = ref('')
const notice = ref('')
const hasRegion = ref(false)
const region = ref<{ province: string; city: string; district?: string } | null>(null)

function switchTab(tab: string) {
  active.value = tab
}

function getSceneLabel(scene: string): string {
  const labels: Record<string, string> = {
    food: '饮食推荐', travel: '旅游推荐', social: '社交推荐',
    outfit: '穿搭推荐', career: '生涯推荐'
  }
  return labels[scene] || scene
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listRecommendations(active.value, region.value || undefined)
  } catch (err) {
    error.value = (err as Error).message || '推荐加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function loadRegion() {
  try {
    region.value = await getMyRegion()
    hasRegion.value = !!region.value
  } catch {
    hasRegion.value = false
  }
}

function onRegionChanged() {
  loadRegion().then(() => load())
}

async function feedback(item: LocationRecommendation, rating: string) {
  try {
    await submitFeedback(item.id, rating, item.tags)
    submitted.value[item.id] = rating
    notice.value = rating === 'DISLIKE'
      ? '已收到反馈，后续会减少相似类型推荐。'
      : '已收到反馈，后续会优先推荐相似内容。'
    await load()
  } catch (err) {
    error.value = (err as Error).message || '反馈提交失败。'
  }
}

function feedbackLabel(rating: string) {
  return rating === 'LIKE' ? '喜欢' : rating === 'NEUTRAL' ? '一般' : '不喜欢'
}

function isAi(item: LocationRecommendation): boolean {
  return item.source === 'ai'
}

// ---- Share to community ----
const shareItemId = ref<number | null>(null)
const shareText = ref('')

function sceneToDomain(scene: string): string {
  return (scene || '').toUpperCase() || 'OTHER'
}

function openShare(item: LocationRecommendation) {
  shareItemId.value = item.id
  shareText.value = `【${item.title}】${item.description}\n\n#${item.tags?.join(' #') || ''}`
}

function cancelShare() {
  shareItemId.value = null
  shareText.value = ''
}

async function submitShare(item: LocationRecommendation) {
  try {
    await postApi.create({
      content: shareText.value,
      domainTag: sceneToDomain(item.scene)
    })
    shareItemId.value = null
    shareText.value = ''
    notice.value = '已发布到社区'
  } catch (err) {
    error.value = (err as Error).message || '发布失败'
  }
}

watch(active, load)
onMounted(() => {
  const tabParam = route.query.tab as string | undefined
  if (tabParam && tabs.some(t => t.value === tabParam)) {
    active.value = tabParam
  }
  loadRegion()
  load()
})
</script>

<template>
  <PageContainer
    eyebrow="生活推荐"
    title="把画像变成可以行动的生活建议"
    description="百分数表示当前推荐与你最近测评画像、历史反馈和后台规则的综合适配度。"
  >
    <template #actions>
      <div class="segmented">
        <button v-for="tab in tabs" :key="tab.value" :class="{ active: active === tab.value }" @click="switchTab(tab.value)">
          {{ tab.label }}
        </button>
      </div>
    </template>

    <RegionSelector @update:has-region="hasRegion = $event" @region-changed="onRegionChanged" />

    <div v-if="notice" class="notice">{{ notice }}</div>
    <div v-if="error" class="error">{{ error }}</div>
    <LoadingState v-if="loading" message="正在计算推荐排序..." />

    <EmptyState
      v-else-if="error.includes('请先完成')"
      title="请先完成基础性格测试"
      description="完成测评后，系统会根据你的画像、场景偏好和历史反馈给出推荐。"
      action-label="去完成测评"
      action-to="/tests/personality"
    />

    <EmptyState
      v-else-if="!items.length"
      title="暂无推荐数据"
      description="当前场景没有可用推荐，请切换分类或稍后重试。"
    />

    <section v-else class="grid two">
      <article
        v-for="item in items"
        :key="item.id"
        class="card recommendation-card"
        :class="{ 'rec-card-ai': isAi(item) }"
      >
        <div v-if="isAi(item)" class="tag-row ai-tag-row">
          <span class="ai-tag-badge">
            <Sparkles :size="12" class="ai-tag-icon" /> AI 精准
          </span>
        </div>
        <div class="split">
          <div>
            <p class="eyebrow">{{ getSceneLabel(item.scene) }}</p>
            <h2>{{ item.title }}</h2>
          </div>
          <span class="score-pill" title="综合适配度">{{ item.score }}%</span>
        </div>
        <p v-if="item.address" class="address-line">{{ item.address }}</p>
        <p>{{ item.description }}</p>
        <p v-if="item.aiReason" class="ai-reason">{{ item.aiReason }}</p>
        <div class="tag-row">
          <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
        </div>
        <p v-if="submitted[item.id]" class="notice compact">已记录反馈：{{ feedbackLabel(submitted[item.id]) }}</p>
        <div class="toolbar">
          <button class="primary" type="button" @click="feedback(item, 'LIKE')">喜欢</button>
          <button class="secondary" type="button" @click="feedback(item, 'NEUTRAL')">一般</button>
          <button class="ghost" type="button" @click="feedback(item, 'DISLIKE')">不喜欢</button>
          <button class="ghost" type="button" @click="openShare(item)">分享</button>
        </div>

        <!-- Inline share form -->
        <div v-if="shareItemId === item.id" class="share-form">
          <textarea
            v-model="shareText"
            class="share-textarea"
            rows="3"
            placeholder="写点感想分享到社区..."
          ></textarea>
          <div class="share-actions">
            <button class="primary small" type="button" @click="submitShare(item)">发布</button>
            <button class="ghost small" type="button" @click="cancelShare()">取消</button>
          </div>
        </div>
      </article>
    </section>
  </PageContainer>
</template>

<style scoped>
/* ---- Share form ---- */
.share-form {
  margin-top: 12px;
  border-top: 1px solid var(--line);
  padding-top: 12px;
}
.share-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 0.88rem;
  font-family: inherit;
  resize: vertical;
  min-height: 64px;
}
.share-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
button.small {
  padding: 4px 14px;
  font-size: 0.82rem;
}

.address-line {
  font-size: 0.85rem;
  color: var(--muted);
  margin-bottom: 0.25rem;
}

.ai-reason {
  font-size: 0.85rem;
  color: var(--notice-text);
  font-style: italic;
  margin-top: 0.5rem;
  padding-top: 6px;
  border-top: 1px dashed rgba(240, 224, 176, 0.5);
}
</style>
