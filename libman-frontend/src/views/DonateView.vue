<template>
  <div class="donate-view">
    <div class="container">
      <div class="page-header">
        <div class="header-icon">
          <el-icon><Present /></el-icon>
        </div>
        <h1 class="page-title">捐赠图书</h1>
        <p class="page-subtitle">分享知识，传递爱心</p>
      </div>

      <div class="donate-card">
        <el-form
          ref="formRef"
          :model="bookForm"
          :rules="rules"
          label-width="100px"
          class="donate-form"
          label-position="top"
        >
          <div class="form-section">
            <h3 class="section-title">
              <el-icon><Reading /></el-icon>
              基本信息
            </h3>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="书名" prop="bookName">
                  <el-input
                    v-model="bookForm.bookName"
                    placeholder="请输入书名"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><Reading /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              
              <el-col :span="12">
                <el-form-item label="作者" prop="author">
                  <el-input
                    v-model="bookForm.author"
                    placeholder="请输入作者"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><User /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="出版社" prop="publish">
                  <el-input
                    v-model="bookForm.publish"
                    placeholder="请输入出版社"
                    size="large"
                  >
                    <template #prefix>
                      <el-icon><OfficeBuilding /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              
              <el-col :span="12">
                <el-form-item label="定价" prop="price">
                  <el-input-number
                    v-model="bookForm.price"
                    :min="0"
                    :precision="2"
                    :step="0.1"
                    placeholder="请输入定价"
                    size="large"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section">
            <h3 class="section-title">
              <el-icon><Collection /></el-icon>
              分类与标签
            </h3>
            
            <el-form-item label="分类" prop="categoryIds">
              <el-select
                v-model="bookForm.categoryIds"
                multiple
                placeholder="请选择分类（可多选）"
                size="large"
                style="width: 100%"
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.categoryName || category.category_name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="标签" prop="tags">
              <el-input
                v-model="bookForm.tags"
                type="textarea"
                :rows="3"
                placeholder="请输入标签，用逗号分隔，例如：经典,畅销,推荐"
                size="large"
              />
            </el-form-item>
          </div>

          <div class="form-section">
            <h3 class="section-title">
              <el-icon><Picture /></el-icon>
              图书封面
            </h3>
            
            <el-form-item label="封面图片" prop="coverImage">
              <el-upload
                class="cover-uploader"
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="handleUpload"
                accept="image/*"
              >
                <div v-if="bookForm.coverImage" class="cover-preview">
                  <img :src="bookForm.coverImage" alt="封面预览" />
                  <div class="cover-overlay">
                    <el-icon><Edit /></el-icon>
                    <span>更换图片</span>
                  </div>
                </div>
                <div v-else class="cover-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传封面</div>
                  <div class="upload-hint">支持 JPG、PNG 格式，建议尺寸 400x600</div>
                </div>
              </el-upload>
            </el-form-item>
          </div>
          
          <el-form-item class="submit-section">
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="submit-button"
              @click="handleSubmit"
            >
              <el-icon v-if="!loading"><Check /></el-icon>
              {{ loading ? '提交中...' : '提交捐赠' }}
            </el-button>
            <el-button
              size="large"
              class="reset-button"
              @click="handleReset"
            >
              <el-icon><RefreshLeft /></el-icon>
              重置表单
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 成功弹窗 -->
    <el-dialog
      v-model="successDialogVisible"
      width="500px"
      align-center
      :show-close="false"
      class="success-dialog"
    >
      <div class="success-content">
        <div class="success-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <h2 class="success-title">捐赠成功！</h2>
        <p class="success-message">感谢您的慷慨捐赠，您的爱心将帮助更多人获取知识</p>
        <div class="success-info">
          <div class="info-item">
            <span class="info-label">书名：</span>
            <span class="info-value">{{ submittedBook.bookName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">作者：</span>
            <span class="info-value">{{ submittedBook.author }}</span>
          </div>
        </div>
        <el-button
          type="primary"
          size="large"
          class="success-button"
          @click="handleSuccessClose"
        >
          继续捐赠
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Present, Reading, User, OfficeBuilding, Collection,
  Picture, Plus, Edit, Check, RefreshLeft, CircleCheck
} from '@element-plus/icons-vue'
import { addBookWithCover } from '@/api/book'
import { getCategoryList } from '@/api/category'

const formRef = ref(null)
const loading = ref(false)
const categories = ref([])
const successDialogVisible = ref(false)
const submittedBook = ref({})
const coverFile = ref(null) // 保存原始文件对象

const bookForm = reactive({
  bookName: '',
  author: '',
  publish: '',
  price: 0,
  categoryIds: [],
  tags: '',
  coverImage: '' // 用于预览的 base64
})

const rules = {
  bookName: [
    { required: true, message: '请输入书名', trigger: 'blur' }
  ],
  author: [
    { required: true, message: '请输入作者', trigger: 'blur' }
  ],
  publish: [
    { required: true, message: '请输入出版社', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入定价', trigger: 'blur' }
  ]
}

const loadCategories = async () => {
  try {
    const data = await getCategoryList()
    categories.value = data || []
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

// 图片上传前验证
const beforeUpload = (file) => {
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

// 处理图片上传（预览并保存文件）
const handleUpload = (options) => {
  const file = options.file
  
  // 保存原始文件对象，用于后续上传
  coverFile.value = file
  
  // 生成预览
  const reader = new FileReader()
  reader.onload = (e) => {
    bookForm.coverImage = e.target.result
    ElMessage.success('图片已选择')
  }
  reader.readAsDataURL(file)
  
  return false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      // 按照接口说明构建数据：只传必需字段
      const submitData = {
        bookName: bookForm.bookName,
        author: bookForm.author,
        publish: bookForm.publish,
        price: bookForm.price,
        categoryIds: JSON.stringify(bookForm.categoryIds) // JSON 数组字符串 "[1,2]"
      }
      
      // 可选字段：只在有值时添加
      if (bookForm.tags && bookForm.tags.trim()) {
        submitData.tags = bookForm.tags
      }
      
      console.log('提交数据:', submitData)
      console.log('封面文件:', coverFile.value)
      
      // 使用带封面上传的接口
      const result = await addBookWithCover(submitData, coverFile.value)
      
      console.log('捐赠成功:', result)
      
      // 保存提交的图书信息用于成功弹窗显示
      submittedBook.value = { ...bookForm }
      
      // 显示成功弹窗
      successDialogVisible.value = true
    } catch (error) {
      console.error('捐赠失败:', error)
      ElMessage.error(error.message || error.response?.data?.message || '捐赠失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const handleReset = () => {
  formRef.value?.resetFields()
  bookForm.coverImage = ''
  coverFile.value = null
}

const handleSuccessClose = () => {
  successDialogVisible.value = false
  handleReset()
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.donate-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #1A98AA 0%, #16849a 100%);
  border-radius: 50%;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(26, 152, 170, 0.3);
}

.header-icon .el-icon {
  font-size: 40px;
  color: white;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #333;
  margin: 0 0 10px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 表单卡片 */
.donate-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.donate-form {
  padding: 0;
}

/* 表单分区 */
.form-section {
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 2px dashed #e8e8e8;
}

.form-section:last-of-type {
  border-bottom: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 25px 0;
}

.section-title .el-icon {
  font-size: 24px;
  color: #67c23a;
}

/* 表单项样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
}

:deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 图片上传 */
.cover-uploader {
  width: 100%;
}

.cover-preview,
.cover-placeholder {
  width: 200px;
  height: 280px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cover-preview {
  position: relative;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-overlay .el-icon {
  font-size: 32px;
}

.cover-placeholder {
  border: 2px dashed #dcdfe6;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.cover-placeholder:hover {
  border-color: #67c23a;
  background: #f0f9ff;
}

.upload-icon {
  font-size: 48px;
  color: #67c23a;
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.upload-hint {
  font-size: 12px;
  color: #999;
  text-align: center;
  padding: 0 20px;
}

/* 提交按钮区域 */
.submit-section {
  margin-top: 40px;
  text-align: center;
}

.submit-button,
.reset-button {
  min-width: 160px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
}

.submit-button {
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border: none;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.reset-button:hover {
  background: #f5f5f5;
}

/* 成功弹窗 */
:deep(.success-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.success-dialog .el-dialog__body) {
  padding: 0;
}

.success-content {
  padding: 50px 40px;
  text-align: center;
}

.success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border-radius: 50%;
  margin-bottom: 30px;
  animation: successPulse 0.6s ease;
}

@keyframes successPulse {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.success-icon .el-icon {
  font-size: 60px;
  color: white;
}

.success-title {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 15px 0;
}

.success-message {
  font-size: 16px;
  color: #666;
  margin: 0 0 30px 0;
  line-height: 1.6;
}

.success-info {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  text-align: left;
}

.success-info .info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.success-info .info-label {
  font-weight: 600;
  color: #67c23a;
  min-width: 80px;
}

.success-info .info-value {
  flex: 1;
  color: #333;
  font-weight: 500;
}

.success-button {
  min-width: 180px;
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 25px;
  background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
  border: none;
}

.success-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

/* 响应式 */
@media (max-width: 768px) {
  .container {
    padding: 20px 15px;
  }

  .donate-card {
    padding: 25px 20px;
  }

  .page-title {
    font-size: 28px;
  }

  .section-title {
    font-size: 18px;
  }

  .cover-preview,
  .cover-placeholder {
    width: 160px;
    height: 224px;
  }

  .success-content {
    padding: 40px 25px;
  }

  .success-title {
    font-size: 24px;
  }
}
</style>