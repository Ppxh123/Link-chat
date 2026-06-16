export function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000 && date.getDate() === now.getDate()) return `${hours}:${minutes}`

  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.getDate() === yesterday.getDate() &&
      date.getMonth() === yesterday.getMonth() &&
      date.getFullYear() === yesterday.getFullYear()) {
    return `昨天 ${hours}:${minutes}`
  }

  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  if (date.getFullYear() === now.getFullYear()) {
    return `${month}-${day} ${hours}:${minutes}`
  }
  return `${date.getFullYear()}-${month}-${day}`
}

export function formatTimeDivider(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')

  if (date.toDateString() === now.toDateString()) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  if (date.getFullYear() === now.getFullYear()) return `${month}月${day}日`
  return `${date.getFullYear()}年${month}月${day}日`
}