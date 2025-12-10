<template>
  <div class="ai-assistant-view">
    <div class="container">
      <div class="chat-container">
        <div class="chat-header">
          <h1>AI 图书助手</h1>
          <p>基于 DeepSeek API 的智能对话助手</p>
        </div>
        
        <div class="chat-messages" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.role === 'user'" :size="40">
                {{ userName.charAt(0) }}
              </el-avatar>
              <img v-else src="/img/ai-avatar.png" alt="AI" class="ai-avatar-img" />
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
              <img src="/img/ai-avatar.png" alt="AI" class="ai-avatar-img" />
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
            :rows="3"
            placeholder="请输入您的问题... (Shift+Enter换行)"
            @keydown.enter="handleKeyDown"
          />
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!inputMessage.trim()"
            @click="handleSend"
          >
            <el-icon v-if="!loading"><Position /></el-icon>
            {{ loading ? '发送中...' : '发送' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Position } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { marked } from 'marked'

// 配置 marked
marked.setOptions({
  breaks: true, // 支持换行
  gfm: true, // 启用 GitHub 风格的 Markdown
})

const userStore = useUserStore()
const userName = computed(() => userStore.userName || '用户')
const userId = computed(() => userStore.userId)

// 从 localStorage 加载当前用户的聊天记录
const loadMessages = () => {
  const currentUserId = userId.value
  if (!currentUserId) return []
  
  const storageKey = `ai_chat_messages_${currentUserId}`
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
      content: '您好！我是AI图书助手，可以帮您推荐图书、解答阅读问题。请问有什么可以帮助您的吗？',
      time: new Date().toLocaleTimeString()
    }
  ]
}

// 保存聊天记录到 localStorage
const saveMessages = () => {
  const currentUserId = userId.value
  if (!currentUserId) return
  
  const storageKey = `ai_chat_messages_${currentUserId}`
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
const typingMessage = ref(null) // 正在打字的消息

// 生成会话ID（包含用户ID）
onMounted(() => {
  const currentUserId = userId.value || 'guest'
  sessionId.value = `session_${currentUserId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
})

// 监听用户变化，重新加载聊天记录
watch(userId, (newUserId, oldUserId) => {
  if (newUserId !== oldUserId) {
    messages.value = loadMessages()
    const currentUserId = newUserId || 'guest'
    sessionId.value = `session_${currentUserId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
})

// 监听消息变化，自动保存
watch(messages, () => {
  saveMessages()
}, { deep: true })

// 渲染 Markdown
const renderMarkdown = (content) => {
  try {
    return marked.parse(content)
  } catch (error) {
    console.error('Markdown 解析错误:', error)
    return content
  }
}

// 打字机效果
const typeWriter = async (fullText, messageIndex) => {
  const message = messages.value[messageIndex]
  if (!message) return
  
  const speed = 30 // 每个字符的延迟时间（毫秒）
  let currentIndex = 0
  
  // 清空内容
  message.content = ''
  
  // 逐字添加
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
    // 调用后端AI接口，设置60秒超时
    const result = await request.post('/ai/chat', {
      sessionId: sessionId.value,
      message: question
    }, {
      timeout: 60000 // 60秒超时，给AI足够的响应时间
    })
    
    // 收到响应后立即关闭loading，开始打字
    loading.value = false
    
    // 后端返回的数据结构：{ status: 'SUCCESS', sessionId: 'xxx', reply: 'xxx' }
    // 但响应拦截器会返回 res.data，如果没有data字段则返回undefined
    // 所以我们需要处理这种情况
    let replyContent = '抱歉，我没有理解您的问题。'
    
    if (result) {
      // 如果result存在，说明有data字段
      replyContent = result.reply || result.message || replyContent
      if (result.sessionId) {
        sessionId.value = result.sessionId
      }
    }
    
    // 创建一个空的AI消息
    const aiMessage = {
      role: 'assistant',
      content: '',
      time: new Date().toLocaleTimeString()
    }
    
    messages.value.push(aiMessage)
    const messageIndex = messages.value.length - 1
    typingMessage.value = messageIndex
    
    // 使用打字机效果显示回复
    await typeWriter(replyContent, messageIndex)
    saveMessages() // 保存AI回复
  } catch (error) {
    console.error('AI对话错误:', error)
    ElMessage.error('发送失败：' + (error.message || '未知错误'))
    
    // 如果失败，添加错误提示消息
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
</script>

<style scoped>
.ai-assistant-view {
  min-height: 100vh;
  background: #F5F5F5;
  padding: 20px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  height: calc(100vh - 100px);
}

.chat-container {
  height: 100%;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: linear-gradient(135deg, #1995A7 0%, #178a9c 100%);
  color: white;
  padding: 30px;
  text-align: center;
}

.chat-header h1 {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: 700;
}

.chat-header p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 30px;
  background: #f5f7fa;
}

.message {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
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

.ai-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: contain;
  background: white;
  padding: 4px;
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
  padding: 15px 20px;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  line-height: 1.6;
  word-wrap: break-word;
  position: relative;
}

/* 打字机光标效果 */
.message-text.typing::after {
  content: '|';
  animation: blink 1s infinite;
  margin-left: 2px;
  color: #1995A7;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

/* Markdown 样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-content :deep(h1) { font-size: 1.8em; }
.markdown-content :deep(h2) { font-size: 1.5em; }
.markdown-content :deep(h3) { font-size: 1.3em; }

.markdown-content :deep(p) {
  margin: 8px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  font-size: 0.95em;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #e8e8e8;
  padding: 8px 12px;
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

.markdown-content :deep(table tr:hover) {
  background: #f0f0f0;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #1995A7;
  padding-left: 16px;
  margin: 12px 0;
  color: #666;
  font-style: italic;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 16px 0;
}

.markdown-content :deep(a) {
  color: #1995A7;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: #333;
}

.markdown-content :deep(em) {
  font-style: italic;
}

.message.user .message-text {
  background: linear-gradient(135deg, #1995A7 0%, #178a9c 100%);
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  padding: 0 5px;
}

.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 15px 20px;
  background: white;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1995A7;
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
    transform: translateY(-10px);
    opacity: 1;
  }
}

.chat-input {
  padding: 20px 30px;
  background: white;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  resize: none;
}

.chat-input .el-button {
  height: 50px;
  padding: 0 30px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 768px) {
  .ai-assistant-view {
    padding: 10px;
  }
  
  .container {
    height: calc(100vh - 80px);
  }
  
  .chat-header {
    padding: 20px;
  }
  
  .chat-header h1 {
    font-size: 22px;
  }
  
  .chat-messages {
    padding: 20px 15px;
  }
  
  .message-content {
    max-width: 80%;
  }
  
  .chat-input {
    padding: 15px;
  }
}
</style>