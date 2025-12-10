<template>
  <div class="categories-view">
    <div class="container">
      <div class="content-wrapper">
        <!-- 分类选择栏 -->
        <div class="category-bar">
          <el-tag
            v-for="category in categories"
            :key="category.id"
            :type="selectedCategories.has(category.id) ? 'primary' : 'info'"
            :effect="selectedCategories.has(category.id) ? 'dark' : 'plain'"
            class="category-chip"
            @click="toggleCategory(category.id)"
          >
            {{ category.categoryName || category.category_name || category.name || `#${category.id}` }}
          </el-tag>
        </div>
        
        <!-- 图书列表 -->
        <div class="book-container">
          <div v-if="selectedCategories.size === 0" class="empty">
            <el-empty description="请选择分类查看图书" />
          </div>
          
          <div v-else-if="loading" class="loading">
            <el-skeleton :rows="5" animated />
          </div>
          
          <div v-else-if="books.length === 0" class="empty">
            <el-empty description="该分类下暂无图书" />
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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import { getCategoryList } from '@/api/category'
import { getBooksByCategoryIds } from '@/api/book'

const route = useRoute()

const categories = ref([])
const selectedCategories = ref(new Set())
const books = ref([])
const loading = ref(false)

// 加载分类列表
const loadCategories = async () => {
  try {
    const data = await getCategoryList()
    categories.value = data || []
  } catch (error) {
    ElMessage.error('加载分类失败')
    console.error(error)
  }
}

// 切换分类选择
const toggleCategory = (categoryId) => {
  if (selectedCategories.value.has(categoryId)) {
    selectedCategories.value.delete(categoryId)
  } else {
    selectedCategories.value.add(categoryId)
  }
  loadBooksByCategories()
}

// 根据选中的分类加载图书
const loadBooksByCategories = async () => {
  if (selectedCategories.value.size === 0) {
    books.value = []
    return
  }
  
  loading.value = true
  try {
    const categoryIds = Array.from(selectedCategories.value)
    const data = await getBooksByCategoryIds(categoryIds, 1) // mode=1 表示交集
    books.value = data || []
  } catch (error) {
    ElMessage.error('加载图书失败')
    console.error(error)
    books.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  
  // 滚动到页面顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
  
  // 只有当URL中有categoryId参数时，才自动选中该分类
  // 从导航栏直接点击"分类"时，不会有categoryId参数，所以不会选中任何分类
  const categoryId = route.query.categoryId
  if (categoryId) {
    const id = parseInt(categoryId)
    if (!isNaN(id)) {
      selectedCategories.value.add(id)
      await loadBooksByCategories()
    }
  }
  // 如果没有categoryId参数，selectedCategories保持为空，不加载任何图书
})

// 监听路由变化，当categoryId改变时更新选中的分类
watch(() => route.query.categoryId, async (newCategoryId) => {
  if (newCategoryId) {
    const id = parseInt(newCategoryId)
    if (!isNaN(id)) {
      // 滚动到页面顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
      
      // 清空之前的选择，只选中新的分类
      selectedCategories.value.clear()
      selectedCategories.value.add(id)
      await loadBooksByCategories()
    }
  }
})
</script>

<style scoped>
.categories-view {
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

.category-bar {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(26, 152, 170, 0.15);
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  border: 1px solid rgba(26, 152, 170, 0.1);
}

.category-chip {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 15px;
  padding: 10px 20px;
  border-radius: 20px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.category-chip:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 6px 20px rgba(26, 152, 170, 0.25);
}

/* 自定义选中状态的样式 */
:deep(.category-chip.el-tag--primary) {
  background: linear-gradient(135deg, #1995A7 0%, #178a9c 100%);
  border-color: #1995A7;
  color: white;
}

:deep(.category-chip.el-tag--primary:hover) {
  background: linear-gradient(135deg, #178a9c 0%, #156d7d 100%);
  border-color: #178a9c;
}

/* 自定义未选中状态的样式 */
:deep(.category-chip.el-tag--info) {
  background: white;
  border-color: #e0e0e0;
  color: #666;
}

:deep(.category-chip.el-tag--info:hover) {
  background: #f5f5f5;
  border-color: #1995A7;
  color: #1995A7;
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
  .book-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
  }
}

@media (max-width: 900px) {
  .book-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 10px;
  }
  
  .category-bar {
    padding: 15px;
  }
  
  .book-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .book-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>