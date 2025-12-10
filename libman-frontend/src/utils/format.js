/**
 * 格式化工具函数
 */

/**
 * 格式化图书状态
 */
export function formatBookStatus(status) {
  const statusMap = {
    0: '无效',
    1: '可借阅',
    2: '已借出',
    3: '不存在'
  }
  return statusMap[status] || '未知'
}

/**
 * 获取状态对应的类型（用于 Element Plus Tag）
 */
export function getStatusType(status) {
  const typeMap = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

/**
 * 格式化标签（JSON数组转逗号分隔）
 */
export function formatTags(tagsJson) {
  if (!tagsJson) return ''
  try {
    const arr = JSON.parse(tagsJson)
    if (Array.isArray(arr)) {
      return arr.join(', ')
    }
    return tagsJson
  } catch (error) {
    return tagsJson
  }
}

/**
 * 格式化分类名称
 */
export function formatCategories(categoryNames) {
  if (!categoryNames) return '未分类'
  return categoryNames
}

/**
 * 格式化价格
 */
export function formatPrice(price) {
  if (price === null || price === undefined) return '未定价'
  return `¥${Number(price).toFixed(2)}`
}

/**
 * 格式化日期
 */
export function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * HTML 转义
 */
export function escapeHtml(str) {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}