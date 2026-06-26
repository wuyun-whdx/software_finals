<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadingState from '../components/common/LoadingState.vue'
import type { ChatConversation, ChatMessage, UserProfile } from '../types'
import { getFriends } from '../services/friendService'
import { getConversations, getMessages, sendMessage, markRead } from '../services/chatService'
import { useAuthStore } from '../stores/auth'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// ---- Sidebar state ----
const conversations = ref<ChatConversation[]>([])
const sidebarLoading = ref(true)
const sidebarError = ref('')
let sidebarPollTimer: ReturnType<typeof setInterval> | null = null

// ---- Chat panel state ----
const activeFriendId = ref<number | null>(null)
const messages = ref<ChatMessage[]>([])
const friend = ref<UserProfile | null>(null)
const inputText = ref('')
const chatLoading = ref(false)
const chatError = ref('')
const messagesEl = ref<HTMLDivElement | null>(null)
let chatPollTimer: ReturnType<typeof setInterval> | null = null
let currentPage = 0
let allLoaded = false

const hasActiveChat = computed(() => activeFriendId.value !== null)

// ---- Sidebar logic ----
async function loadConversations() {
  try {
    conversations.value = await getConversations()
  } catch {
    sidebarError.value = '加载聊天列表失败'
  } finally {
    sidebarLoading.value = false
  }
}

function selectFriend(friendId: number) {
  if (activeFriendId.value === friendId) return
  activeFriendId.value = friendId
  router.replace(`/match/chat/${friendId}`)
}

function goFriends() {
  router.push('/match/friends')
}

// ---- Chat panel logic ----
async function loadFriend() {
  if (!activeFriendId.value) return
  try {
    const friends = await getFriends()
    friend.value = friends.find(f => f.id === activeFriendId.value) || null
  } catch { /* skip */ }
}

async function loadMessages(reset = false) {
  if (!activeFriendId.value) return
  if (reset) {
    currentPage = 0
    allLoaded = false
  }
  try {
    const msgs = await getMessages(activeFriendId.value, currentPage)
    if (msgs.length === 0 && currentPage === 0) {
      messages.value = []
      return
    }
    if (reset) {
      messages.value = msgs
    } else {
      const existingIds = new Set(messages.value.map(m => m.id))
      const newMsgs = msgs.filter(m => !existingIds.has(m.id))
      messages.value = [...newMsgs, ...messages.value]
    }
    if (msgs.length < 50) allLoaded = true
  } catch {
    chatError.value = '加载消息失败'
  } finally {
    chatLoading.value = false
  }
}

async function doSend() {
  const text = inputText.value.trim()
  if (!text || !activeFriendId.value) return
  inputText.value = ''
  try {
    const msg = await sendMessage(activeFriendId.value, text)
    messages.value = [...messages.value, msg]
    await scrollToBottom()
  } catch {
    chatError.value = '发送失败'
  }
}

async function doPoll() {
  if (!activeFriendId.value) return
  try {
    const msgs = await getMessages(activeFriendId.value, 0)
    const existingIds = new Set(messages.value.map(m => m.id))
    const newMsgs = msgs.filter(m => !existingIds.has(m.id))
    if (newMsgs.length > 0) {
      messages.value = [...messages.value, ...newMsgs]
      await scrollToBottom()
      await markRead(activeFriendId.value)
    }
  } catch { /* skip */ }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

function onScroll() {
  if (!messagesEl.value) return
  if (messagesEl.value.scrollTop < 50 && !allLoaded) {
    const prevHeight = messagesEl.value.scrollHeight
    currentPage++
    loadMessages(false).then(() => {
      nextTick(() => {
        if (messagesEl.value) {
          messagesEl.value.scrollTop = messagesEl.value.scrollHeight - prevHeight
        }
      })
    })
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    doSend()
  }
}

// Watch route param to switch active chat (only after mount)
watch(() => route.params.friendId, (newId) => {
  const id = newId ? Number(newId) : null
  if (id && id !== activeFriendId.value) {
    activeFriendId.value = id
    chatLoading.value = true
    chatError.value = ''
    messages.value = []
    friend.value = null
    loadFriend()
    loadMessages(true)
  }
})

// ---- Lifecycle ----
onMounted(async () => {
  await loadConversations()

  const fid = Number(route.params.friendId)
  if (fid) {
    activeFriendId.value = fid
    await loadFriend()
    await loadMessages(true)
    await markRead(fid)
    await scrollToBottom()
  }

  sidebarPollTimer = setInterval(loadConversations, 5000)
  chatPollTimer = setInterval(doPoll, 3000)
})

onUnmounted(() => {
  if (sidebarPollTimer) clearInterval(sidebarPollTimer)
  if (chatPollTimer) clearInterval(chatPollTimer)
})
</script>

<template>
  <div class="chat-layout">
    <!-- Left Sidebar: Conversation List -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h3>消息</h3>
      </div>

      <div v-if="sidebarError" class="error">{{ sidebarError }}</div>
      <LoadingState v-if="sidebarLoading" message="加载中..." />

      <div v-if="!sidebarLoading && conversations.length === 0" class="sidebar-empty">
        <p class="muted">暂无聊天</p>
        <button class="primary small" @click="goFriends">添加好友</button>
      </div>

      <div v-else class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.friend.id"
          :class="['conv-item', { active: activeFriendId === conv.friend.id }]"
          @click="selectFriend(conv.friend.id)"
        >
          <div class="conv-avatar">{{ conv.friend.displayName.charAt(0) }}</div>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.friend.displayName }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-last">{{ conv.lastMessage || '开始聊天吧～' }}</span>
              <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Right Panel: Chat Detail -->
    <main class="chat-main">
      <template v-if="hasActiveChat">
        <!-- Chat Header -->
        <header class="chat-header">
          <div v-if="friend" class="chat-partner">
            <div class="avatar-small">{{ friend.displayName.charAt(0) }}</div>
            <span class="partner-name">{{ friend.displayName }}</span>
          </div>
          <span v-else class="partner-name">加载中...</span>
        </header>

        <div v-if="chatError" class="error">{{ chatError }}</div>

        <!-- Messages -->
        <LoadingState v-if="chatLoading" message="加载消息中..." />

        <div v-else ref="messagesEl" class="messages-area" @scroll="onScroll">
          <div v-if="messages.length === 0" class="empty-chat">
            <p class="muted">你们还没有聊过天，发送第一条消息吧～</p>
          </div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message-row', msg.sender.id === auth.user?.id ? 'mine' : 'theirs']"
          >
            <div class="bubble">
              <p class="msg-text">{{ msg.content }}</p>
              <span class="msg-time">
                {{ formatTime(msg.createdAt) }}
                <span v-if="msg.sender.id === auth.user?.id">{{ msg.read ? '✓✓' : '✓' }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Input (fixed at bottom) -->
        <div class="input-area">
          <input
            v-model="inputText"
            type="text"
            placeholder="输入消息..."
            class="chat-input"
            maxlength="500"
            @keydown="onKeydown"
          />
          <button class="primary" :disabled="!inputText.trim()" @click="doSend">发送</button>
        </div>
      </template>

      <!-- Placeholder when no chat selected -->
      <div v-else class="no-chat-placeholder">
        <div class="placeholder-content">
          <span class="placeholder-icon">💬</span>
          <h3>选择一个聊天</h3>
          <p class="muted">从左侧列表选择一位好友开始聊天</p>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ===== Layout ===== */
.chat-layout {
  display: flex;
  height: calc(100vh - 108px); /* 64px header + 44px subnav */
  overflow: hidden;
}

/* ===== Sidebar ===== */
.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-right: 1px solid var(--line);
}
.sidebar-header {
  padding: 16px;
  flex-shrink: 0;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 1.1rem;
}
.sidebar-empty {
  padding: 24px 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.conv-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px 8px;
}
.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.conv-item:hover {
  background: var(--soft);
}
.conv-item.active {
  background: var(--soft);
}
.conv-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--blip);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
  flex-shrink: 0;
}
.conv-info {
  flex: 1;
  min-width: 0;
}
.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}
.conv-name {
  font-weight: 600;
  font-size: 0.9rem;
}
.conv-time {
  font-size: 0.7rem;
  color: var(--muted);
  flex-shrink: 0;
}
.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.conv-last {
  font-size: 0.8rem;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}
.unread-badge {
  background: var(--blip);
  color: #fff;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 0.7rem;
  flex-shrink: 0;
}

/* ===== Chat Main ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f0eef6;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.chat-partner {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--blip);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
}
.partner-name {
  font-weight: 600;
}

/* ===== Messages ===== */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.empty-chat {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.message-row { display: flex; }
.message-row.mine { justify-content: flex-end; }
.message-row.theirs { justify-content: flex-start; }

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 14px;
  word-break: break-word;
}
.mine .bubble {
  background: var(--blip);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.theirs .bubble {
  background: #fff;
  color: var(--ink);
  border-bottom-left-radius: 4px;
}
.msg-text { margin: 0; font-size: 0.95rem; line-height: 1.5; }
.msg-time {
  font-size: 0.7rem;
  opacity: 0.7;
  display: block;
  margin-top: 4px;
  text-align: right;
}

/* ===== Input (fixed bottom) ===== */
.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: var(--surface);
  border-top: 1px solid var(--line);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: #f0eef6;
  color: var(--ink);
  font-size: 0.95rem;
  outline: none;
}
.chat-input:focus { border-color: var(--blip); }

/* ===== Placeholder ===== */
.no-chat-placeholder {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}
.placeholder-content {
  text-align: center;
}
.placeholder-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 12px;
}
.placeholder-content h3 {
  margin-bottom: 8px;
}
</style>
