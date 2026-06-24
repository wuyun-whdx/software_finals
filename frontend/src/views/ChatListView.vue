<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import PageContainer from '../components/common/PageContainer.vue'
import type { ChatConversation } from '../types'
import { getConversations, getUnreadCount } from '../services/chatService'
import { formatTime } from '../utils/format'

const router = useRouter()
const conversations = ref<ChatConversation[]>([])
const loading = ref(true)
const error = ref('')
let pollTimer: ReturnType<typeof setInterval> | null = null

async function load() {
  try {
    conversations.value = await getConversations()
  } catch (err) {
    error.value = '加载聊天列表失败'
  } finally {
    loading.value = false
  }
}

function goChat(friendId: number) {
  router.push(`/match/chat/${friendId}`)
}

function goFriends() {
  router.push('/match/friends')
}

onMounted(() => {
  load()
  pollTimer = setInterval(load, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <PageContainer
    eyebrow="聊天"
    title="我的消息"
    description="与好友进行一对一聊天"
  >
    <div v-if="error" class="error">{{ error }}</div>
    <LoadingState v-if="loading" message="正在加载..." />

    <div v-if="!loading && conversations.length === 0" class="section-gap">
      <EmptyState
        icon="message-circle"
        title="暂无聊天"
        description="还没有好友？去添加好友开始聊天吧～"
      >
        <button class="primary" @click="goFriends">去添加好友</button>
      </EmptyState>
    </div>

    <div v-else class="conversation-list">
      <div
        v-for="conv in conversations"
        :key="conv.friend.id"
        class="conv-item"
        @click="goChat(conv.friend.id)"
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
  </PageContainer>
</template>

<style scoped>
.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.conv-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.conv-item:hover {
  background: var(--hover);
}
.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1.1rem;
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
  margin-bottom: 4px;
}
.conv-name {
  font-weight: 600;
}
.conv-time {
  font-size: 0.8rem;
  color: var(--muted);
}
.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.conv-last {
  font-size: 0.85rem;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
.unread-badge {
  background: var(--accent);
  color: #fff;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 0.75rem;
  flex-shrink: 0;
}
</style>
