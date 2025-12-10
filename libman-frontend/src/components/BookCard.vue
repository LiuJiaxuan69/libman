<template>
  <div class="book-card" @click="showDetails">
    <!-- 图书封面 -->
    <div class="book-cover">
      <div v-if="coverLoading" class="cover-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载封面中...</span>
      </div>
      <img
        v-else
        :src="coverUrl"
        :alt="book.bookName"
        @error="handleImageError"
      />
      <div class="book-status-badge" :class="statusClass">
        {{ statusText }}
      </div>
    </div>
    
    <!-- 图书信息 -->
    <div class="book-info">
      <h3 class="book-title" :title="book.bookName">{{ book.bookName }}</h3>
      <p class="book-author">{{ book.author }}</p>
      <p class="book-meta">{{ book.publish }}</p>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="book.bookName"
      width="600px"
      append-to-body
      align-center
      :close-on-click-modal="false"
      class="book-dialog"
      @close="dialogVisible = false"
    >
      <div class="dialog-content">
        <div class="dialog-cover">
          <div v-if="coverLoading" class="cover-loading-dialog">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载封面中...</span>
          </div>
          <img
            v-else
            :src="coverUrl"
            :alt="book.bookName"
            @error="handleImageError"
          />
          <div class="cover-overlay">
            <el-tag :type="statusType" size="large" class="status-tag">
              {{ statusText }}
            </el-tag>
          </div>
        </div>
        <div class="dialog-info">
          <div class="info-item">
            <span class="info-label">作者</span>
            <span class="info-value">{{ book.author }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">出版社</span>
            <span class="info-value">{{ book.publish }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">定价</span>
            <span class="info-value price">{{ formatPrice(book.price) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">分类</span>
            <span class="info-value">{{ formatCategories(book.categoryNames) }}</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-actions">
          <el-button
            v-if="canBorrow"
            size="large"
            :loading="borrowing"
            :disabled="cooldown > 0"
            :class="['action-button', cooldown > 0 ? 'countdown-button' : 'borrow-button']"
            @click="handleBorrow"
          >
            <el-icon v-if="!borrowing"><Reading /></el-icon>
            {{ cooldown > 0 ? `${cooldown}秒后可操作` : '借阅' }}
          </el-button>
          <el-button
            v-else-if="isBorrowedByMe"
            type="success"
            size="large"
            :loading="returning"
            :disabled="cooldown > 0"
            class="action-button return-button"
            @click="handleReturn"
          >
            <el-icon v-if="!returning"><CircleCheck /></el-icon>
            {{ cooldown > 0 ? `${cooldown}秒后可操作` : '归还' }}
          </el-button>
          <el-button v-else size="large" disabled class="action-button">
            <el-icon><Lock /></el-icon>
            已借出
          </el-button>
          <el-button size="large" class="close-button" @click="dialogVisible = false">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, CircleCheck, Lock, Loading } from '@element-plus/icons-vue'
import { borrowBook, returnBook, getBookCover } from '@/api/book'
import { useBookStore } from '@/stores/book'
import { formatTags, formatCategories, formatPrice, getStatusType, formatBookStatus } from '@/utils/format'

const props = defineProps({
  book: {
    type: Object,
    required: true
  }
})

const bookStore = useBookStore()
const dialogVisible = ref(false)
const borrowing = ref(false)
const returning = ref(false)
const cooldown = ref(0)
const coverUrl = ref('/img/book-placeholder.jpg') // 封面URL
const coverLoading = ref(false) // 封面加载状态
const coverLoaded = ref(false) // 封面是否已加载
let cooldownTimer = null

// 获取书籍封面（懒加载）
const fetchCoverUrl = async () => {
  if (!props.book.id || coverLoaded.value) return
  
  coverLoading.value = true
  try {
    const response = await getBookCover(props.book.id)
    console.log(`[BookCard] 书籍 ${props.book.id} 封面响应:`, response)
    
    if (response) {
      // 后端返回的是文件名或URL
      let url = response
      
      // 处理默认封面
      if (!url || url === 'default.png') {
        console.log(`[BookCard] 使用默认封面`)
        coverUrl.value = '/img/book-placeholder.jpg'
        coverLoaded.value = true
        return
      }
      
      // 处理相对路径
      if (url && !url.startsWith('http://') && !url.startsWith('https://')) {
        // 如果是文件名（如 "book_123_xxx.jpg"），拼接 /covers/ 前缀
        if (!url.startsWith('/covers/')) {
          url = `/covers/${url}`
        }
        // 直接使用 /api 前缀，Vite 代理会转发到后端
        const finalUrl = `/api${url}`
        console.log(`[BookCard] 最终封面URL:`, finalUrl)
        coverUrl.value = finalUrl
      } else {
        // 绝对URL直接使用
        console.log(`[BookCard] 使用绝对URL:`, url)
        coverUrl.value = url
      }
      
      coverLoaded.value = true
    } else {
      console.warn(`[BookCard] 书籍 ${props.book.id} 未返回封面`)
      coverUrl.value = '/img/book-placeholder.jpg'
      coverLoaded.value = true
    }
  } catch (error) {
    console.error(`[BookCard] 获取书籍 ${props.book.id} 封面失败:`, error)
    // 保持默认占位图
    coverUrl.value = '/img/book-placeholder.jpg'
    coverLoaded.value = true
  } finally {
    coverLoading.value = false
  }
}

// 组件挂载时立即加载封面（卡片封面）
onMounted(() => {
  fetchCoverUrl()
})

// 根据 bookStore 判断当前图书的实际状态
const actualStatus = computed(() => {
  // 如果在借阅列表中，状态为已借出(2)，否则为可借阅(1)
  return bookStore.isBorrowed(props.book.id) ? 2 : 1
})

const statusType = computed(() => getStatusType(actualStatus.value))
const statusText = computed(() => formatBookStatus(actualStatus.value))

const isBorrowedByMe = computed(() => {
  return bookStore.isBorrowed(props.book.id)
})

const canBorrow = computed(() => {
  return actualStatus.value === 1 && !isBorrowedByMe.value
})

const statusClass = computed(() => {
  return actualStatus.value === 1 ? 'available' : 'borrowed'
})

const showDetails = () => {
  dialogVisible.value = true
  // 确保封面已加载
  if (!coverLoaded.value) {
    fetchCoverUrl()
  }
}

const handleImageError = (e) => {
  e.target.src = '/img/book-placeholder.jpg'
}

const startCooldown = (seconds) => {
  cooldown.value = seconds
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
  cooldownTimer = setInterval(() => {
    cooldown.value--
    if (cooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

const handleBorrow = async () => {
  borrowing.value = true
  try {
    await borrowBook(props.book.id)
    // 只更新 bookStore，actualStatus 会自动响应
    bookStore.markAsBorrowed(props.book.id)
    ElMessage.success('借阅成功')
    startCooldown(3)
  } catch (error) {
    ElMessage.error(error.message || '借阅失败')
  } finally {
    borrowing.value = false
  }
}

const handleReturn = async () => {
  returning.value = true
  try {
    await returnBook(props.book.id)
    // 只更新 bookStore，actualStatus 会自动响应
    bookStore.markAsReturned(props.book.id)
    ElMessage.success('归还成功')
    startCooldown(3)
  } catch (error) {
    ElMessage.error(error.message || '归还失败')
  } finally {
    returning.value = false
  }
}

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
})
</script>

<style scoped>
.book-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.book-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.book-cover {
  position: relative;
  width: 100%;
  padding-top: 120%; /* 调整为 6:5 比例，降低高度 */
  background: #f5f5f5;
  overflow: hidden;
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
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.cover-loading .el-icon {
  font-size: 24px;
}

.cover-loading-dialog {
  width: 100%;
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
  background: #f5f5f5;
}

.cover-loading-dialog .el-icon {
  font-size: 32px;
}

.book-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.book-card:hover .book-cover img {
  transform: scale(1.05);
}

.book-status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: white;
}

.book-status-badge.available {
  background: #67c23a;
}

.book-status-badge.borrowed {
  background: #909399;
}

.book-info {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.book-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.3;
  min-height: 39px;
}

.book-author {
  font-size: 13px;
  color: #666;
  margin: 0 0 3px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-meta {
  font-size: 12px;
  color: #999;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 对话框样式 */
:deep(.book-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.book-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #1A98AA 0%, #16849a 100%);
  padding: 24px 30px;
  margin: 0;
}

:deep(.book-dialog .el-dialog__title) {
  color: white;
  font-size: 22px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

:deep(.book-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: white;
  font-size: 20px;
}

:deep(.book-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
  color: white;
}

:deep(.book-dialog .el-dialog__body) {
  padding: 30px;
  background: #fafafa;
}

:deep(.book-dialog .el-dialog__footer) {
  padding: 20px 30px;
  background: white;
  border-top: 1px solid #e8e8e8;
}

.dialog-content {
  display: flex;
  gap: 30px;
}

.dialog-cover {
  position: relative;
  flex-shrink: 0;
  width: 180px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

.dialog-cover:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.dialog-cover img {
  width: 100%;
  display: block;
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.3) 0%, transparent 50%);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
}

.status-tag {
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.dialog-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.info-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateX(5px);
}

.info-label {
  font-size: 14px;
  font-weight: 600;
  color: #67c23a;
  min-width: 70px;
  position: relative;
  padding-left: 16px;
}

.info-label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border-radius: 2px;
}

.info-value {
  flex: 1;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.info-value.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 600;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.action-button {
  min-width: 120px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 25px;
  transition: all 0.3s ease;
}

.action-button .el-icon {
  margin-right: 6px;
}

.borrow-button {
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border: none;
  color: white;
}

.borrow-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.countdown-button {
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  color: white;
}

.countdown-button:disabled {
  opacity: 0.8;
}

.return-button {
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border: none;
}

.return-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.close-button {
  border-radius: 25px;
  font-weight: 500;
}

.close-button:hover {
  background: #f5f5f5;
  border-color: #dcdfe6;
}

/* 响应式 */
@media (max-width: 768px) {
  .dialog-content {
    flex-direction: column;
  }

  .dialog-cover {
    width: 100%;
    max-width: 200px;
    margin: 0 auto;
  }
}
</style>