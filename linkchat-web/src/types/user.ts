export interface User {
  id: string
  email: string
  nickname: string
  userCode: string
  avatarUrl: string
  signature: string
  status: number
  lastOnline: string
  role?: number
  isMuted?: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
}

export interface LoginResponse {
  token: string
  userId: number
  email: string
  nickname: string
  avatarUrl: string
  userCode: string
}
