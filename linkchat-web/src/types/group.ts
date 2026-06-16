export interface Group {
  id: string
  name: string
  avatarUrl: string
  ownerId: string
  announcement: string
  memberCount: number
  myRole: number
  isMuted: number
  createdAt: string
}

export interface CreateGroupRequest {
  name: string
  memberIds: number[]
}
