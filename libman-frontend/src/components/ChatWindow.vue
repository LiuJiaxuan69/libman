<template>
  <transition name="chat-window">
    <div
      v-if="visible"
      class="chat-window"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
    >
      <div
        class="chat-header"
        @mousedown="startDrag"
      >
        <div class="header-content">
          <img src="/img/ai-avatar.png" alt="AI" class="ai-avatar" />
          <div class="header-text">
            <h3>AI 图书助手</h3>
            <p>在线咨询</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button
            text
            circle
            @click="close"
            style="color: white;"
            size="small"
          >
            <el-icon :size="16"><Close /></el-icon>
          </el-button>
        </div>
      </div>
      
      <div class="chat-messages" ref="messagesContainer">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message"
          :class="message.role"
        >
          <div class="message-avatar">
            <el-avatar v-if="message.role === 'user'" :size="32">
              {{ userName.charAt(0) }}
            </el-avatar>
            <img v-else src="/img/ai-avatar.png" alt="AI" class="ai-avatar-small" />
          </div>
          <div class="message-content">
            <div 
              class="message-text"
              :class="{ 
                'markdown-content': message.role === 'assistant',
                'typing': typingMessage === index
              }"
              v-html="message.role === 'assistant' ? renderMarkdown(message.content) : message.content"
            ></div>
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>
        
        <div v-if="loading && !typingMessage" class="message assistant">
          <div class="message-avatar">
            <img src="/img/ai-avatar.png" alt="AI" class="ai-avatar-small" />
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题... (Shift+Enter换行)"
          @keydown.enter="handleKeyDown"
        />
        <el-button
          type="primary"
          :loading="loading"
          :disabled="!inputMessage.trim()"
          @click="handleSend"
          circle
        >
          <el-icon><Position /></el-icon>
        </el-button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Position, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { marked } from 'marked'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const userStore = useUserStore()
const userName = computed(() => userStore.userName || '用户')
const userId = computed(() => userStore.userId)

// 从 localStorage 加载当前用户的聊天记录
const loadMessages = () => {
  const currentUserId = userId.value
  if (!currentUserId) return []
  
  const storageKey = `chat_messages_${currentUserId}`
  const stored = localStorage.getItem(storageKey)
  
  if (stored) {
    try {
      return JSON.parse(stored)
    } catch (e) {
      console.error('加载聊天记录失败:', e)
    }
  }
  
  // 默认欢迎消息
  return [
    {
      role: 'assistant',
      content: '您好！我是AI图书助手，有什么可以帮助您的吗？',
      time: new Date().toLocaleTimeString()
    }
  ]
}

// 保存聊天记录到 localStorage
const saveMessages = () => {
  const currentUserId = userId.value
  if (!currentUserId) return
  
  const storageKey = `chat_messages_${currentUserId}`
  try {
    localStorage.setItem(storageKey, JSON.stringify(messages.value))
  } catch (e) {
    console.error('保存聊天记录失败:', e)
  }
}

const messages = ref(loadMessages())

const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const sessionId = ref('')
const typingMessage = ref(null)
const position = ref({ x: 100, y: 100 })
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 生成会话ID（包含用户ID）
watch(() => props.visible, (newVal) => {
  if (newVal && !sessionId.value) {
    const currentUserId = userId.value || 'guest'
    sessionId.value = `session_${currentUserId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
})

// 监听用户变化，重新加载聊天记录
watch(userId, (newUserId, oldUserId) => {
  if (newUserId !== oldUserId) {
    messages.value = loadMessages()
    sessionId.value = '' // 重置会话ID
  }
})

// 监听消息变化，自动保存
watch(messages, () => {
  saveMessages()
}, { deep: true })

// 拖动功能
const startDrag = (e) => {
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - position.value.x,
    y: e.clientY - position.value.y
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (isDragging.value) {
    position.value = {
      x: Math.max(0, Math.min(e.clientX - dragOffset.value.x, window.innerWidth - 500)),
      y: Math.max(0, Math.min(e.clientY - dragOffset.value.y, window.innerHeight - 100))
    }
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onMounted(() => {
  // 初始化位置在右下角
  position.value = {
    x: window.innerWidth - 520,
    y: window.innerHeight - 650
  }
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})

const renderMarkdown = (content) => {
  try {
    return marked.parse(content)
  } catch (error) {
    console.error('Markdown 解析错误:', error)
    return content
  }
}

const typeWriter = async (fullText, messageIndex) => {
  const message = messages.value[messageIndex]
  if (!message) return
  
  const speed = 30
  let currentIndex = 0
  
  message.content = ''
  
  while (currentIndex < fullText.length) {
    message.content += fullText[currentIndex]
    currentIndex++
    scrollToBottom()
    await new Promise(resolve => setTimeout(resolve, speed))
  }
  
  typingMessage.value = null
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 处理键盘事件：Enter发送，Shift+Enter换行
const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
  // Shift+Enter 会自动换行，不需要额外处理
}

const handleSend = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  const userMessage = {
    role: 'user',
    content: inputMessage.value.trim(),
    time: new Date().toLocaleTimeString()
  }
  
  messages.value.push(userMessage)
  saveMessages() // 保存用户消息
  const question = inputMessage.value.trim()
  inputMessage.value = ''
  scrollToBottom()
  
  loading.value = true
  
  try {
    const result = await request.post('/ai/chat', {
      sessionId: sessionId.value,
      message: question
    }, {
      timeout: 60000
    })
    
    // 收到响应后立即关闭loading
    loading.value = false
    
    let replyContent = '抱歉，我没有理解您的问题。'
    
    if (result) {
      replyContent = result.reply || result.message || replyContent
      if (result.sessionId) {
        sessionId.value = result.sessionId
      }
    }
    
    const aiMessage = {
      role: 'assistant',
      content: '',
      time: new Date().toLocaleTimeString()
    }
    
    messages.value.push(aiMessage)
    const messageIndex = messages.value.length - 1
    typingMessage.value = messageIndex
    
    await typeWriter(replyContent, messageIndex)
    saveMessages() // 保存AI回复
  } catch (error) {
    console.error('AI对话错误:', error)
    ElMessage.error('发送失败：' + (error.message || '未知错误'))
    
    const errorMessage = {
      role: 'assistant',
      content: '抱歉，我暂时无法回复。请稍后再试。',
      time: new Date().toLocaleTimeString()
    }
    messages.value.push(errorMessage)
    saveMessages() // 保存错误消息
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

const close = () => {
  emit('update:visible', false)
}
</script>

<style scoped>
.chat-window {
  position: fixed;
  width: 500px;
  height: 600px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 2000;
  transition: height 0.3s ease;
}

.chat-window.minimized {
  height: 60px;
}

.chat-header {
  background: linear-gradient(135deg, #1A98AA 0%, #178a9c 100%);
  color: white;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: move;
  user-select: none;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: contain;
  background: white;
  padding: 4px;
}

.ai-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: contain;
  background: white;
  padding: 2px;
}

.header-text h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.header-text p {
  margin: 0;
  font-size: 11px;
  opacity: 0.9;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.message {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-text {
  background: white;
  padding: 8px 12px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  line-height: 1.5;
  word-wrap: break-word;
  font-size: 13px;
  position: relative;
}

.message-text.typing::after {
  content: '|';
  animation: blink 1s infinite;
  margin-left: 2px;
  color: #1A98AA;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message.user .message-text {
  background: linear-gradient(135deg, #1A98AA 0%, #178a9c 100%);
  color: white;
}

.message-time {
  font-size: 10px;
  color: #999;
  margin-top: 3px;
  padding: 0 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #1A98AA;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.chat-input {
  padding: 12px;
  background: white;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
  font-size: 13px;
}

.chat-input .el-button {
  width: 36px;
  height: 36px;
}

/* Markdown 样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin: 10px 0 5px 0;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-content :deep(h1) { font-size: 1.4em; }
.markdown-content :deep(h2) { font-size: 1.2em; }
.markdown-content :deep(h3) { font-size: 1.1em; }

.markdown-content :deep(p) {
  margin: 5px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 5px 0;
  padding-left: 18px;
}

.markdown-content :deep(li) {
  margin: 2px 0;
}

.markdown-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 0.85em;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #e8e8e8;
  padding: 5px 8px;
  text-align: left;
}

.markdown-content :deep(table th) {
  background: #D9D9D9;
  color: #333;
  font-weight: 600;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background: #f9f9f9;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid #1A98AA;
  padding-left: 10px;
  margin: 8px 0;
  color: #666;
  font-style: italic;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: #333;
}

/* 过渡动画 */
.chat-window-enter-active,
.chat-window-leave-active {
  transition: all 0.3s ease;
}

.chat-window-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.chat-window-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-window {
    width: 100%;
    height: 100%;
    border-radius: 0;
    left: 0 !important;
    top: 0 !important;
  }
}
</style>