import { defineStore } from 'pinia'
import { ref } from 'vue'
import { friendApi } from '@/api/friend'
import type { Friend } from '@/types/friend'

export const useFriendStore = defineStore('friend', () => {
  const friends = ref<Friend[]>([])
  const pendingRequests = ref<Friend[]>([])
  const unreadRequestCount = ref(0)

  async function loadFriends() {
    const res = await friendApi.getFriendList()
    friends.value = res.data
  }

  /** 加载待处理的好友申请列表 */
  async function loadPendingRequests() {
    const res = await friendApi.getPendingRequests()
    pendingRequests.value = res.data
    unreadRequestCount.value = res.data.length
  }

  /** WebSocket 通知：收到新的好友申请 */
  function onNewFriendRequest() {
    unreadRequestCount.value++
    // 重新加载申请列表
    loadPendingRequests()
  }

  async function addFriend(keyword: string) {
    await friendApi.addFriend(keyword)
  }

  async function acceptFriend(friendId: number) {
    await friendApi.acceptFriend(friendId)
    // 从待处理列表中移除
    pendingRequests.value = pendingRequests.value.filter(r => r.friendId !== friendId)
    unreadRequestCount.value = pendingRequests.value.length
    // 重新加载好友列表
    await loadFriends()
  }

  async function rejectFriend(friendId: number) {
    await friendApi.rejectFriend(friendId)
    // 从待处理列表中移除
    pendingRequests.value = pendingRequests.value.filter(r => r.friendId !== friendId)
    unreadRequestCount.value = pendingRequests.value.length
  }

  async function deleteFriend(friendId: number) {
    await friendApi.deleteFriend(friendId)
    friends.value = friends.value.filter(f => f.friendId !== friendId)
  }

  function updateFriendStatus(friendId: number, status: number) {
    const f = friends.value.find(f => f.friendId === friendId)
    if (f) f.status = status
  }

  return {
    friends,
    pendingRequests,
    unreadRequestCount,
    loadFriends,
    loadPendingRequests,
    onNewFriendRequest,
    addFriend,
    acceptFriend,
    rejectFriend,
    deleteFriend,
    updateFriendStatus
  }
})