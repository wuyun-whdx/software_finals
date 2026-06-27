<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import type { TestResult, ReportSnapshot, MatchReport, ShareLinkSummary, MatchInvite, UserFeedback, MyCommentResponse, PostResponse } from '../types'
import { revokeShare } from '../services/reportService'
import { useAuthStore } from '../stores/auth'
import { testApi, recommendationApi, matchApi, reportApi, postApi } from '../api'
import { formatTime } from '../utils/format'
import { getErrorMessage } from '../utils/errors'

const auth = useAuthStore()
const router = useRouter()
const testHistory = ref<TestResult[]>([])
const reportSnapshots = ref<ReportSnapshot[]>([])
const matches = ref<MatchReport[]>([])
const shares = ref<ShareLinkSummary[]>([])
const feedbacks = ref<UserFeedback[]>([])
const invites = ref<MatchInvite[]>([])
const myPosts = ref<PostResponse[]>([])
const myComments = ref<MyCommentResponse[]>([])
const notice = ref('')
const error = ref('')
const loading = ref(true)
const editingPostId = ref<number | null>(null)
const editingCommentId = ref<number | null>(null)
const editText = ref('')

function typeLabel(type: string) {
  const labels: Record<string, string> = {
    PERSONALITY: '性格测试', FOOD: '饮食测试', TRAVEL: '旅游测试', SOCIAL: '社交测试'
  }
  return labels[type] || type
}

async function load() {
  loading.value = true
  error.value = ''
  // Load each section independently so one failure doesn't block others
  try { testHistory.value = await testApi.history() } catch { /* skip */ }
  try { reportSnapshots.value = await reportApi.history() } catch { /* skip */ }
  try { matches.value = await matchApi.list() } catch { /* skip */ }
  try { shares.value = await reportApi.shares() } catch { /* skip */ }
  try { feedbacks.value = await recommendationApi.myFeedback() } catch { /* skip */ }
  try { invites.value = await matchApi.listInvites() } catch (e) { console.warn('invites 加载失败:', e) }
  try { myPosts.value = (await postApi.mine(0)).items } catch { /* skip */ }
  try { myComments.value = await postApi.myComments() } catch { /* skip */ }
  loading.value = false
}

async function doRevokeShare(id: number) {
  try {
    await revokeShare(id)
    notice.value = '分享链接已撤销。'
    await load()
  } catch (err) {
    error.value = (err as Error).message || '撤销失败'
  }
}

async function doDeletePost(id: number) {
  if (!confirm('确认删除该帖子？')) return
  try { await postApi.delete(id); myPosts.value = myPosts.value.filter(p => p.id !== id); notice.value = '帖子已删除' }
  catch (err) { error.value = (err as Error).message || '删除失败' }
}

async function doDeleteComment(id: number) {
  if (!confirm('确认删除该评论？')) return
  try { await postApi.deleteOwnComment(id); myComments.value = myComments.value.filter(c => c.id !== id); notice.value = '评论已删除' }
  catch (err) { error.value = (err as Error).message || '删除失败' }
}

function startEditPost(post: PostResponse) { editingPostId.value = post.id; editingCommentId.value = null; editText.value = post.content }
function startEditComment(c: MyCommentResponse) { editingCommentId.value = c.id; editingPostId.value = null; editText.value = c.content }
function cancelEdit() { editingPostId.value = null; editingCommentId.value = null; editText.value = '' }

async function saveEditPost(id: number) {
  try { await postApi.update(id, { content: editText.value, domainTag: '' }); myPosts.value = myPosts.value.map(p => p.id === id ? { ...p, content: editText.value } : p); cancelEdit(); notice.value = '帖子已更新' }
  catch (err) { error.value = (err as Error).message || '更新失败' }
}

async function saveEditComment(id: number) {
  try { await postApi.updateComment(id, editText.value); myComments.value = myComments.value.map(c => c.id === id ? { ...c, content: editText.value } : c); cancelEdit(); notice.value = '评论已更新' }
  catch (err) { error.value = (err as Error).message || '更新失败' }
}

function logout() {
  auth.logout()
  router.push('/')
}

onMounted(load)
</script>

<template>
  <PageContainer
    eyebrow="个人中心"
    title="管理你的画像、反馈、匹配授权和隐私数据"
    description="你的测试答案仅用于生成个人报告、推荐结果和授权后的双人适配分析。所有数据存储在服务器端。"
  >
    <div v-if="notice" class="notice">{{ notice }}</div>
    <div v-if="error" class="error">{{ error }}</div>
    <LoadingState v-if="loading" inline message="正在加载数据..." />

    <section class="grid two">
      <article class="panel profile-card">
        <div class="avatar">{{ auth.user?.displayName?.slice(0, 1) || '我' }}</div>
        <div>
          <h2>{{ auth.user?.displayName || '本地用户' }}</h2>
          <p class="muted">{{ auth.user?.phone || '未绑定手机号' }}</p>
        </div>
        <div class="toolbar">
          <RouterLink class="button" to="/tests/personality">重新测试</RouterLink>
          <button class="ghost" type="button" @click="logout">退出登录</button>
        </div>
      </article>

      <article class="panel">
        <h2>隐私说明</h2>
        <p>测试数据仅本人可见。未经授权，其他用户无法查看你的完整测试结果；双人适配需要邀请码授权，且只展示维度差异和相处建议。</p>
      </article>
    </section>

    <section class="grid two section-gap profile-grid">
      <!-- 历史测试记录 -->
      <article class="panel">
        <h2>历史测试记录</h2>
        <EmptyState
          v-if="!testHistory.length"
          title="暂无历史测试"
          description="完成测试后，这里会记录测试时间、类型和维度分数。"
          action-label="去测试"
          action-to="/tests/personality"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>测试时间</th><th>测试类型</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="item in testHistory" :key="item.id">
                <td>{{ formatTime(item.createdAt) }}</td>
                <td>{{ typeLabel(item.type) }}</td>
                <td><RouterLink to="/report">查看报告</RouterLink></td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <!-- 推荐反馈记录 -->
      <article class="panel">
        <h2>我的推荐反馈记录</h2>
        <EmptyState
          v-if="!feedbacks.length"
          title="暂无推荐反馈"
          description="在推荐页点击喜欢、一般或不喜欢后，这里会保留记录。"
          action-label="查看推荐"
          action-to="/recommendations"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>推荐名称</th><th>场景</th><th>反馈</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="item in feedbacks" :key="item.id">
                <td>{{ item.itemTitle }}</td>
                <td>{{ item.scene }}</td>
                <td>{{ item.rating }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section class="grid two section-gap profile-grid">
      <!-- 隐私授权管理 -->
      <article class="panel">
        <h2>隐私授权管理</h2>
        <p class="muted">匹配邀请码</p>
        <EmptyState
          v-if="!invites.length"
          title="暂无匹配邀请码"
          description="在匹配页生成邀请码后，这里会显示记录。"
          action-label="去匹配"
          action-to="/match"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>邀请码</th><th>状态</th><th>生成时间</th></tr></thead>
            <tbody>
              <tr v-for="invite in invites" :key="invite.code">
                <td>{{ invite.code }}</td>
                <td>{{ invite.status === 'ACTIVE' ? '有效' : invite.status === 'USED' ? '已使用' : '已撤销' }}</td>
                <td>{{ formatTime(invite.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="muted section-gap">分享链接</p>
        <EmptyState
          v-if="!shares.length"
          title="暂无分享链接"
          description="在报告页生成分享链接后，这里会显示记录。"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>Token</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="share in shares" :key="share.id">
                <td>{{ share.token.slice(0, 8) }}...</td>
                <td>{{ share.active ? '有效' : '已撤销' }}</td>
                <td>{{ formatTime(share.createdAt) }}</td>
                <td><button v-if="share.active" class="danger" type="button" @click="doRevokeShare(share.id)">撤销</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <!-- 双人适配记录 -->
      <article class="panel">
        <h2>我的双人适配记录</h2>
        <EmptyState
          v-if="!matches.length"
          title="暂无双人适配记录"
          description="使用邀请码完成匹配后，这里会记录契合度和建议。"
          action-label="去匹配"
          action-to="/match"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>对象</th><th>契合度</th><th>摘要</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="item in matches" :key="item.id">
                <td>{{ item.target.displayName }}</td>
                <td>{{ item.score }}%</td>
                <td>{{ item.summary }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section class="grid two section-gap profile-grid">
      <!-- 我的帖子 -->
      <article class="panel">
        <h2>我的帖子</h2>
        <EmptyState
          v-if="!myPosts.length"
          title="暂无帖子"
          description="在社区发布的帖子会显示在这里。"
          action-label="去社区"
          action-to="/community"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>内容</th><th>领域</th><th>赞/评/收藏</th><th>时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="p in myPosts" :key="p.id">
                <td v-if="editingPostId === p.id" colspan="3">
                  <textarea v-model="editText" class="field-input" rows="2" style="width:100%"></textarea>
                </td>
                <td v-else>
                  <RouterLink :to="`/community/post/${p.id}`">{{ p.content.slice(0, 40) }}{{ p.content.length > 40 ? '...' : '' }}</RouterLink>
                </td>
                <td v-if="editingPostId !== p.id">{{ p.domainTag }}</td>
                <td v-if="editingPostId !== p.id">{{ p.likeCount }} / {{ p.commentCount }} / {{ p.favoriteCount }}</td>
                <td>{{ formatTime(p.createdAt) }}</td>
                <td>
                  <template v-if="editingPostId === p.id">
                    <button class="primary small" type="button" @click="saveEditPost(p.id)">保存</button>
                    <button class="ghost small" type="button" @click="cancelEdit()">取消</button>
                  </template>
                  <template v-else>
                    <button class="secondary small" type="button" @click="startEditPost(p)">编辑</button>
                    <button class="danger small" type="button" @click="doDeletePost(p.id)">删除</button>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <!-- 我的评论 -->
      <article class="panel">
        <h2>我的评论</h2>
        <EmptyState
          v-if="!myComments.length"
          title="暂无评论"
          description="在社区发表的评论会显示在这里。"
          action-label="去社区"
          action-to="/community"
        />
        <div v-else class="table-wrap">
          <table>
            <thead><tr><th>评论内容</th><th>所属帖子</th><th>时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="c in myComments" :key="c.id">
                <td v-if="editingCommentId === c.id" colspan="2">
                  <textarea v-model="editText" class="field-input" rows="2" style="width:100%"></textarea>
                </td>
                <td v-else>{{ c.content.slice(0, 40) }}{{ c.content.length > 40 ? '...' : '' }}</td>
                <td v-if="editingCommentId !== c.id">
                  <RouterLink :to="`/community/post/${c.postId}`">{{ c.postTitle }}</RouterLink>
                </td>
                <td>{{ formatTime(c.createdAt) }}</td>
                <td>
                  <template v-if="editingCommentId === c.id">
                    <button class="primary small" type="button" @click="saveEditComment(c.id)">保存</button>
                    <button class="ghost small" type="button" @click="cancelEdit()">取消</button>
                  </template>
                  <template v-else>
                    <button class="secondary small" type="button" @click="startEditComment(c)">编辑</button>
                    <button class="danger small" type="button" @click="doDeleteComment(c.id)">删除</button>
                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>
  </PageContainer>
</template>
