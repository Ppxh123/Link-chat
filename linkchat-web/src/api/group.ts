import request from './request'
import type { Group, CreateGroupRequest } from '@/types/group'
import type { User } from '@/types/user'

export const groupApi = {
  createGroup(data: CreateGroupRequest) {
    return request.post<any, { data: Group }>('/group/create', data)
  },
  getGroupInfo(groupId: string) {
    return request.get<any, { data: Group }>(`/group/${groupId}`)
  },
  dismissGroup(groupId: string) {
    return request.delete(`/group/${groupId}`)
  },
  leaveGroup(groupId: string) {
    return request.delete(`/group/${groupId}/leave`)
  },
  inviteMembers(groupId: string, userIds: number[]) {
    return request.post(`/group/${groupId}/invite`, { userIds })
  },
  removeMember(groupId: string, memberId: string) {
    return request.delete(`/group/${groupId}/member/${memberId}`)
  },
  setAdmin(groupId: string, memberId: string) {
    return request.put(`/group/${groupId}/admin/${memberId}`)
  },
  removeAdmin(groupId: string, memberId: string) {
    return request.delete(`/group/${groupId}/admin/${memberId}`)
  },
  transferOwnership(groupId: string, newOwnerId: string) {
    return request.put(`/group/${groupId}/transfer/${newOwnerId}`)
  },
  updateGroupName(groupId: string, name: string) {
    return request.put(`/group/${groupId}/name`, { name })
  },
  updateAnnouncement(groupId: string, announcement: string) {
    return request.put(`/group/${groupId}/announcement`, { announcement })
  },
  uploadGroupAvatar(groupId: string, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/group/${groupId}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  muteAll(groupId: string, muted: boolean) {
    return request.put(`/group/${groupId}/mute-all`, { muted })
  },
  muteMember(groupId: string, memberId: string, muted: boolean) {
    return request.put(`/group/${groupId}/mute-member/${memberId}`, { muted })
  },
  getGroupMembers(groupId: string) {
    return request.get<any, { data: User[] }>(`/group/${groupId}/members`)
  },
  getUserGroups() {
    return request.get<any, { data: Group[] }>('/group/list')
  }
}
