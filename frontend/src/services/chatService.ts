import { chatApi } from '../api'
import type { ChatConversation, ChatMessage } from '../types'

export async function sendMessage(friendId: number, content: string): Promise<ChatMessage> {
  return chatApi.sendMessage(friendId, content)
}

export async function getMessages(friendId: number, page = 0): Promise<ChatMessage[]> {
  return chatApi.getMessages(friendId, page)
}

export async function getConversations(): Promise<ChatConversation[]> {
  return chatApi.getConversations()
}

export async function markRead(friendId: number): Promise<void> {
  return chatApi.markRead(friendId)
}

export async function getUnreadCount(): Promise<number> {
  return chatApi.getUnreadCount()
}
