<template>
  <div class="box">
    <div class="content">
      <div class="login-wrapper">
        <h1>登录</h1>
        <div class="login-form">
          <div class="username form-item">
            <span>用户名</span>
            <input 
              v-model="formData.userName" 
              type="text" 
              class="input-item"
              placeholder="请输入用户名"
              @keyup.enter="handleLogin"
            >
          </div>
          <div class="password form-item">
            <span>密码</span>
            <input 
              v-model="formData.password" 
              type="password" 
              class="input-item"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
          </div>
          <button class="login-btn" @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </div>
        <div class="register-link">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formData = reactive({
  userName: '',
  password: ''
})

const handleLogin = async () => {
  // 验证表单
  if (!formData.userName.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!formData.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    await userStore.login({
      userName: formData.userName,
      password: formData.password
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
}

.box {
  width: 100vw;
  height: 100vh;
  background-color: rgb(29, 67, 89);
}

.box .content .login-wrapper h1 {
  text-align: center;
}

.box .content .login-wrapper .login-form .form-item {
  margin: 20px 0;
}

.box .content .login-wrapper .login-form .form-item span {
  display: block;
  margin: 5px 20px;
  font-weight: 100;
}

.box .content .login-wrapper .login-form .form-item .input-item {
  width: 100%;
  border-radius: 40px;
  padding: 20px;
  box-sizing: border-box;
  font-size: 20px;
  font-weight: 200;
}

.box .content .login-wrapper .login-form .form-item .input-item:focus {
  outline: none;
}

.box .content .login-wrapper .login-form .login-btn {
  width: 100%;
  border-radius: 40px;
  color: #fff;
  border: 0;
  font-weight: 100;
  margin-top: 10px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.box .content .login-wrapper .login-form .login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.box .content .login-wrapper .register-link {
  text-align: center;
  margin-top: 20px;
}

.box .content .login-wrapper .register-link a {
  color: rgb(59, 72, 89);
  text-decoration: none;
  margin-left: 5px;
  font-weight: bold;
}

.box .content .login-wrapper .register-link a:hover {
  text-decoration: underline;
}

/* 一般大于手机的尺寸CSS */
@media (min-width: 767px) {
  .box {
    background-color: rgb(29, 67, 89);
  }

  .box .content {
    width: 85vw;
    height: 90vh;
    background: url('@/assets/login/login_two.jpg') no-repeat;
    background-size: 90% 100%;
    position: absolute;
    right: 15%;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 20px;
    background-color: #fff;
  }

  .box .content .login-wrapper {
    width: 25vw;
    position: absolute;
    right: 15%;
    top: 50%;
    transform: translateY(-50%);
  }

  .box .content .login-wrapper h1 {
    text-align: center;
    font-size: 45px;
    color: rgb(81, 100, 115);
    margin-bottom: 40px;
  }

  .box .content .login-wrapper .login-form {
    margin: 10px 0;
  }

  .box .content .login-wrapper .login-form .form-item span {
    color: rgb(81, 100, 115);
  }

  .box .content .login-wrapper .login-form .form-item .input-item {
    height: 60px;
    border: 1px solid rgb(214, 222, 228);
  }

  .box .content .login-wrapper .login-form .login-btn {
    height: 50px;
    background-color: rgb(59, 72, 89);
    font-size: 20px;
  }

  .box .content .login-wrapper .login-form .login-btn:hover {
    background-color: rgb(45, 58, 75);
  }

  .box .content .login-wrapper .register-link {
    color: rgb(81, 100, 115);
  }
}

/* 手机端CSS */
@media (max-width: 768px) {
  .box .content {
    width: 100vw;
    height: 100vh;
    background: url('@/assets/login/login_bg_phone.png') no-repeat;
    background-size: 100% 100%;
    display: flex;
    align-items: flex-start;
    justify-content: center;
  }

  .box .content .login-wrapper {
    width: 70%;
    height: 60%;
    padding-top: 15%;
  }

  .box .content .login-wrapper h1 {
    font-size: 30px;
    color: #fff;
  }

  .box .content .login-wrapper .login-form .form-item {
    margin: 10px 0;
  }

  .box .content .login-wrapper .login-form .form-item span {
    color: rgb(113, 129, 141);
  }

  .box .content .login-wrapper .login-form .form-item .input-item {
    height: 30px;
    border: 1px solid rgb(113, 129, 141);
    background-color: transparent;
    color: #fff;
  }

  .box .content .login-wrapper .login-form .form-item .input-item::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }

  .box .content .login-wrapper .login-form .login-btn {
    height: 40px;
    background-color: rgb(235, 95, 93);
    font-size: 16px;
  }

  .box .content .login-wrapper .register-link {
    color: #fff;
  }

  .box .content .login-wrapper .register-link a {
    color: rgb(235, 95, 93);
  }
}
</style>