import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBorrowedBooks, addBorrowedBook, removeBorrowedBook } from '@/utils/storage'

export const useBookStore = defineStore('book', () => {
  // 状态
  const books = ref([])
  const borrowedBooks = ref(getBorrowedBooks())
  const loading = ref(false)
  const offset = ref(0)
  const hasMore = ref(true)
  
  // 添加图书到列表
  function addBooks(newBooks) {
    books.value.push(...newBooks)
    offset.value += newBooks.length
  }
  
  // 清空图书列表
  function clearBooks() {
    books.value = []
    offset.value = 0
    hasMore.value = true
  }
  
  // 更新图书信息（仅用于store内部的books数组）
  function updateBook(bookId, updates) {
    const index = books.value.findIndex(b => b.id === bookId)
    if (index !== -1) {
      books.value[index] = { ...books.value[index], ...updates }
    }
  }
  
  // 标记为已借阅（只更新localStorage，不更新books数组）
  function markAsBorrowed(bookId) {
    addBorrowedBook(bookId)
    borrowedBooks.value = getBorrowedBooks()
  }
  
  // 标记为已归还（只更新localStorage，不更新books数组）
  function markAsReturned(bookId) {
    removeBorrowedBook(bookId)
    borrowedBooks.value = getBorrowedBooks()
  }
  
  // 检查是否已借阅
  function isBorrowed(bookId) {
    return borrowedBooks.value.has(Number(bookId))
  }
  
  // 设置加载状态
  function setLoading(value) {
    loading.value = value
  }
  
  // 设置是否还有更多
  function setHasMore(value) {
    hasMore.value = value
  }
  
  return {
    books,
    borrowedBooks,
    loading,
    offset,
    hasMore,
    addBooks,
    clearBooks,
    updateBook,
    markAsBorrowed,
    markAsReturned,
    isBorrowed,
    setLoading,
    setHasMore
  }
})