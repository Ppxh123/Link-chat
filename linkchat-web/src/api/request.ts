import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { toast } from '@/components/design/LcToast.ts'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data
    if (data.code !== 200) {
      toast.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    const status = error.response?.status
    // 401（未认证）或 403（权限不足/Token版本过期）都清除 token 并跳转登录
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      if (window.location.pathname !== '/login') {
        toast.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    } else if (status >= 500) {
      toast.error('服务器异常，请稍后重试')
    } else {
      toast.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default instance
