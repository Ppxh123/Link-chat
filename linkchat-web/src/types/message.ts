export interface Message {
  id: string
  senderId: string
  senderName: string
  senderAvatar: string
  receiverId: number
  groupId: number | null
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'EMOJI' | 'SYSTEM'
  content: string
  fileUrl: string
  fileName: string
  fileSize: number
  fileMime?: string
  isRecalled: number
  quotedMsgId: number | null
  ackStatus: 'SENT' | 'DELIVERED' | 'READ' | 'FAILED'
  createdAt: string
}

export interface SendMessageRequest {
  receiverId: number
  groupId?: number
  messageType: string
  content?: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  fileMime?: string
  quotedMsgId?: number
}