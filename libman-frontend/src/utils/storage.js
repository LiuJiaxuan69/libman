/**
 * 本地存储工具类
 */

const BORROWED_BOOKS_KEY = 'borrowed_books'

/**
 * 获取已借阅的图书ID集合
 */
export function getBorrowedBooks() {
  try {
    const data = localStorage.getItem(BORROWED_BOOKS_KEY)
    if (!data) return new Set()
    const arr = JSON.parse(data)
    return new Set(arr.map(id => Number(id)))
  } catch (error) {
    console.error('获取借阅记录失败:', error)
    return new Set()
  }
}

/**
 * 保存已借阅的图书ID集合
 */
export function saveBorrowedBooks(bookIds) {
  try {
    const arr = Array.from(bookIds)
    localStorage.setItem(BORROWED_BOOKS_KEY, JSON.stringify(arr))
  } catch (error) {
    console.error('保存借阅记录失败:', error)
  }
}

/**
 * 添加借阅记录
 */
export function addBorrowedBook(bookId) {
  const books = getBorrowedBooks()
  books.add(Number(bookId))
  saveBorrowedBooks(books)
}

/**
 * 移除借阅记录
 */
export function removeBorrowedBook(bookId) {
  const books = getBorrowedBooks()
  books.delete(Number(bookId))
  saveBorrowedBooks(books)
}

/**
 * 检查是否已借阅
 */
export function isBorrowed(bookId) {
  const books = getBorrowedBooks()
  return books.has(Number(bookId))
}

/**
 * 清空借阅记录
 */
export function clearBorrowedBooks() {
  localStorage.removeItem(BORROWED_BOOKS_KEY)
}