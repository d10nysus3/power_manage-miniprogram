/**
 * 格式化工具函数
 */

// 统一规范日期字符串，兼容 iOS 上 new Date 的限制
function normalizeDateStr(dateStr: string): string {
  if (!dateStr) return ''
  // 已经是 ISO 或纯日期的，直接返回
  if (dateStr.includes('T')) return dateStr
  // 后端常见格式：yyyy-MM-dd HH:mm:ss → 转成 iOS 支持的 yyyy/MM/dd HH:mm:ss
  const hasTime = dateStr.includes(' ')
  const [datePart, timePart] = hasTime ? dateStr.split(' ') : [dateStr, '']
  const safeDate = datePart.replace(/-/g, '/')
  return hasTime ? `${safeDate} ${timePart}` : safeDate
}

// 安全创建 Date，失败时返回 null
function toSafeDate(dateStr: string): Date | null {
  const normalized = normalizeDateStr(dateStr)
  const d = new Date(normalized)
  return isNaN(d.getTime()) ? null : d
}

// 格式化日期时间
export function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = toSafeDate(dateStr)
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 格式化日期
export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = toSafeDate(dateStr)
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 格式化时间
export function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = toSafeDate(dateStr)
  if (!date) return ''
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

// 格式化数字（千分位）
export function formatNumber(num: number): string {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 格式化异常类型
export function formatAnomalyType(type: string): string {
  const typeMap: Record<string, string> = {
    'overload': '过载',
    'leakage': '漏电',
    'fluctuation': '异常波动',
    'outage': '停电',
    // 兼容后端 anomaly-records/search 返回的文案
    'HIGH_DEVIATION': '高峰异常',
    'PATTERN_DEVIATION': '模式异常',
    'TREND_DEVIATION': '趋势异常',
    // 兼容 Dashboard /type-distribution translate 文案
    '高偏差': '高峰异常',
    '模式偏离': '模式异常',
    '趋势偏离': '趋势异常'
  }
  return typeMap[type] || type
}

// 格式化严重程度
export function formatSeverity(severity: string): string {
  const severityMap: Record<string, string> = {
    'low': '低',
    'medium': '中',
    'high': '高',
    'critical': '紧急'
  }
  return severityMap[severity] || severity
}

// 获取异常类型标签颜色
export function getAnomalyTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    'overload': 'danger',
    'leakage': 'warning',
    'fluctuation': 'info',
    'outage': 'danger',
    // 兼容后端 anomaly-records/search 返回的文案（中文）
    '高峰异常': 'danger',
    '模式异常': 'warning',
    '趋势异常': 'info',
    // 兼容后端原始枚举值
    'HIGH_DEVIATION': 'danger',
    'PATTERN_DEVIATION': 'warning',
    'TREND_DEVIATION': 'info',
    // 兼容 translate 文案
    '高偏差': 'danger',
    '模式偏离': 'warning',
    '趋势偏离': 'info'
  }
  return colorMap[type] || 'info'
}

// 获取严重程度标签颜色
export function getSeverityColor(severity: string): string {
  const colorMap: Record<string, string> = {
    'low': 'info',
    'medium': 'warning',
    'high': 'danger',
    'critical': 'danger'
  }
  return colorMap[severity] || 'info'
}

// 相对时间格式化（如：刚刚、5分钟前）
export function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = toSafeDate(dateStr)
  if (!date) return ''
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return formatDate(dateStr)
}