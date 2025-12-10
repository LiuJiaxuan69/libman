<template>
  <div class="my-donations-view">
    <div class="container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <h1>
            <el-icon><Box /></el-icon>
            我的捐赠
          </h1>
          <p>管理您捐赠的所有图书</p>
        </div>
        <el-button type="primary" size="large" @click="router.push('/donate')">
          <el-icon><Plus /></el-icon>
          捐赠新书
        </el-button>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #1A98AA 0%, #178a9c 100%);">
            <el-icon :size="32"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalBooks }}</div>
            <div class="stat-label">总捐赠数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ availableBooks }}</div>
            <div class="stat-label">可借阅</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ borrowedBooks }}</div>
            <div class="stat-label">已借出</div>
          </div>
        </div>
      </div>

      <!-- 搜索和筛选 -->
      <div class="filter-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索书名、作者..."
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable class="status-filter">
          <el-option label="全部" value="" />
          <el-option label="可借阅" :value="0" />
          <el-option label="已借出" :value="1" />
        </el-select>
      </div>

      <!-- 图书列表 -->
      <div v-loading="loading" class="books-grid">
        <div v-if="filteredBooks.length === 0" class="empty-state">
          <el-icon :size="80" color="#ccc"><DocumentDelete /></el-icon>
          <p>暂无捐赠的图书</p>
          <el-button type="primary" @click="router.push('/donate')">
            立即捐赠
          </el-button>
        </div>

        <div
          v-for="book in filteredBooks"
          :key="book.id"
          class="book-card"
        >
          <div class="book-cover">
            <div v-if="!book.coverLoaded" class="cover-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <img
              v-else
              :src="getBookCover(book)"
              :alt="book.bookName"
              @error="handleImageError"
            />
            <div class="book-status" :class="book.status === 1 ? 'available' : 'borrowed'">
              {{ book.status === 1 ? '可借阅' : '已借出' }}
            </div>
          </div>
          <div class="book-info">
            <h3 class="book-title">{{ book.bookName }}</h3>
            <p class="book-author">{{ book.author }}</p>
            <p class="book-publish">{{ book.publish }}</p>
            <div class="book-categories" v-if="book.categoryNames">
              <el-tag
                v-for="(cat, idx) in book.categoryNames.split(',')"
                :key="idx"
                size="small"
                type="info"
              >
                {{ cat }}
              </el-tag>
            </div>
            <div class="book-actions">
              <el-button
                type="primary"
                size="small"
                @click="editBook(book)"
                :icon="Edit"
              >
                编辑
              </el-button>
              <el-button
                type="info"
                size="small"
                @click="viewDetails(book)"
                :icon="View"
              >
                详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑图书信息"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        v-if="editingBook"
        :model="editingBook"
        label-width="100px"
        class="edit-form"
      >
        <el-form-item label="书名">
          <el-input v-model="editingBook.bookName" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editingBook.author" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="editingBook.publish" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editingBook.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editingBook.description"
            type="textarea"
            :rows="4"
            placeholder="请输入图书描述"
          />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            class="cover-uploader"
            :action="`/api/book/${editingBook.id}/cover`"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeCoverUpload"
            :headers="{ 'X-Requested-With': 'XMLHttpRequest' }"
          >
            <img v-if="editingBook.coverUrl" :src="getBookCover(editingBook)" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBook" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="图书详情"
      width="500px"
    >
      <div v-if="viewingBook" class="book-detail">
        <div class="detail-cover">
          <div v-if="!viewingBook.coverLoaded" class="cover-loading-dialog">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载封面中...</span>
          </div>
          <img
            v-else
            :src="getBookCover(viewingBook)"
            :alt="viewingBook.bookName"
            @error="handleImageError"
          />
        </div>
        <div class="detail-info">
          <h2>{{ viewingBook.bookName }}</h2>
          <p><strong>作者：</strong>{{ viewingBook.author }}</p>
          <p><strong>出版社：</strong>{{ viewingBook.publish }}</p>
          <p><strong>价格：</strong>¥{{ viewingBook.price }}</p>
          <p><strong>状态：</strong>
            <el-tag :type="viewingBook.status === 1 ? 'success' : 'warning'">
              {{ viewingBook.status === 1 ? '可借阅' : '已借出' }}
            </el-tag>
          </p>
          <p v-if="viewingBook.description"><strong>描述：</strong>{{ viewingBook.description }}</p>
          <div v-if="viewingBook.categoryNames">
            <strong>分类：</strong>
            <el-tag
              v-for="(cat, idx) in viewingBook.categoryNames.split(',')"
              :key="idx"
              size="small"
              style="margin-right: 8px;"
            >
              {{ cat }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Box,
  Plus,
  Reading,
  CircleCheck,
  Clock,
  Search,
  DocumentDelete,
  Edit,
  View,
  Loading
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { getBookCover as fetchBookCover } from '@/api/book'

const router = useRouter()

const books = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const editDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editingBook = ref(null)
const viewingBook = ref(null)
const saving = ref(false)

// 统计数据
const totalBooks = computed(() => books.value.length)
const availableBooks = computed(() => books.value.filter(b => b.status === 1).length)
const borrowedBooks = computed(() => books.value.filter(b => b.status === 2).length)

// 过滤后的图书列表
const filteredBooks = computed(() => {
  let result = books.value

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(book =>
      book.bookName.toLowerCase().includes(query) ||
      book.author.toLowerCase().includes(query)
    )
  }

  // 状态过滤
  if (statusFilter.value !== '') {
    result = result.filter(book => book.status === statusFilter.value)
  }

  return result
})

// 获取图书封面URL（用于显示）
const getBookCover = (book) => {
  if (!book.coverUrl || book.coverUrl === 'default.png') {
    return '/img/book-placeholder.jpg'
  }
  if (book.coverUrl.startsWith('http://') || book.coverUrl.startsWith('https://')) {
    return book.coverUrl
  }
  // 直接使用 /api 前缀，Vite 代理会转发到后端
  if (!book.coverUrl.startsWith('/covers/')) {
    return `/api/covers/${book.coverUrl}`
  }
  return `/api${book.coverUrl}`
}

// 懒加载图书封面
const loadBookCover = async (book) => {
  if (book.coverLoaded) return
  
  try {
    const response = await fetchBookCover(book.id)
    if (response) {
      let url = response
      
      // 处理默认封面
      if (!url || url === 'default.png') {
        book.coverUrl = 'default.png'
        book.coverLoaded = true
        return
      }
      
      // 处理相对路径 - 保存为相对路径，显示时通过 getBookCover 添加 /api 前缀
      if (url && !url.startsWith('http://') && !url.startsWith('https://')) {
        if (!url.startsWith('/covers/')) {
          url = `/covers/${url}`
        }
        book.coverUrl = url
      } else {
        book.coverUrl = url
      }
      
      book.coverLoaded = true
    }
  } catch (error) {
    console.error('获取封面失败:', error)
    book.coverUrl = 'default.png'
    book.coverLoaded = true
  }
}

// 图片加载错误处理
const handleImageError = (e) => {
  e.target.src = '/img/book-placeholder.jpg'
}

// 加载我的捐赠
const loadMyDonations = async () => {
  loading.value = true
  try {
    const result = await request.get('/book/my')
    books.value = (result || []).map(book => ({
      ...book,
      coverLoaded: false // 初始化封面加载状态
    }))
    
    // 懒加载所有图书封面
    books.value.forEach(book => {
      loadBookCover(book)
    })
  } catch (error) {
    console.error('加载捐赠列表失败:', error)
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 编辑图书
const editBook = (book) => {
  editingBook.value = { ...book }
  editDialogVisible.value = true
}

// 查看详情
const viewDetails = (book) => {
  viewingBook.value = book
  detailDialogVisible.value = true
}

// 保存图书
const saveBook = async () => {
  if (!editingBook.value) return

  saving.value = true
  try {
    await request.patch(`/book/${editingBook.value.id}`, {
      bookName: editingBook.value.bookName,
      author: editingBook.value.author,
      publish: editingBook.value.publish,
      price: editingBook.value.price,
      description: editingBook.value.description
    })

    ElMessage.success('保存成功')
    editDialogVisible.value = false
    await loadMyDonations()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 封面上传成功
const handleCoverSuccess = (response) => {
  if (response.status === 'SUCCESS') {
    ElMessage.success('封面上传成功')
    loadMyDonations()
  } else {
    ElMessage.error('封面上传失败')
  }
}

// 封面上传前验证
const beforeCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

onMounted(() => {
  loadMyDonations()
})
</script>

<style scoped>
.my-donations-view {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 20px 20px 40px 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 100px; /* 左右对称留出空间，避免与浮动工具栏重叠 */
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding: 0;
  background: transparent;
}

.header-content h1 {
  margin: 0 0 8px 0;
  font-size: 36px;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.header-content h1 .el-icon {
  color: #1A98AA;
}

.header-content p {
  margin: 0;
  color: #666;
  font-size: 16px;
  font-weight: 400;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1A98AA, #178a9c);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border-color: #1A98AA;
}

.stat-card:hover::before {
  transform: scaleX(1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon :deep(.el-icon) {
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8e8e8;
}

.search-input {
  flex: 1;
  max-width: 500px;
}

.status-filter {
  width: 180px;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  min-height: 400px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  border: 2px dashed #e8e8e8;
}

.empty-state p {
  font-size: 16px;
  color: #999;
  margin: 16px 0 24px 0;
  font-weight: 500;
}

.book-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.book-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12);
  border-color: #1A98AA;
}

.book-cover {
  position: relative;
  height: 260px;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 32px;
}

.cover-loading-dialog {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #909399;
  font-size: 16px;
  background: #f5f5f5;
}

.cover-loading-dialog .el-icon {
  font-size: 40px;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.book-card:hover .book-cover img {
  transform: scale(1.05);
}

.book-status {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 2;
}

.book-status.available {
  background: rgba(82, 196, 26, 0.95);
  color: white;
}

.book-status.borrowed {
  background: rgba(250, 173, 20, 0.95);
  color: white;
}

.book-info {
  padding: 16px;
}

.book-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: -0.2px;
}

.book-author,
.book-publish {
  margin: 4px 0;
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.book-categories {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.book-categories :deep(.el-tag) {
  font-size: 12px;
  padding: 2px 8px;
}

.book-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.book-actions .el-button {
  flex: 1;
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 12px;
}

.edit-form {
  padding: 20px 0;
}

.cover-uploader {
  width: 160px;
  height: 220px;
  border: 2px dashed #d9d9d9;
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
}

.cover-uploader:hover {
  border-color: #1A98AA;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-uploader-icon {
  font-size: 48px;
  color: #999;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-cover {
  width: 100%;
  height: 320px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info h2 {
  margin: 0 0 20px 0;
  font-size: 26px;
  color: #1a1a1a;
  font-weight: 700;
}

.detail-info p {
  margin: 10px 0;
  font-size: 15px;
  color: #666;
  line-height: 1.8;
}

.detail-info strong {
  color: #1a1a1a;
  margin-right: 8px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .my-donations-view {
    padding: 20px 16px 20px 16px;
  }
  
  .container {
    padding: 0 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .header-content h1 {
    font-size: 28px;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    padding: 16px;
  }

  .search-input {
    max-width: 100%;
  }

  .status-filter {
    width: 100%;
  }

  .books-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>