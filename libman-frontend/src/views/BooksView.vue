<template>
  <div class="books-view">
    <div class="container">
      <div class="content-wrapper">
        <h1 class="page-title">图书列表</h1>
        
        <!-- 搜索框 -->
        <div class="search-section">
          <div class="search-container">
            <el-select
              v-model="searchType"
              class="search-select"
              placeholder="搜索类型"
            >
              <el-option label="书名搜索" value="bookName" />
              <el-option label="作者搜索" value="author" />
              <el-option label="出版社搜索" value="publish" />
            </el-select>
            <div class="search-input-wrapper">
              <el-icon class="search-icon"><Search /></el-icon>
              <input
                v-model="searchKeyword"
                type="text"
                class="search-input"
                placeholder="请输入您要检索的信息"
                @keyup.enter="handleSearch"
              />
            </div>
            <button class="search-button" @click="handleSearch">
              检索
            </button>
          </div>
          <div v-if="searchKeyword" class="search-result-hint">
            找到 {{ books.length }} 本图书
          </div>
        </div>
        
        <!-- 图书列表 -->
        <div class="book-container">
          <div v-if="loading" class="loading">
            <div class="skeleton-grid">
              <el-skeleton
                v-for="i in 8"
                :key="i"
                animated
                class="skeleton-card"
              >
                <template #template>
                  <el-skeleton-item variant="image" style="width: 100%; height: 280px;" />
                  <el-skeleton-item variant="h3" style="width: 80%; margin-top: 10px;" />
                  <el-skeleton-item variant="text" style="width: 60%; margin-top: 8px;" />
                </template>
              </el-skeleton>
            </div>
          </div>
          
          <div v-else-if="books.length === 0" class="empty">
            <el-empty description="暂无图书" />
          </div>
          
          <div v-else class="book-grid">
            <BookCard
              v-for="book in books"
              :key="book.id"
              :book="book"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import BookCard from '@/components/BookCard.vue'
import { getBookListByPage } from '@/api/book'

const allBooks = ref([])
const searchKeyword = ref('')
const searchType = ref('bookName')
const loading = ref(false)

// 根据搜索关键词和类型过滤图书
const books = computed(() => {
  if (!searchKeyword.value.trim()) {
    return allBooks.value
  }
  
  const keyword = searchKeyword.value.toLowerCase().trim()
  return allBooks.value.filter(book => {
    const fieldValue = (book[searchType.value] || '').toLowerCase()
    return fieldValue.includes(keyword)
  })
})

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已通过 computed 实现，这里可以添加额外的处理
  if (searchKeyword.value.trim() && books.value.length === 0) {
    ElMessage.info('未找到匹配的图书')
  }
}

// 加载所有图书 - 使用分页API
const loadAllBooks = async () => {
  loading.value = true
  try {
    // 使用分页API获取图书，一次获取多页
    const pageSize = 50 // 每页50本
    const maxPages = 20 // 最多获取20页，共1000本
    let allBooksData = []
    
    for (let page = 1; page <= maxPages; page++) {
      console.log(`正在请求第 ${page} 页...`)
      const result = await getBookListByPage({
        currentPage: page,
        pageSize: pageSize
      })
      
      console.log(`第 ${page} 页返回结果:`, result)
      
      if (result) {
        // 检查不同的数据结构
        let bookList = null
        if (Array.isArray(result)) {
          bookList = result
        } else if (result.records) {
          // 后端返回的是 records 字段
          bookList = result.records
        } else if (result.list) {
          bookList = result.list
        } else if (result.data && Array.isArray(result.data)) {
          bookList = result.data
        } else if (result.data && result.data.records) {
          bookList = result.data.records
        } else if (result.data && result.data.list) {
          bookList = result.data.list
        }
        
        console.log(`第 ${page} 页解析后的图书列表:`, bookList)
        
        if (bookList && bookList.length > 0) {
          allBooksData = allBooksData.concat(bookList)
          console.log(`加载第 ${page} 页，获取 ${bookList.length} 本图书`)
          
          // 如果返回的数量少于pageSize，说明已经是最后一页
          if (bookList.length < pageSize) {
            break
          }
        } else {
          console.log(`第 ${page} 页没有数据，停止加载`)
          break
        }
      } else {
        console.log(`第 ${page} 页返回null，停止加载`)
        break
      }
    }
    
    allBooks.value = allBooksData
    console.log('总共加载图书:', allBooks.value.length, '本')
    console.log('图书数据:', allBooks.value)
    
    if (allBooks.value.length === 0) {
      ElMessage.info('暂无图书数据，请检查数据库中是否有图书记录')
    }
  } catch (error) {
    ElMessage.error('加载图书失败：' + (error.message || '未知错误'))
    console.error('加载图书错误:', error)
    console.error('错误详情:', error.response || error)
    allBooks.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadAllBooks()
})
</script>

<style scoped>
.books-view {
  min-height: 100vh;
}

.container {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
}

/* 搜索区域 */
.search-section {
  background: #f5f5f5;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.search-container {
  width: 100%;
  max-width: 1000px;
  display: flex;
  align-items: center;
  gap: 0;
  background: white;
  border-radius: 50px;
  padding: 8px 8px 8px 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.search-container:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.search-select {
  width: 140px;
  flex-shrink: 0;
}

:deep(.search-select .el-input__wrapper) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
}

:deep(.search-select .el-input__inner) {
  color: #409eff;
  font-weight: 500;
  font-size: 15px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 15px;
  border-left: 1px solid #e0e0e0;
}

.search-icon {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
  background: transparent;
}

.search-input::placeholder {
  color: #c0c4cc;
}

.search-button {
  padding: 12px 40px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.search-button:hover {
  background: linear-gradient(135deg, #3a8ee6 0%, #337ecc 100%);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.search-button:active {
  transform: scale(0.98);
}

.search-result-hint {
  text-align: center;
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.book-container {
  background: transparent;
  padding: 0;
  min-height: 400px;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
}

.loading,
.empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .book-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
  }
}

@media (max-width: 900px) {
  .book-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 10px;
  }
  
  .page-title {
    font-size: 24px;
    margin-bottom: 20px;
  }
  
  .book-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .book-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>