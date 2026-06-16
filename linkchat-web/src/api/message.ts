import request from './request'
import type { Message, SendMessageRequest } from '@/types/message'

export const messageApi = {
  sendMessage(data: SendMessageRequest) {
    return request.post<any, { data: Message }>('/message/send', data)
  },
  getChatHistory(params: { peerId?: number; groupId?: number; page: number; size: number }) {
    return request.get<any, { data: Message[] }>('/message/history', { params })
  },
  recallMessage(messageId: string) {
    return request.put(`/message/recall/${messageId}`)
  },
  deleteMessage(messageId: string) {
    return request.delete(`/message/${messageId}`)
  },
  forwardMessage(messageId: string, targetId: number) {
    return request.post<any, { data: Message }>(`/message/forward/${messageId}`, null, { params: { targetId } })
  },
  searchMessages(peerId: number, keyword: string) {
    return request.get<any, { data: Message[] }>('/message/search', { params: { peerId, keyword } })
  },
  markAsRead(senderId: number) {
    return request.put(`/message/read/${senderId}`)
  }
}
