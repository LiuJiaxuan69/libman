<template>
  <div>
    <div class="floating-actions">
      <el-tooltip content="公告" placement="left">
        <div class="action-item" @click="showAnnouncement">
          <el-icon><Bell /></el-icon>
        </div>
      </el-tooltip>
      
      <el-tooltip content="个人中心" placement="left">
        <div class="action-item" @click="goToProfile">
          <el-icon><User /></el-icon>
        </div>
      </el-tooltip>
      
      <el-tooltip content="AI助手" placement="left">
        <div class="action-item" @click="showChatWindow">
          <el-icon><Monitor /></el-icon>
        </div>
      </el-tooltip>
      
      <el-tooltip content="留言反馈" placement="left">
        <div class="action-item" @click="showFeedback">
          <el-icon><ChatDotRound /></el-icon>
        </div>
      </el-tooltip>
      
      <el-tooltip content="返回顶部" placement="left">
        <div class="action-item" @click="scrollToTop">
          <el-icon><CaretTop /></el-icon>
        </div>
      </el-tooltip>
    </div>

    <!-- 公告对话框 -->
    <el-dialog v-model="announcementVisible" title="系统公告" width="500px">
      <div class="announcement-content">
        <!-- <div class="announcement-item"> -->
          <!-- <h4>📢 图书馆开放时间调整通知</h4>
          <p class="date">2025-12-10</p>
          <p>尊敬的读者：为了更好地服务广大读者，图书馆开放时间调整如下：</p>
          <p><strong>周一至周五：</strong> 8:00 - 22:00</p>
          <p><strong>周六至周日：</strong> 9:00 - 21:00</p>
          <p><strong>法定节假日：</strong> 10:00 - 18:00</p> -->
        <!-- </div> -->
        <div class="announcement-item">
          <h4>📚 新书上架通知</h4>
          <p class="date">2025-12-08</p>
          <p>本月新增图书500余册，涵盖文学、科技、历史等多个领域，欢迎广大读者前来借阅！</p>
        </div>
        <div class="announcement-item">
          <h4>🎉 读书活动预告</h4>
          <p class="date">2025-12-05</p>
          <p>本月将举办"阅读分享会"活动，诚邀各位读者参与交流，共享阅读乐趣。</p>
        </div>
      </div>
    </el-dialog>

    <!-- 留言反馈对话框 -->
    <el-dialog v-model="feedbackVisible" title="留言反馈" width="500px">
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="反馈内容">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入您的意见或建议"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" :max="5" show-text />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>

    <!-- AI聊天窗口 -->
    <ChatWindow :visible="chatWindowVisible" @update:visible="chatWindowVisible = $event" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, User, Monitor, ChatDotRound, CaretTop } from '@element-plus/icons-vue'
import ChatWindow from './ChatWindow.vue'
import { submitFeedback as submitFeedbackApi } from '@/api/user'

const router = useRouter()

// 对话框显示状态
const announcementVisible = ref(false)
const feedbackVisible = ref(false)
const chatWindowVisible = ref(false)

// 反馈表单
const feedbackForm = ref({
  content: '',
  rating: 0
})

// 显示公告
const showAnnouncement = () => {
  announcementVisible.value = true
}

// 跳转到个人中心
const goToProfile = () => {
  router.push('/profile')
}

// 显示AI聊天窗口
const showChatWindow = () => {
  console.log('打开聊天窗口')
  chatWindowVisible.value = true
  console.log('chatWindowVisible:', chatWindowVisible.value)
}

// 显示留言反馈
const showFeedback = () => {
  feedbackVisible.value = true
}

// 提交反馈
const submitFeedback = async () => {
  if (!feedbackForm.value.content.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  
  try {
    const rating = feedbackForm.value.rating > 0 ? feedbackForm.value.rating : null
    await submitFeedbackApi(feedbackForm.value.content, rating)
    ElMessage.success('感谢您的反馈！我们会尽快处理')
    feedbackVisible.value = false
    feedbackForm.value = { content: '', rating: 0 }
  } catch (error) {
    console.error('提交反馈失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  }
}

// 返回顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}
</script>

<style scoped>
.floating-actions {
  position: fixed;
  right: 30px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px 10px;
  background: white;
  border-radius: 50px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 2px solid #e0e0e0;
}

.action-item {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #1A98AA;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  font-size: 24px;
}

.action-item:hover {
  background: #178a9c;
  transform: scale(1.1);
  box-shadow: 0 4px 15px rgba(26, 152, 170, 0.4);
}

.action-item:active {
  transform: scale(0.95);
}

.announcement-content {
  max-height: 500px;
  overflow-y: auto;
}

.announcement-item {
  padding: 20px;
  margin-bottom: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #1A98AA;
}

.announcement-item:last-child {
  margin-bottom: 0;
}

.announcement-item h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #2c3e50;
  font-weight: 600;
}

.announcement-item .date {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.announcement-item p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.8;
  color: #666;
}

.announcement-item p strong {
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .floating-actions {
    right: 15px;
    padding: 10px 5px;
    gap: 10px;
  }

  .action-item {
    width: 45px;
    height: 45px;
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .floating-actions {
    right: 10px;
    padding: 8px 4px;
    gap: 8px;
  }

  .action-item {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}
</style>