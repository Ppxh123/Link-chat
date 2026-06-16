import request from './request'

export const statusApi = {
  updateStatus(status: number) {
    return request.put('/status', null, { params: { status } })
  },
  getStatus(userId: number) {
    return request.get('/status/' + userId)
  }
}