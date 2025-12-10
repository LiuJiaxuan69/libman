import request from './request'

/**
 * 获取图书列表（分页）
 */
export function getBookListByPage(pageRequest) {
  return request({
    url: '/book/getListByPage',
    method: 'post',
    data: pageRequest
  })
}

/**
 * 获取图书列表（按偏移量）
 */
export function getBookListByOffset(offsetRequest) {
  return request({
    url: '/book/getListByOffset',
    method: 'post',
    data: offsetRequest
  })
}

/**
 * 获取首页图书
 */
export function getIndexPage(pageSize = 10) {
  return request({
    url: '/book/getIndexPage',
    method: 'get',
    params: { pageSize }
  })
}

/**
 * 借阅图书
 */
export function borrowBook(bookId) {
  return request({
    url: '/book/borrowBook',
    method: 'post',
    data: bookId,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 归还图书
 */
export function returnBook(bookId) {
  return request({
    url: '/book/returnBook',
    method: 'post',
    data: bookId,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 添加图书
 */
export function addBook(bookInfo) {
  return request({
    url: '/book/addBook',
    method: 'post',
    data: bookInfo
  })
}

/**
 * 添加图书（带封面上传）
 */
export function addBookWithCover(bookInfo, file) {
  const formData = new FormData()
  formData.append('book', JSON.stringify(bookInfo))
  if (file) {
    formData.append('file', file)
  }
  return request({
    url: '/book/addBookWithCover',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 检查是否加载完毕
 */
export function checkIsEnd(currentCount) {
  return request({
    url: '/book/isEnd',
    method: 'post',
    data: currentCount,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 根据分类ID获取图书
 */
export function getBooksByCategoryIds(categoryIds, mode = 1) {
  return request({
    url: '/book/getBooksByCategoryIds',
    method: 'post',
    data: { categoryIds, mode }
  })
}

/**
 * 获取所有图书
 */
export function getAllBooks() {
  return request({
    url: '/book/getListByOffset',
    method: 'post',
    data: {
      offset: 0,
      limit: 1000 // 获取大量图书
    }
  })
}

/**
 * 获取图书封面
 */
export function getBookCover(bookId) {
  return request({
    url: `/book/${bookId}/cover`,
    method: 'get'
  })
}

/**
 * 订阅图书更新（SSE）
 */
export function subscribeBookUpdates() {
  return new EventSource('/api/book/subscribe')
}