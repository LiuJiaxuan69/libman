<template>
  <div class="box">
    <!-- 粒子背景 - 只在大屏幕显示 -->
    <div id="particles-js" class="particles-container"></div>
    <div class="content">
      <h1 class="system-title">图书管理系统</h1>
      <Transition :name="transitionName" mode="out-in">
        <div v-if="isLogin" key="login" class="auth-wrapper">
          <h1>登录</h1>
          <div class="auth-form">
            <div class="username form-item">
              <span>用户名</span>
              <input
                v-model="loginForm.userName"
                type="text"
                class="input-item"
                placeholder="请输入用户名"
                @keyup.enter="handleLogin"
                @input="clearError"
                @focus="onInputFocus"
                @blur="onInputBlur"
              >
            </div>
            <div class="password form-item">
              <span>密码</span>
              <div class="password-input-wrapper">
                <input
                  v-model="loginForm.password"
                  :type="showLoginPassword ? 'text' : 'password'"
                  class="input-item"
                  placeholder="请输入密码"
                  @keyup.enter="handleLogin"
                  @input="clearError"
                  @focus="onInputFocus"
                  @blur="onInputBlur"
                >
                <svg
                  class="eye-icon"
                  @click="showLoginPassword = !showLoginPassword"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path v-if="showLoginPassword" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle v-if="showLoginPassword" cx="12" cy="12" r="3"></circle>
                  <path v-if="!showLoginPassword" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line v-if="!showLoginPassword" x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </div>
              <!-- 错误提示 -->
              <Transition name="error-slide">
                <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
              </Transition>
            </div>
            <button class="auth-btn" @click="handleLogin" :disabled="loading">
              {{ loading ? '登录中...' : '登 录' }}
            </button>
          </div>
          <div class="switch-link">
            <span>还没有账号？</span>
            <a href="javascript:;" @click="switchToRegister">立即注册</a>
          </div>
        </div>

        <div v-else key="register" class="auth-wrapper">
          <h1>注册</h1>
          <div class="auth-form">
            <div class="username form-item">
              <span>用户名</span>
              <input
                v-model="registerForm.userName"
                type="text"
                class="input-item"
                placeholder="请输入用户名（3-20个字符）"
                @input="clearError"
                @focus="onInputFocus"
                @blur="onInputBlur"
              >
            </div>
            <div class="password form-item">
              <span>密码</span>
              <div class="password-input-wrapper">
                <input
                  v-model="registerForm.password"
                  :type="showRegisterPassword ? 'text' : 'password'"
                  class="input-item"
                  placeholder="请输入密码（至少3位）"
                  @input="clearError"
                  @focus="onInputFocus"
                  @blur="onInputBlur"
                >
                <svg
                  class="eye-icon"
                  @click="showRegisterPassword = !showRegisterPassword"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path v-if="showRegisterPassword" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle v-if="showRegisterPassword" cx="12" cy="12" r="3"></circle>
                  <path v-if="!showRegisterPassword" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line v-if="!showRegisterPassword" x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </div>
            </div>
            <div class="confirm-password form-item">
              <span>确认密码</span>
              <div class="password-input-wrapper">
                <input
                  v-model="registerForm.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="input-item"
                  placeholder="请再次输入密码"
                  @keyup.enter="handleRegister"
                  @input="clearError"
                  @focus="onInputFocus"
                  @blur="onInputBlur"
                >
                <svg
                  class="eye-icon"
                  @click="showConfirmPassword = !showConfirmPassword"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path v-if="showConfirmPassword" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle v-if="showConfirmPassword" cx="12" cy="12" r="3"></circle>
                  <path v-if="!showConfirmPassword" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line v-if="!showConfirmPassword" x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </div>
              <!-- 错误提示 -->
              <Transition name="error-slide">
                <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
              </Transition>
            </div>
            <button class="auth-btn" @click="handleRegister" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </div>
          <div class="switch-link">
            <span>已有账号？</span>
            <a href="javascript:;" @click="switchToLogin">立即登录</a>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/user'
import Swal from 'sweetalert2'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
// 默认显示登录，只有路由明确是 register 时才显示注册
const isLogin = ref(true)
const transitionName = ref('slide-left')

// 根据路由初始化状态
if (route.name === 'register') {
  isLogin.value = false
}

// 密码可见性控制
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)

// 提示消息
const errorMessage = ref('')

const loginForm = reactive({
  userName: '',
  password: ''
})

const registerForm = reactive({
  userName: '',
  password: '',
  confirmPassword: ''
})

// 清除错误提示
const clearError = () => {
  errorMessage.value = ''
}

// 显示错误提示
const showError = (message) => {
  errorMessage.value = message
}

// 显示成功提示
const showSuccess = (message) => {
  Swal.fire({
    icon: 'success',
    title: message,
    showConfirmButton: false,
    timer: 1500,
    customClass: {
      popup: 'custom-swal-popup',
      title: 'custom-swal-title'
    }
  })
}

const switchToRegister = () => {
  transitionName.value = 'slide-left'
  isLogin.value = false
  clearError()
}

const switchToLogin = () => {
  transitionName.value = 'slide-right'
  isLogin.value = true
  clearError()
}

const handleLogin = async () => {
  clearError()
  
  if (!loginForm.userName.trim()) {
    showError('请输入用户名')
    return
  }
  if (!loginForm.password.trim()) {
    showError('请输入密码')
    return
  }

  loading.value = true
  try {
    await userStore.login({
      userName: loginForm.userName,
      password: loginForm.password
    })
    showSuccess('登录成功！')
    setTimeout(() => {
      router.push('/')
    }, 1500)
  } catch (error) {
    console.error('登录失败:', error)
    showError(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  clearError()
  
  if (!registerForm.userName.trim()) {
    showError('请输入用户名')
    return
  }
  if (registerForm.userName.length < 3 || registerForm.userName.length > 20) {
    showError('用户名长度在3-20个字符')
    return
  }
  if (!registerForm.password.trim()) {
    showError('请输入密码')
    return
  }
  if (registerForm.password.length < 3) {
    showError('密码长度至少3位')
    return
  }
  if (!registerForm.confirmPassword.trim()) {
    showError('请确认密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    showError('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register({
      userName: registerForm.userName,
      password: registerForm.password
    })
    showSuccess('注册成功！')
    setTimeout(() => {
      switchToLogin()
      // 清空注册表单
      registerForm.userName = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''
    }, 1500)
  } catch (error) {
    console.error('注册失败:', error)
    showError(error.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 初始化时根据路由设置状态 - 已在上面处理

// 粒子效果初始化
let particlesInitialized = false

const loadParticlesScript = () => {
  return new Promise((resolve) => {
    if (window.particlesJS) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/particles.js@2.0.0/particles.min.js'
    script.onload = resolve
    document.head.appendChild(script)
  })
}

const initParticles = async () => {
  if (window.innerWidth > 768 && !particlesInitialized) {
    await loadParticlesScript()
    await nextTick()
    
    if (window.particlesJS) {
      try {
        window.particlesJS('particles-js', {
          particles: {
            number: { value: 60, density: { enable: true, value_area: 800 } },
            color: { value: '#3B4859' },
            shape: { type: 'circle' },
            opacity: {
              value: 0.3,
              random: true,
              anim: { enable: true, speed: 0.5, opacity_min: 0.1, sync: false }
            },
            size: {
              value: 2.5,
              random: true,
              anim: { enable: true, speed: 1.5, size_min: 0.3, sync: false }
            },
            line_linked: {
              enable: true,
              distance: 150,
              color: '#3B4859',
              opacity: 0.25,
              width: 1
            },
            move: {
              enable: true,
              speed: 1.5,
              direction: 'none',
              random: false,
              straight: false,
              out_mode: 'out',
              bounce: false
            }
          },
          interactivity: {
            detect_on: 'canvas',
            events: {
              onhover: { enable: true, mode: 'grab' },
              onclick: { enable: true, mode: 'push' },
              resize: true
            },
            modes: {
              grab: {
                distance: 180,
                line_linked: { opacity: 0.6 }
              },
              push: { particles_nb: 3 }
            }
          },
          retina_detect: true
        })
        particlesInitialized = true
        console.log('粒子效果已初始化 - 右侧50%区域，颜色#3B4859')
      } catch (error) {
        console.error('粒子初始化失败:', error)
      }
    }
  }
}

const handleResize = () => {
  if (window.innerWidth <= 768 && particlesInitialized) {
    // 小屏幕时清除粒子
    const canvas = document.querySelector('#particles-js canvas')
    if (canvas) {
      canvas.remove()
    }
    particlesInitialized = false
  } else if (window.innerWidth > 768 && !particlesInitialized) {
    // 大屏幕时初始化粒子
    initParticles()
  }
}

// 输入框聚焦效果 - 移除粒子变化，只保留CSS效果
const onInputFocus = () => {
  // 不修改粒子效果
}

const onInputBlur = () => {
  // 不修改粒子效果
}

onMounted(() => {
  initParticles()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
}

.box {
  width: 100vw;
  height: 100vh;
  background-color: #ffffff;
  position: relative;
  overflow: hidden;
}

/* 粒子容器 - 只在右侧50%显示 */
.particles-container {
  position: absolute;
  top: 0;
  right: 0;
  width: 50%;
  height: 100%;
  z-index: 1;
  display: none;
}

.particles-container canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 大屏幕显示粒子 */
@media (min-width: 769px) {
  .particles-container {
    display: block;
  }
}

/* 错误提示样式 */
.error-message {
  color: #ff4757;
  font-size: 14px;
  margin-top: 8px;
  padding-left: 20px;
  animation: errorShake 0.3s ease-out;
}

@keyframes errorShake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}

/* 错误提示动画 */
.error-slide-enter-active,
.error-slide-leave-active {
  transition: all 0.3s ease;
}

.error-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

.box .content .auth-wrapper h1 {
  text-align: center;
}

.box .content .auth-wrapper .auth-form .form-item {
  margin: 20px 0;
}

.box .content .auth-wrapper .auth-form .form-item span {
  display: block;
  margin: 5px 20px;
  font-weight: 100;
}

.box .content .auth-wrapper .auth-form .form-item .password-input-wrapper {
  position: relative;
  width: 100%;
}

.box .content .auth-wrapper .auth-form .form-item .input-item {
  width: 100%;
  border-radius: 40px;
  padding: 20px;
  box-sizing: border-box;
  font-size: 20px;
  font-weight: 200;
  transition: all 0.3s ease;
}

.box .content .auth-wrapper .auth-form .form-item .input-item:hover {
  border-color: rgb(100, 120, 140);
  box-shadow: 0 2px 8px rgba(59, 72, 89, 0.1);
}

.box .content .auth-wrapper .auth-form .form-item .input-item:focus {
  outline: none;
  border-color: rgb(59, 72, 89) !important;
  box-shadow: 0 0 0 3px rgba(59, 72, 89, 0.1), 0 4px 12px rgba(59, 72, 89, 0.15);
  transform: translateY(-2px);
}

.box .content .auth-wrapper .auth-form .form-item .eye-icon {
  position: absolute;
  right: 25px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  cursor: pointer;
  user-select: none;
  opacity: 0.5;
  transition: all 0.3s ease;
  color: rgb(81, 100, 115);
}

.box .content .auth-wrapper .auth-form .form-item .eye-icon:hover {
  opacity: 0.8;
  transform: translateY(-50%) scale(1.1);
}

.box .content .auth-wrapper .auth-form .auth-btn {
  width: 100%;
  border-radius: 40px;
  color: #fff;
  border: 0;
  font-weight: 100;
  margin-top: 10px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.box .content .auth-wrapper .auth-form .auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.box .content .auth-wrapper .switch-link {
  text-align: center;
  margin-top: 20px;
}

.box .content .auth-wrapper .switch-link a {
  color: rgb(59, 72, 89);
  text-decoration: none;
  margin-left: 5px;
  font-weight: bold;
  cursor: pointer;
}

.box .content .auth-wrapper .switch-link a:hover {
  text-decoration: underline;
}

/* 卡片切换动画 */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0, 0.1, 1);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 一般大于手机的尺寸CSS */
@media (min-width: 767px) {
  .box {
    background: url('@/assets/login/login_two.jpg') no-repeat center center;
    background-size: cover;
  }

  .box .content {
    width: 85vw;
    height: 90vh;
    position: absolute;
    right: 15%;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 20px;
    background-color: transparent;
    z-index: 10;
  }

  .box .content .system-title {
    position: absolute;
    left: 3%;
    top: 50%;
    transform: translateY(-50%);
    font-size: 60px;
    font-weight: bold;
    color: #ffffff;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
    letter-spacing: 8px;
    margin: 0;
    transition: all 0.3s ease;
    z-index: 20;
  }

  .box .content .auth-wrapper {
    width: 32vw;
    min-width: 420px;
    max-width: 550px;
    position: absolute;
    right: 2%;
    top: 50%;
    transform: translateY(-50%);
    transition: all 0.3s ease;
    z-index: 20;
    /* 毛玻璃效果 */
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 24px;
    padding: 40px 35px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1),
                0 2px 8px rgba(0, 0, 0, 0.05),
                inset 0 1px 1px rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(255, 255, 255, 0.3);
  }

  .box .content .auth-wrapper h1 {
    text-align: center;
    font-size: 45px;
    color: rgb(81, 100, 115);
    margin-bottom: 40px;
  }

  .box .content .auth-wrapper .auth-form {
    margin: 10px 0;
  }

  .box .content .auth-wrapper .auth-form .form-item span {
    color: rgb(81, 100, 115);
  }

  .box .content .auth-wrapper .auth-form .form-item .input-item {
    height: 60px;
    border: 1px solid rgb(214, 222, 228);
    transition: all 0.3s ease;
  }

  .box .content .auth-wrapper .auth-form .form-item .input-item:hover {
    border-color: rgb(100, 120, 140);
    box-shadow: 0 2px 8px rgba(59, 72, 89, 0.1);
  }

  .box .content .auth-wrapper .auth-form .form-item .input-item:focus {
    border-color: rgb(59, 72, 89) !important;
    box-shadow: 0 0 0 3px rgba(59, 72, 89, 0.1), 0 4px 12px rgba(59, 72, 89, 0.15);
    transform: translateY(-2px);
  }

  .box .content .auth-wrapper .auth-form .auth-btn {
    height: 50px;
    background-color: rgb(59, 72, 89);
    font-size: 20px;
  }

  .box .content .auth-wrapper .auth-form .auth-btn:hover {
    background-color: rgb(45, 58, 75);
  }

  .box .content .auth-wrapper .switch-link {
    color: rgb(81, 100, 115);
  }
}

/* 中等屏幕：标题移到卡片上方 */
@media (max-width: 1200px) {
  .box .content .system-title {
    left: 50%;
    top: 18%;
    transform: translate(-50%, -50%);
    font-size: 67px;
    letter-spacing: 6px;
    color: #ffffff !important;
    text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.6);
    white-space: nowrap;
  }

  .box .content .auth-wrapper {
    left: 50%;
    top: 60%;
    transform: translate(-50%, -50%);
    width: 50vw;
    min-width: 420px;
    max-width: 550px;
    /* 毛玻璃效果 */
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 24px;
    padding: 40px 35px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1),
                0 2px 8px rgba(0, 0, 0, 0.05),
                inset 0 1px 1px rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(255, 255, 255, 0.3);
  }
}

/* 手机端CSS */
@media (max-width: 768px) {
  .box .content {
    width: 100vw;
    height: 100vh;
    background: url('@/assets/login/login_bg_phone.png') no-repeat;
    background-size: 100% 100%;
    position: relative;
    overflow: visible;
  }

  .box .content .system-title {
    position: fixed;
    left: 50%;
    top: 95px;
    transform: translateX(-50%);
    font-size: 50px;
    letter-spacing: 4px;
    color: #ffffff !important;
    text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.7);
    z-index: 100;
    white-space: nowrap;
  }

  .box .content .auth-wrapper {
    width: 85%;
    max-width: 400px;
    left: 50%;
    top: 55%;
    transform: translate(-50%, -50%);
    position: absolute;
    /* 手机端毛玻璃效果 - 更透明 */
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(15px);
    -webkit-backdrop-filter: blur(15px);
    border-radius: 20px;
    padding: 30px 25px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .box .content .auth-wrapper h1 {
    font-size: 30px;
    color: #fff;
  }

  .box .content .auth-wrapper .auth-form .form-item {
    margin: 10px 0;
  }

  .box .content .auth-wrapper .auth-form .form-item span {
    color: rgb(113, 129, 141);
  }

  .box .content .auth-wrapper .auth-form .form-item .input-item {
    height: 40px;
    border: 1px solid rgb(113, 129, 141);
    background-color: transparent;
    color: #fff;
    padding: 10px 45px 10px 20px;
  }

  .box .content .auth-wrapper .auth-form .form-item .input-item::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }

  .box .content .auth-wrapper .auth-form .form-item .eye-icon {
    width: 18px;
    height: 18px;
    right: 15px;
    color: rgba(255, 255, 255, 0.7);
  }

  .box .content .auth-wrapper .auth-form .form-item .eye-icon:hover {
    color: rgba(255, 255, 255, 0.9);
  }

  .box .content .auth-wrapper .auth-form .auth-btn {
    height: 40px;
    background-color: rgb(235, 95, 93);
    font-size: 16px;
  }

  .box .content .auth-wrapper .switch-link {
    color: #fff;
  }

  .box .content .auth-wrapper .switch-link a {
    color: rgb(235, 95, 93);
  }

  .error-message {
    color: #ff6b81;
    font-size: 12px;
  }
}

/* SweetAlert2 自定义样式 */
:deep(.custom-swal-popup) {
  border-radius: 16px;
  padding: 30px;
}

:deep(.custom-swal-title) {
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

:deep(.swal2-icon.swal2-success) {
  border-color: #667eea;
}

:deep(.swal2-icon.swal2-success [class^='swal2-success-line']) {
  background-color: #667eea;
}

:deep(.swal2-icon.swal2-success .swal2-success-ring) {
  border-color: rgba(102, 126, 234, 0.3);
}

:deep(.swal2-timer-progress-bar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>