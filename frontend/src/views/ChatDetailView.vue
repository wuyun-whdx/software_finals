<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadingState from '../components/common/LoadingState.vue'
import type { ChatMessage, UserProfile } from '../types'
import { getFriends } from '../services/friendService'
import { getMessages, sendMessage, markRead } from '../services/chatService'
import { useAuthStore } from '../stores/auth'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const friendId = computed(() => Number(route.params.friendId))

const messages = ref<ChatMessage[]>([])
const friend = ref<UserProfile | null>(null)
const inputText = ref('')
const loading = ref(true)
const error = ref('')
const messagesEl = ref<HTMLDivElement | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null
let currentPage = 0
let allLoaded = false

async function loadFriend() {
  try {
    const friends = await getFriends()
    friend.value = friends.find(f => f.id === friendId.value) || null
  } catch { /* skip */ }
}

async function loadMessages(reset = false) {
  if (reset) {
    currentPage = 0
    allLoaded = false
  }
  try {
    const msgs = await getMessages(friendId.value, currentPage)
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
  } catch (err) {
    error.value = '加载消息失败'
  } finally {
    loading.value = false
  }
}

async function doSend() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  try {
    const msg = await sendMessage(friendId.value, text)
    messages.value = [...messages.value, msg]
    await scrollToBottom()
  } catch (err) {
    error.value = '发送失败'
  }
}

async function doPoll() {
  try {
    const msgs = await getMessages(friendId.value, 0)
    const existingIds = new Set(messages.value.map(m => m.id))
    const newMsgs = msgs.filter(m => !existingIds.has(m.id))
    if (newMsgs.length > 0) {
      messages.value = [...messages.value, ...newMsgs]
      await scrollToBottom()
      await markRead(friendId.value)
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

watch(() => friendId.value, () => {
  loadFriend()
  loadMessages(true)
})

onMounted(async () => {
  await loadFriend()
  await loadMessages(true)
  await markRead(friendId.value)
  await scrollToBottom()
  pollTimer = setInterval(doPoll, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="chat-page">
    <!-- Header -->
    <header class="chat-header">
      <button class="back-btn" @click="router.push('/match/chat')">← 返回</button>
      <div v-if="friend" class="chat-partner">
        <div class="avatar-small">{{ friend.displayName.charAt(0) }}</div>
        <span class="partner-name">{{ friend.displayName }}</span>
      </div>
      <span v-else class="partner-name">加载中...</span>
    </header>

    <div v-if="error" class="error">{{ error }}</div>

    <!-- Messages -->
    <LoadingState v-if="loading" message="加载消息中..." />

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

    <!-- Input -->
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
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-height: 100vh;
  background: var(--bg);
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.back-btn {
  background: none;
  border: none;
  color: var(--accent);
  font-size: 0.95rem;
  cursor: pointer;
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
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
}
.partner-name { font-weight: 600; }

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
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 14px;
  word-break: break-word;
}
.mine .bubble {
  background: var(--accent);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.theirs .bubble {
  background: var(--surface);
  color: var(--text);
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

.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: var(--surface);
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--bg);
  color: var(--text);
  font-size: 0.95rem;
  outline: none;
}
.chat-input:focus { border-color: var(--accent); }
</style>
