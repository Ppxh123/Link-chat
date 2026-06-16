/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

/**
 * 获取文件扩展名
 */
export function getFileExtension(filename: string): string {
  const dot = filename.lastIndexOf('.')
  return dot === -1 ? '' : filename.substring(dot + 1).toLowerCase()
}

/**
 * 判断文件是否为图片
 */
export function isImageFile(filename: string): boolean {
  const ext = getFileExtension(filename)
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(ext)
}

/**
 * 判断文件是否为 PDF
 */
export function isPdfFile(filename: string): boolean {
  return getFileExtension(filename) === 'pdf'
}

/**
 * 判断文件是否为 Office 文档
 */
export function isOfficeFile(filename: string): boolean {
  const ext = getFileExtension(filename)
  return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)
}

/**
 * 判断文件是否为文本文件
 */
export function isTextFile(filename: string): boolean {
  const ext = getFileExtension(filename)
  return ['txt', 'md', 'json', 'xml', 'csv', 'log', 'yml', 'yaml', 'ini', 'cfg', 'py', 'js', 'ts', 'java', 'css', 'html', 'sql'].includes(ext)
}
