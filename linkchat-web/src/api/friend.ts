import request from './request'
import type { Friend } from '@/types/friend'

export const friendApi = {
  addFriend(keyword: string) {
    return request.post('/friend/add', { keyword })
  },
  /** 获取待处理的好友申请列表 */
  getPendingRequests() {
    return request.get<any, { data: Friend[] }>('/friend/requests')
  },
  acceptFriend(friendId: number) {
    return request.put(`/friend/accept/${friendId}`)
  },
  rejectFriend(friendId: number) {
    return request.put(`/friend/reject/${friendId}`)
  },
  deleteFriend(friendId: number) {
    return request.delete(`/friend/${friendId}`)
  },
  getFriendList() {
    return request.get<any, { data: Friend[] }>('/friend/list')
  },
  searchFriends(keyword: string) {
    return request.get<any, { data: Friend[] }>('/friend/search', { params: { keyword } })
  }
}