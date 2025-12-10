<template>
  <div class="profile-view">
    <div class="container">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>个人中心</span>
          </div>
        </template>
        
        <div class="profile-content">
          <!-- 用户信息区域 -->
          <div class="user-info">
            <div class="avatar-container">
              <el-avatar :size="100" :src="avatarUrl" />
              <el-upload
                class="avatar-uploader"
                :action="uploadAction"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :on-success="handleAvatarSuccess"
                :on-error="handleAvatarError"
                :headers="uploadHeaders"
                accept="image/png,image/jpeg"
              >
                <el-button size="small" type="primary" circle class="upload-btn">
                  <el-icon><Camera /></el-icon>
                </el-button>
              </el-upload>
            </div>
            <h2>{{ displayName }}</h2>
            <p class="user-id">ID: {{ userId }}</p>
            <p class="username">用户名: {{ userName }}</p>
          </div>
          
          <el-divider />
          
          <!-- 个人信息编辑区域 -->
          <div class="info-section">
            <h3>个人信息</h3>
            <el-form :model="profileForm" label-width="100px" class="profile-form">
              <el-form-item label="昵称">
                <div class="form-item-content">
                  <el-input 
                    v-model="profileForm.nickName" 
                    placeholder="请输入昵称（2-32个字符）"
                    :disabled="!editingNickname"
                    maxlength="32"
                  />
                  <el-button 
                    v-if="!editingNickname" 
                    type="primary" 
                    size="small"
                    @click="startEditNickname"
                  >
                    编辑
                  </el-button>
                  <div v-else class="button-group">
                    <el-button 
                      type="success" 
                      size="small"
                      @click="saveNickname"
                      :loading="savingNickname"
                    >
                      保存
                    </el-button>
                    <el-button 
                      size="small"
                      @click="cancelEditNickname"
                    >
                      取消
                    </el-button>
                  </div>
                </div>
              </el-form-item>
              
              <el-form-item label="修改密码">
                <el-button 
                  type="primary" 
                  size="small"
                  @click="showPasswordDialog = true"
                >
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <el-divider />
          
          <!-- 借阅统计 -->
          <div class="info-section">
            <h3>借阅统计</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="当前借阅">
                {{ borrowedBooks.size }} 本
              </el-descriptions-item>
              <el-descriptions-item label="历史借阅">
                暂无数据
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <el-divider />
          
          <!-- 当前借阅的图书 -->
          <div class="info-section">
            <h3>当前借阅的图书</h3>
            <el-empty v-if="borrowedBooks.size === 0" description="暂无借阅记录" />
            <div v-else class="borrowed-list">
              <el-tag
                v-for="bookId in Array.from(borrowedBooks)"
                :key="bookId"
                type="success"
                class="book-tag"
              >
                图书 ID: {{ bookId }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input 
            v-model="passwordForm.oldPassword" 
            type="password" 
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password" 
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closePasswordDialog">取消</el-button>
          <el-button type="primary" @click="savePassword" :loading="savingPassword">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { useBookStore } from '@/stores/book'
import { updateAvatar, updateNickname, updatePassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

const userStore = useUserStore()
const bookStore = useBookStore()

const userName = computed(() => userStore.userName)
const userId = computed(() => userStore.userId)
const avatarUrl = computed(() => {
  const avatar = userStore.avatar || 'default.jpg'
  return `/api/avatars/${avatar}?t=${Date.now()}`
})
const borrowedBooks = computed(() => bookStore.borrowedBooks)

// 显示名称：优先显示昵称，否则显示用户名
const displayName = computed(() => {
  return userStore.nickName || userStore.userName || '未设置昵称'
})

// 头像上传相关
const uploadAction = '/api/user/avatar'
const uploadHeaders = computed(() => {
  // 如果需要token，可以在这里添加
  return {}
})

const beforeAvatarUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const handleAvatarSuccess = async (response) => {
  if (response.success) {
    ElMessage.success('头像上传成功')
    // 更新用户信息
    await userStore.fetchUserInfo()
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const handleAvatarError = () => {
  ElMessage.error('头像上传失败，请重试')
}

// 昵称编辑相关
const profileForm = reactive({
  nickName: ''
})
const editingNickname = ref(false)
const savingNickname = ref(false)
const originalNickname = ref('')

const startEditNickname = () => {
  originalNickname.value = userStore.nickName || ''
  profileForm.nickName = originalNickname.value
  editingNickname.value = true
}

const cancelEditNickname = () => {
  profileForm.nickName = originalNickname.value
  editingNickname.value = false
}

const saveNickname = async () => {
  const nickName = profileForm.nickName.trim()
  
  if (!nickName) {
    ElMessage.warning('昵称不能为空')
    return
  }
  
  if (nickName.length < 2 || nickName.length > 32) {
    ElMessage.warning('昵称长度应在2到32个字符之间')
    return
  }
  
  if (nickName === originalNickname.value) {
    editingNickname.value = false
    return
  }
  
  savingNickname.value = true
  try {
    const response = await updateNickname(nickName)
    if (response.success) {
      ElMessage.success('昵称修改成功')
      // 更新用户信息
      await userStore.fetchUserInfo()
      editingNickname.value = false
    } else {
      ElMessage.error(response.message || '昵称修改失败')
    }
  } catch (error) {
    ElMessage.error('昵称修改失败，请重试')
  } finally {
    savingNickname.value = false
  }
}

// 密码修改相关
const showPasswordDialog = ref(false)
const savingPassword = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const closePasswordDialog = () => {
  showPasswordDialog.value = false
  passwordFormRef.value?.resetFields()
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const savePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    savingPassword.value = true
    try {
      const response = await updatePassword(
        passwordForm.oldPassword,
        passwordForm.newPassword
      )
      if (response.success) {
        ElMessage.success('密码修改成功')
        closePasswordDialog()
      } else {
        ElMessage.error(response.message || '密码修改失败')
      }
    } catch (error) {
      ElMessage.error('密码修改失败，请重试')
    } finally {
      savingPassword.value = false
    }
  })
}
</script>

<style scoped>
.profile-view {
  min-height: 100vh;
  background: #f5f5f5;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.profile-card {
  margin-top: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
}

.profile-content {
  padding: 20px 0;
}

.user-info {
  text-align: center;
  position: relative;
}

.avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.avatar-uploader {
  position: absolute;
  bottom: 0;
  right: 0;
}

.upload-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info h2 {
  margin: 16px 0 8px 0;
  font-size: 24px;
}

.user-id {
  color: #909399;
  font-size: 14px;
  margin: 4px 0;
}

.username {
  color: #606266;
  font-size: 14px;
  margin: 4px 0;
}

.info-section {
  margin: 20px 0;
}

.info-section h3 {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: bold;
}

.profile-form {
  max-width: 600px;
}

.form-item-content {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.form-item-content .el-input {
  flex: 1;
}

.button-group {
  display: flex;
  gap: 8px;
}

.borrowed-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.book-tag {
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-upload) {
  display: inline-block;
}
</style>