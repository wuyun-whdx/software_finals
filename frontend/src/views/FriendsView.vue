<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import type { FriendInvite, FriendRequestItem, OpenMatchRecommendation, OpenMatchStatus, UserProfile } from '../types'
import {
  generateFriendInvite, addFriendByInvite, sendFriendRequest, getFriends, getFriendRequests,
  acceptFriendRequest, rejectFriendRequest, blockFriendRequest, cancelFriendRequest,
  deleteFriend, toggleOpenMatch, getOpenMatchStatus, getOpenMatchRecommendations,
  searchByPhone
} from '../services/friendService'
import { getErrorMessage } from '../utils/errors'

import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const activeTab = ref<'friends' | 'recommendations' | 'requests'>('friends')
const friends = ref<UserProfile[]>([])
const requests = ref<FriendRequestItem[]>([])
const recommendations = ref<OpenMatchRecommendation[]>([])
const matchStatus = ref<OpenMatchStatus | null>(null)
const myInvite = ref<FriendInvite | null>(null)

const inviteCode = ref('')
const searchPhone = ref('')
const requestMessage = ref('')
const notice = ref('')
const error = ref('')
const loading = ref(true)

const incomingRequests = computed(() => requests.value.filter(r => r.status === 'PENDING' && r.toUser.id === auth.user?.id))
const outgoingRequests = computed(() => requests.value.filter(r => r.status === 'PENDING' && r.fromUser.id === auth.user?.id))

async function load() {
  loading.value = true
  error.value = ''
  try { friends.value = await getFriends() } catch { /* skip */ }
  try { requests.value = await getFriendRequests() } catch { /* skip */ }
  try {
    matchStatus.value = await getOpenMatchStatus()
    if (matchStatus.value?.enabled) {
      recommendations.value = await getOpenMatchRecommendations()
    }
  } catch { /* skip */ }
  loading.value = false
}

async function doCreateInvite() {
  error.value = ''
  try {
    myInvite.value = await generateFriendInvite()
    notice.value = `好友邀请码已生成：${myInvite.value.code}`
  } catch (err) {
    error.value = getErrorMessage(err)
  }
}

async function doCopyInvite() {
  if (!myInvite.value) return
  await navigator.clipboard.writeText(myInvite.value.code)
  notice.value = '邀请码已复制到剪贴板'
}

async function doAddByInvite() {
  if (!inviteCode.value.trim()) return
  error.value = ''
  try {
    await addFriendByInvite(inviteCode.value.trim())
    notice.value = '好友添加成功！'
    inviteCode.value = ''
    await load()
  } catch (err) {
    error.value = getErrorMessage(err)
  }
}

async function doSearchAndRequest() {
  if (!searchPhone.value.trim()) return
  error.value = ''
  try {
    const user = await searchByPhone(searchPhone.value.trim())
    await sendFriendRequest(user.phone, requestMessage.value || undefined)
    notice.value = `已向 ${user.displayName} 发送好友申请`
    searchPhone.value = ''
    requestMessage.value = ''
    await load()
  } catch (err) {
    error.value = getErrorMessage(err)
  }
}

async function doAccept(id: number) {
  try { await acceptFriendRequest(id); await load() } catch (err) { error.value = getErrorMessage(err) }
}
async function doReject(id: number) {
  try { await rejectFriendRequest(id); await load() } catch (err) { error.value = getErrorMessage(err) }
}
async function doBlockRequest(id: number) {
  try { await blockFriendRequest(id); await load() } catch (err) { error.value = getErrorMessage(err) }
}
async function doCancelRequest(id: number) {
  try { await cancelFriendRequest(id); await load() } catch (err) { error.value = getErrorMessage(err) }
}
async function doDeleteFriend(friendId: number, name: string) {
  if (!confirm(`确定删除好友 ${name}？聊天记录仍会保留。`)) return
  try { await deleteFriend(friendId); await load() } catch (err) { error.value = getErrorMessage(err) }
}
async function doToggleMatch() {
  error.value = ''
  try {
    matchStatus.value = await toggleOpenMatch()
    notice.value = matchStatus.value!.message
    if (matchStatus.value!.enabled) {
      recommendations.value = await getOpenMatchRecommendations()
    } else {
      recommendations.value = []
    }
  } catch (err) {
    error.value = getErrorMessage(err)
  }
}

async function doRequestFromRec(user: UserProfile) {
  error.value = ''
  try {
    await sendFriendRequest(user.phone, '你好！系统推荐我们人格契合度很高，交个朋友吧～')
    notice.value = `已向 ${user.displayName} 发送好友申请`
    await load()
  } catch (err) {
    error.value = getErrorMessage(err)
  }
}

function goChat(friendId: number) {
  router.push(`/match/chat/${friendId}`)
}

onMounted(load)
</script>

<template>
  <PageContainer
    eyebrow="好友与匹配"
    title="管理好友关系与发现新朋友"
    description="通过邀请码添加好友、发送好友申请，或开启开放匹配发现人格契合度高的朋友。"
  >
    <div v-if="notice" class="notice">{{ notice }}</div>
    <div v-if="error" class="error">{{ error }}</div>

    <!-- Tab Bar -->
    <nav class="tab-bar">
      <button :class="{ active: activeTab === 'friends' }" @click="activeTab = 'friends'">好友 ({{ friends.length }})</button>
      <button :class="{ active: activeTab === 'recommendations' }" @click="activeTab = 'recommendations'">推荐匹配</button>
      <button :class="{ active: activeTab === 'requests' }" @click="activeTab = 'requests'">
        申请列表
        <span v-if="incomingRequests.length" class="badge">{{ incomingRequests.length }}</span>
      </button>
    </nav>

    <LoadingState v-if="loading" message="正在加载..." />

    <!-- Tab: Friends -->
    <section v-if="activeTab === 'friends' && !loading" class="tab-content">
      <div class="action-row">
        <div class="invite-section">
          <button class="primary" @click="doCreateInvite">生成好友邀请码</button>
          <div v-if="myInvite" class="invite-box">
            <span class="invite-code">{{ myInvite.code }}</span>
            <button class="ghost small" @click="doCopyInvite">复制</button>
          </div>
        </div>
        <div class="invite-input-section">
          <input v-model="inviteCode" placeholder="输入好友邀请码" class="field-input" />
          <button class="primary" @click="doAddByInvite">添加好友</button>
        </div>
      </div>
      <div class="action-row">
        <input v-model="searchPhone" placeholder="输入手机号搜索" class="field-input" maxlength="11" />
        <input v-model="requestMessage" placeholder="验证消息（选填）" class="field-input" maxlength="50" />
        <button class="primary" @click="doSearchAndRequest">搜索并发送申请</button>
      </div>

      <div v-if="friends.length === 0" class="section-gap">
        <EmptyState icon="users" title="暂无好友" description="生成邀请码分享给朋友，或通过手机号搜索添加好友" />
      </div>
      <div v-else class="friend-list section-gap">
        <div v-for="f in friends" :key="f.id" class="friend-item">
          <div class="friend-info">
            <div class="avatar">{{ f.displayName.charAt(0) }}</div>
            <div>
              <p class="friend-name">{{ f.displayName }}</p>
              <p class="muted small">{{ f.phone }}</p>
            </div>
          </div>
          <div class="friend-actions">
            <button class="primary small" @click="goChat(f.id)">聊天</button>
            <button class="ghost small danger" @click="doDeleteFriend(f.id, f.displayName)">删除</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Tab: Recommendations -->
    <section v-if="activeTab === 'recommendations' && !loading" class="tab-content">
      <div class="match-toggle-bar">
        <div>
          <p class="status-label">
            开放匹配状态：
            <span :class="matchStatus?.enabled ? 'status-on' : 'status-off'">
              {{ matchStatus?.enabled ? '已开启 ✅' : '未开启' }}
            </span>
          </p>
          <p class="muted small">{{ matchStatus?.message }}</p>
        </div>
        <button :class="matchStatus?.enabled ? 'ghost' : 'primary'" @click="doToggleMatch">
          {{ matchStatus?.enabled ? '关闭匹配' : '开启匹配' }}
        </button>
      </div>

      <div v-if="!matchStatus?.enabled" class="section-gap">
        <EmptyState icon="lock" title="匹配未开启" description="开启后，你的10维人格分数将进入匹配池，发现与你契合度≥80%的朋友。" />
      </div>

      <div v-else-if="recommendations.length === 0" class="section-gap">
        <EmptyState icon="search" title="暂无推荐" description="当前匹配池中没有契合度≥80%的用户，请稍后再来查看。" />
      </div>

      <div v-else class="rec-list section-gap">
        <div v-for="rec in recommendations" :key="rec.user.id" class="rec-item">
          <div class="rec-info">
            <div class="avatar">{{ rec.user.displayName.charAt(0) }}</div>
            <div>
              <p class="friend-name">{{ rec.user.displayName }}</p>
              <p class="muted small">高契合: {{ rec.topDimensions.join(' · ') }}</p>
            </div>
          </div>
          <div class="rec-score">
            <span class="score">{{ rec.score }}%</span>
            <button class="primary small" @click="doRequestFromRec(rec.user)">加好友</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Tab: Requests -->
    <section v-if="activeTab === 'requests' && !loading" class="tab-content">
      <h3>收到的申请 ({{ incomingRequests.length }})</h3>
      <div v-if="incomingRequests.length === 0">
        <EmptyState icon="inbox" title="暂无收到的申请" description="当有人向你发送好友申请时，会显示在这里" />
      </div>
      <div v-else class="request-list">
        <div v-for="r in incomingRequests" :key="r.id" class="request-item">
          <div class="friend-info">
            <div class="avatar">{{ r.fromUser.displayName.charAt(0) }}</div>
            <div>
              <p class="friend-name">{{ r.fromUser.displayName }}</p>
              <p class="muted small">{{ r.message || '(无验证消息)' }}</p>
            </div>
          </div>
          <div class="request-actions">
            <button class="primary small" @click="doAccept(r.id)">同意</button>
            <button class="ghost small" @click="doReject(r.id)">拒绝</button>
            <button class="ghost small danger" @click="doBlockRequest(r.id)">拉黑</button>
          </div>
        </div>
      </div>

      <h3 class="section-gap">发出的申请 ({{ outgoingRequests.length }})</h3>
      <div v-if="outgoingRequests.length === 0">
        <EmptyState icon="send" title="暂无发出的申请" description="你可以通过手机号搜索用户并发送好友申请" />
      </div>
      <div v-else class="request-list">
        <div v-for="r in outgoingRequests" :key="r.id" class="request-item">
          <div class="friend-info">
            <div class="avatar">{{ r.toUser.displayName.charAt(0) }}</div>
            <div>
              <p class="friend-name">{{ r.toUser.displayName }}</p>
              <p class="muted small">{{ r.message || '(无验证消息)' }} · 待处理</p>
            </div>
          </div>
          <button class="ghost small danger" @click="doCancelRequest(r.id)">取消申请</button>
        </div>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.friend-list, .rec-list, .request-list, .tab-bar, .action-row {
  /* 映射到全局设计系统变量 */
  --accent: var(--blip);
  --text: var(--ink);
  --border: var(--line);
  --bg: #f0eef6;
}
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--surface);
  margin-bottom: 20px;
  overflow-x: auto;
}
.tab-bar button {
  padding: 10px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  color: var(--muted);
  font-size: 0.95rem;
  cursor: pointer;
  position: relative;
}
.tab-bar button.active {
  color: var(--text);
  border-bottom-color: var(--accent);
}
.badge {
  background: var(--accent);
  color: #fff;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 0.75rem;
  margin-left: 4px;
}
.action-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.invite-section, .invite-input-section {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.invite-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface);
  padding: 6px 12px;
  border-radius: 8px;
}
.invite-code {
  font-family: monospace;
  font-size: 1.1rem;
  letter-spacing: 1px;
}
.field-input {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text);
  font-size: 0.9rem;
  min-width: 180px;
}
.friend-list, .rec-list, .request-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.friend-item, .rec-item, .request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface);
  border-radius: 10px;
}
.friend-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
}
.friend-name {
  font-weight: 600;
}
.friend-actions, .request-actions, .rec-score {
  display: flex;
  gap: 6px;
  align-items: center;
}
.match-toggle-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--surface);
  border-radius: 10px;
  margin-bottom: 16px;
}
.status-on { color: var(--blip); font-weight: 600; }
.status-off { color: var(--muted); }
.score {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--blip);
  margin-right: 8px;
}
.danger { color: var(--trace); }
.small { font-size: 0.85rem; }
</style>
