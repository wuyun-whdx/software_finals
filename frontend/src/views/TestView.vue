<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import type { Question } from '../types'
import { loadQuestions, submitTest } from '../services/testService'

const route = useRoute()
const router = useRouter()
const questionsByType = ref<Record<string, Question[]>>({})
const answers = ref<Record<string, number>>({})

/** 用 "type:id" 组合键避免跨模块题目 ID 冲突 */
function answerKey(q: { type: string; id: number }) {
  return `${q.type}:${q.id}`
}
const currentIndex = ref(0)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const notice = ref('')

const allQuestions = computed(() =>
  Object.values(questionsByType.value).flat().sort((a, b) => {
    const typeOrder = ['personality', 'food', 'travel', 'social']
    return typeOrder.indexOf(a.type) - typeOrder.indexOf(b.type)
  })
)
const currentQuestion = computed(() => allQuestions.value[currentIndex.value] || null)
const answeredCount = computed(() => allQuestions.value.filter((q) => answers.value[answerKey(q)]).length)
const totalProgress = computed(() =>
  allQuestions.value.length ? Math.round((answeredCount.value / allQuestions.value.length) * 100) : 0
)
const moduleKeys = ['personality', 'food', 'travel', 'social']
const moduleLabels: Record<string, string> = {
  personality: '基础性格', food: '饮食偏好', travel: '旅游偏好', social: '社交倾向'
}

function jumpToType(type: string) {
  const idx = allQuestions.value.findIndex((q) => q.type === type)
  if (idx >= 0) currentIndex.value = idx
}

function next() {
  error.value = ''
  if (!currentQuestion.value) return
  if (!answers.value[answerKey(currentQuestion.value)]) {
    error.value = '请先回答当前题目，再进入下一题。'
    return
  }
  currentIndex.value = Math.min(currentIndex.value + 1, allQuestions.value.length - 1)
}

function prev() {
  error.value = ''
  currentIndex.value = Math.max(currentIndex.value - 1, 0)
}

async function submit() {
  error.value = ''
  notice.value = ''
  const missing = allQuestions.value.length - answeredCount.value
  if (missing > 0) {
    error.value = `你还有 ${missing} 道题未完成，请完成后再生成报告。`
    return
  }
  if (!window.confirm('确认提交后将生成新的画像报告，并覆盖当前推荐依据。')) return
  submitting.value = true
  try {
    // Submit all four types
    for (const type of moduleKeys) {
      const typeQuestions = questionsByType.value[type] || []
      const typeAnswers = typeQuestions
        .filter((q) => answers.value[answerKey(q)])
        .map((q) => {
          const opt = q.options.find((o) => o.label === String(answers.value[answerKey(q)]))
          return { questionId: q.id, optionIds: opt ? [opt.id] : [] }
        })
      if (typeAnswers.length > 0) {
        await submitTest(type, typeAnswers)
      }
    }
    notice.value = '测试已提交，正在前往报告页。'
    setTimeout(() => router.push('/report'), 400)
  } catch (err) {
    error.value = (err as Error).message || '提交失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    questionsByType.value = await loadQuestions()
    jumpToType((route.params.type as string) || 'personality')
  } catch {
    error.value = '题目加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.type, (value) => jumpToType((value as string) || 'personality'))

function handleKeydown(e: KeyboardEvent) {
  if (loading.value || submitting.value || !currentQuestion.value) return
  // 仅在真正需要文字输入时忽略按键（radio/checkbox 不拦截）
  const el = e.target as HTMLElement
  const tag = el?.tagName
  if (
    tag === 'TEXTAREA' ||
    tag === 'SELECT' ||
    (tag === 'INPUT' && (el as HTMLInputElement).type !== 'radio' && (el as HTMLInputElement).type !== 'checkbox')
  )
    return

  const key = e.key
  if (key >= '1' && key <= '5') {
    e.preventDefault()
    answers.value[answerKey(currentQuestion.value)] = Number(key)
    error.value = ''
  } else if (key === 'Enter') {
    e.preventDefault()
    if (!answers.value[answerKey(currentQuestion.value)]) {
      error.value = '请先回答当前题目，再进入下一题。'
      return
    }
    if (currentIndex.value < allQuestions.value.length - 1) {
      next()
    } else {
      submit()
    }
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <PageContainer
    eyebrow="测试中心"
    title="完成四类测评，生成你的生活画像"
    description="请按真实感受选择 1-5 级量表（可按键盘 1-5 数字键快速选择）。所有测试结果由后端计算并持久化。"
  >
    <template #actions>
      <span class="progress-label">总进度 {{ totalProgress }}%</span>
    </template>

    <div class="progress-track"><span :style="{ width: `${totalProgress}%` }"></span></div>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="notice" class="notice">{{ notice }}</div>
    <LoadingState v-if="loading" message="正在加载题库..." />

    <EmptyState
      v-else-if="!allQuestions.length"
      title="题目加载失败，请稍后重试。"
      description="当前题库为空，请刷新页面或联系管理员补充题库。"
      action-label="刷新"
      action-to="/tests/personality"
    />

    <section v-else class="test-layout">
      <aside class="module-list">
        <button
          v-for="key in moduleKeys"
          :key="key"
          type="button"
          :class="{ active: currentQuestion?.type === key }"
          @click="jumpToType(key)"
        >
          <strong>{{ moduleLabels[key] }}测试</strong>
          <span>{{ questionsByType[key]?.length || 0 }} 题</span>
        </button>
      </aside>

      <article v-if="currentQuestion" class="panel question-panel">
        <div class="split">
          <p class="eyebrow">{{ moduleLabels[currentQuestion.type] }}测试</p>
          <strong>{{ currentIndex + 1 }} / {{ allQuestions.length }}</strong>
        </div>
        <h2>{{ currentQuestion.content }}</h2>
        <div class="scale-grid">
          <label v-for="n in 5" :key="n" :class="{ selected: answers[answerKey(currentQuestion)] === n }">
            <input v-model="answers[answerKey(currentQuestion)]" type="radio" :name="answerKey(currentQuestion)" :value="n" />
            <span>{{ n }}</span>
            <strong>{{ ['非常不同意','不太同意','一般','比较同意','非常同意'][n-1] }}</strong>
          </label>
        </div>
        <div class="toolbar question-actions">
          <button class="ghost" type="button" :disabled="currentIndex === 0" @click="prev">上一题</button>
          <button v-if="currentIndex < allQuestions.length - 1" class="primary" type="button" @click="next">下一题</button>
          <button v-else class="primary" type="button" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中...' : '生成报告' }}
          </button>
        </div>
      </article>
    </section>
  </PageContainer>
</template>
