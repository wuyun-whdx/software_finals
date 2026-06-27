<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Users, HelpCircle, Lightbulb, MessageSquare, HeartHandshake } from 'lucide-vue-next'
import { adminApi } from '../api'
import BaseModal from '../components/common/BaseModal.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import { formatTime } from '../utils/format'
import { getErrorMessage } from '../utils/errors'
import type { AdminDashboard, AdminUser, Question, Recommendation, RecommendationRule } from '../types'

const dashboard = ref<AdminDashboard | null>(null)
const users = ref<AdminUser[]>([])
const questions = ref<Question[]>([])
const recommendations = ref<Recommendation[]>([])
const rules = ref<RecommendationRule[]>([])
const feedbacks = ref<any[]>([])
const logs = ref<any[]>([])
const error = ref('')
const notice = ref('')
const loading = ref(false)

// Create/edit modal state
const showQuestionForm = ref(false)
const showRecForm = ref(false)
const showRuleForm = ref(false)
const editingQuestion = ref<Partial<Question> | null>(null)
const editingRec = ref<Partial<Recommendation> | null>(null)
const newRule = ref({ tag: '', label: '', weight: 4 })

const stats = computed(() => dashboard.value?.stats)

async function load() {
  error.value = ''
  loading.value = true
  try {
    const results = await Promise.all([
      adminApi.dashboard(), adminApi.users(), adminApi.questions(),
      adminApi.recommendationItems(), adminApi.recommendationRules(),
      adminApi.feedback(), adminApi.logs()
    ])
    dashboard.value = results[0]; users.value = results[1]; questions.value = results[2]
    recommendations.value = results[3]; rules.value = results[4]
    feedbacks.value = results[5]; logs.value = results[6]
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

// --- User ---
async function toggleUser(user: AdminUser) {
  const updated = await adminApi.updateUser(user.id, { active: !user.active })
  users.value = users.value.map((item) => item.id === user.id ? updated : item)
  notice.value = `${updated.displayName} 已${updated.active ? '恢复' : '停用'}`
}
async function setUserRole(user: AdminUser, role: string) {
  const updated = await adminApi.updateUser(user.id, { role })
  users.value = users.value.map((item) => item.id === user.id ? updated : item)
  notice.value = `${updated.displayName} 角色已更新为 ${role}`
}

// --- Question CRUD ---
function openNewQuestion() { editingQuestion.value = { type: 'personality', content: '', active: true }; showQuestionForm.value = true }
function openEditQuestion(q: Question) { editingQuestion.value = { ...q }; showQuestionForm.value = true }
async function saveQuestion() {
  const q = editingQuestion.value!
  const payload = { type: q.type!, content: q.content!, active: q.active, options: [{ label: '1', content: '非常不同意', weights: { OPENNESS: 1 } }, { label: '2', content: '不太同意', weights: { OPENNESS: 2 } }, { label: '3', content: '一般', weights: { OPENNESS: 3 } }, { label: '4', content: '比较同意', weights: { OPENNESS: 4 } }, { label: '5', content: '非常同意', weights: { OPENNESS: 5 } }] }
  if ((q as any).id) await adminApi.updateQuestion((q as any).id, payload)
  else await adminApi.createQuestion(payload)
  showQuestionForm.value = false; notice.value = '题目已保存'; await load()
}
async function deleteQuestion(id: number) {
  if (!confirm('确认删除该题目？')) return
  await adminApi.deleteQuestion(id)
  notice.value = '题目已删除'; await load()
}

// --- Recommendation CRUD ---
function openNewRec() { editingRec.value = { scene: 'food', title: '', description: '', tags: [], baseScore: 65, active: true }; showRecForm.value = true }
function openEditRec(r: Recommendation) { editingRec.value = { ...r }; showRecForm.value = true }
async function saveRec() {
  const r = editingRec.value!
  const payload = { scene: r.scene!, title: r.title!, description: r.description!, tags: r.tags || [], baseScore: r.baseScore, active: r.active }
  if ((r as any).id) await adminApi.updateRecommendation((r as any).id, payload)
  else await adminApi.createRecommendation(payload)
  showRecForm.value = false; notice.value = '推荐项已保存'; await load()
}
async function deleteRec(id: number) {
  if (!confirm('确认删除该推荐项？')) return
  await adminApi.deleteRecommendation(id)
  notice.value = '推荐项已删除'; await load()
}

// --- Rule CRUD ---
async function toggleRule(rule: RecommendationRule) {
  const updated = await adminApi.updateRecommendationRule(rule.id, { ...rule, active: !rule.active })
  rules.value = rules.value.map((item) => item.id === rule.id ? updated : item)
  notice.value = `规则「${updated.label}」已${updated.active ? '启用' : '停用'}`
}
async function createRule() {
  await adminApi.createRecommendationRule(newRule.value)
  newRule.value = { tag: '', label: '', weight: 4 }; showRuleForm.value = false
  notice.value = '规则已创建'; await load()
}
async function deleteRule(id: number) {
  if (!confirm('确认删除该规则？')) return
  await adminApi.deleteRecommendationRule(id)
  notice.value = '规则已删除'; await load()
}

onMounted(load)
</script>

<template>
  <PageContainer
    eyebrow="后台管理"
    title="内容、用户和推荐规则管理"
    description="管理题库、推荐项、推荐规则、用户状态和系统日志。"
  >
    <template #actions>
      <button class="secondary" type="button" @click="load">刷新</button>
    </template>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="notice" class="notice">{{ notice }}</div>
    <LoadingState v-if="loading" message="正在加载后台数据..." />

    <template v-else>
      <!-- Stats -->
      <section class="grid five">
        <div class="card metric"><Users :size="24" class="metric-icon" /><strong>{{ stats?.users ?? 0 }}</strong><span>用户</span></div>
        <div class="card metric"><HelpCircle :size="24" class="metric-icon" /><strong>{{ stats?.questions ?? 0 }}</strong><span>题目</span></div>
        <div class="card metric"><Lightbulb :size="24" class="metric-icon" /><strong>{{ stats?.recommendations ?? 0 }}</strong><span>推荐项</span></div>
        <div class="card metric"><MessageSquare :size="24" class="metric-icon" /><strong>{{ stats?.feedbacks ?? 0 }}</strong><span>反馈</span></div>
        <div class="card metric"><HeartHandshake :size="24" class="metric-icon" /><strong>{{ stats?.matches ?? 0 }}</strong><span>匹配</span></div>
      </section>

      <!-- Users + Rules -->
      <section class="grid two section-gap">
        <article class="panel admin-panel">
          <h2>用户管理</h2>
          <EmptyState v-if="!users.length" title="暂无用户数据" description="用户注册后会显示在此列表中。" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>手机号</th><th>昵称</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="user in users" :key="user.id">
                  <td>{{ user.phone }}</td><td>{{ user.displayName }}</td>
                  <td>
                    <select :value="user.role" @change="setUserRole(user, ($event.target as HTMLSelectElement).value)" class="filter-select">
                      <option value="USER">USER</option><option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                  <td>{{ user.active ? '启用' : '停用' }}</td>
                  <td><button class="secondary small" type="button" @click="toggleUser(user)">{{ user.active ? '停用' : '恢复' }}</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel admin-panel">
          <div class="split"><h2>推荐规则</h2><button class="primary small" type="button" @click="showRuleForm = true">+ 新建</button></div>
          <EmptyState v-if="!rules.length" title="暂无推荐规则" description="点击「新建」添加推荐权重规则。" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>标签</th><th>名称</th><th>权重</th><th>状态</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="rule in rules" :key="rule.id">
                  <td>{{ rule.tag }}</td><td>{{ rule.label }}</td><td>{{ rule.weight }}</td>
                  <td>{{ rule.active ? '启用' : '停用' }}</td>
                  <td>
                    <button class="secondary small" type="button" @click="toggleRule(rule)">{{ rule.active ? '停用' : '启用' }}</button>
                    <button class="danger small" type="button" @click="deleteRule(rule.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <!-- Questions + Recommendations -->
      <section class="grid two section-gap">
        <article class="panel admin-panel">
          <div class="split"><h2>题库管理</h2><button class="primary small" type="button" @click="openNewQuestion">+ 新建</button></div>
          <EmptyState v-if="!questions.length" title="暂无题目" description="点击「新建」添加题目" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>类型</th><th>题目</th><th>选项</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="q in questions" :key="q.id">
                  <td>{{ q.type }}</td><td>{{ q.content.slice(0, 40) }}{{ q.content.length > 40 ? '...' : '' }}</td>
                  <td>{{ q.options.length }}</td>
                  <td>
                    <button class="secondary small" type="button" @click="openEditQuestion(q)">编辑</button>
                    <button class="danger small" type="button" @click="deleteQuestion(q.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel admin-panel">
          <div class="split"><h2>推荐库管理</h2><button class="primary small" type="button" @click="openNewRec">+ 新建</button></div>
          <EmptyState v-if="!recommendations.length" title="暂无推荐项" description="点击「新建」添加推荐项" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>场景</th><th>标题</th><th>标签</th><th>分</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="r in recommendations" :key="r.id">
                  <td>{{ r.scene }}</td><td>{{ r.title }}</td><td>{{ r.tags?.join(' / ') }}</td>
                  <td>{{ r.baseScore ?? r.score }}</td>
                  <td>
                    <button class="secondary small" type="button" @click="openEditRec(r)">编辑</button>
                    <button class="danger small" type="button" @click="deleteRec(r.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <!-- Feedback + Logs -->
      <section class="grid two section-gap">
        <article class="panel admin-panel">
          <h2>最近反馈</h2>
          <EmptyState v-if="!feedbacks.length" title="暂无反馈数据" description="用户提交推荐反馈后会显示在这里。" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>用户</th><th>推荐项</th><th>评分</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="f in feedbacks" :key="f.id">
                  <td>{{ f.userPhone }}</td><td>{{ f.itemTitle }}</td><td>{{ f.rating }}</td><td>{{ formatTime(f.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="panel admin-panel">
          <h2>操作日志</h2>
          <EmptyState v-if="!logs.length" title="暂无日志" description="管理员操作记录会显示在这里。" />
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>管理员</th><th>操作</th><th>详情</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="log in logs" :key="log.id">
                  <td>{{ log.adminPhone }}</td><td>{{ log.action }}</td><td>{{ log.detail }}</td><td>{{ formatTime(log.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <!-- Modals -->
      <BaseModal :open="showQuestionForm" title="题目管理" @close="showQuestionForm = false">
        <div class="field"><label>类型</label><select v-model="editingQuestion!.type"><option>personality</option><option>food</option><option>travel</option><option>social</option></select></div>
        <div class="field"><label>题目内容</label><input v-model="editingQuestion!.content" /></div>
        <div class="toolbar modal-action-bar">
          <button class="ghost" type="button" @click="showQuestionForm = false">取消</button>
          <button class="primary" type="button" @click="saveQuestion">保存题目</button>
        </div>
      </BaseModal>

      <BaseModal :open="showRecForm" title="推荐项管理" @close="showRecForm = false">
        <div class="field"><label>场景</label><select v-model="editingRec!.scene"><option>food</option><option>travel</option><option>social</option><option>outfit</option><option>career</option></select></div>
        <div class="field"><label>标题</label><input v-model="editingRec!.title" /></div>
        <div class="field"><label>描述</label><input v-model="editingRec!.description" /></div>
        <div class="field"><label>标签（逗号分隔）</label><input :value="editingRec!.tags?.join(',') || ''" @input="(e: any) => editingRec!.tags = e.target.value.split(',').map((s: string) => s.trim())" /></div>
        <div class="field"><label>基础分</label><input v-model.number="editingRec!.baseScore" type="number" min="0" max="100" /></div>
        <div class="toolbar modal-action-bar">
          <button class="ghost" type="button" @click="showRecForm = false">取消</button>
          <button class="primary" type="button" @click="saveRec">保存</button>
        </div>
      </BaseModal>

      <BaseModal :open="showRuleForm" title="新建规则" @close="showRuleForm = false">
        <div class="field"><label>标签</label><input v-model="newRule.tag" placeholder="explore/structured/social/gentle" /></div>
        <div class="field"><label>名称</label><input v-model="newRule.label" placeholder="中文名称" /></div>
        <div class="field"><label>权重</label><input v-model.number="newRule.weight" type="number" min="1" max="10" /></div>
        <div class="toolbar modal-action-bar">
          <button class="ghost" type="button" @click="showRuleForm = false">取消</button>
          <button class="primary" type="button" @click="createRule">保存</button>
        </div>
      </BaseModal>
    </template>
  </PageContainer>
</template>

<style scoped>
.modal-action-bar {
  margin-top: 16px;
  justify-content: flex-end;
}

/* Uniform card height with vertical scroll */
.admin-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: 800px;
}
.admin-panel h2,
.admin-panel .split {
  flex-shrink: 0;
}
.admin-panel .table-wrap {
  overflow: auto;
  flex: 1;
}
</style>
