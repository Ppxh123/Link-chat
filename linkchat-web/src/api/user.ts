import request from './request'
import type { User } from '@/types/user'

export const userApi = {
  getProfile() {
    return request.get<any, { data: User }>('/user/profile')
  },
  getUserProfile(userId: number) {
    return request.get<any, { data: User }>(`/user/profile/${userId}`)
  },
  updateProfile(data: { nickname?: string; signature?: string }) {
    return request.put('/user/profile', data)
  },
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  searchUsers(keyword: string) {
    return request.get<any, { data: User[] }>('/user/search', { params: { keyword } })
  }
}