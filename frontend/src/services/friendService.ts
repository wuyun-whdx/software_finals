import { friendApi, openMatchApi } from '../api'
import type { FriendInvite, FriendRequestItem, OpenMatchRecommendation, OpenMatchStatus, UserProfile } from '../types'

export async function generateFriendInvite(): Promise<FriendInvite> {
  return friendApi.createInvite()
}

export async function listFriendInvites(): Promise<FriendInvite[]> {
  return friendApi.listInvites()
}

export async function addFriendByInvite(inviteCode: string): Promise<FriendInvite> {
  return friendApi.addByInvite(inviteCode)
}

export async function sendFriendRequest(phone: string, message?: string): Promise<FriendRequestItem> {
  return friendApi.sendRequest(phone, message)
}

export async function getFriends(): Promise<UserProfile[]> {
  return friendApi.listFriends()
}

export async function getFriendRequests(): Promise<FriendRequestItem[]> {
  return friendApi.listRequests()
}

export async function acceptFriendRequest(id: number): Promise<FriendRequestItem> {
  return friendApi.acceptRequest(id)
}

export async function rejectFriendRequest(id: number): Promise<FriendRequestItem> {
  return friendApi.rejectRequest(id)
}

export async function blockFriendRequest(id: number): Promise<FriendRequestItem> {
  return friendApi.blockRequest(id)
}

export async function cancelFriendRequest(id: number): Promise<void> {
  return friendApi.cancelRequest(id)
}

export async function deleteFriend(friendId: number): Promise<void> {
  return friendApi.deleteFriend(friendId)
}

export async function blockFriend(friendId: number): Promise<void> {
  return friendApi.blockFriend(friendId)
}

export async function unblockFriend(friendId: number): Promise<void> {
  return friendApi.unblockFriend(friendId)
}

export async function getBlockedList(): Promise<UserProfile[]> {
  return friendApi.listBlocked()
}

export async function searchByPhone(phone: string): Promise<UserProfile> {
  return friendApi.searchByPhone(phone)
}

// Open Match
export async function toggleOpenMatch(): Promise<OpenMatchStatus> {
  return openMatchApi.toggle()
}

export async function getOpenMatchStatus(): Promise<OpenMatchStatus> {
  return openMatchApi.status()
}

export async function getOpenMatchRecommendations(): Promise<OpenMatchRecommendation[]> {
  return openMatchApi.recommendations()
}
