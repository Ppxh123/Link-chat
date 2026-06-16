import request from './request'
import type { LoginRequest, RegisterRequest, LoginResponse } from '@/types/user'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<any, { data: LoginResponse }>('/auth/login', data)
  },
  register(data: RegisterRequest) {
    return request.post('/auth/register', data)
  },
  changePassword(data: { oldPassword: string; newPassword: string }) {
    return request.put('/auth/password', data)
  }
}