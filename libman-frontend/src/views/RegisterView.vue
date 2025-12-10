<template>
  <div class="box">
    <div class="content">
      <div class="register-wrapper">
        <h1>注册</h1>
        <div class="register-form">
          <div class="username form-item">
            <span>用户名</span>
            <input 
              v-model="formData.userName" 
              type="text" 
              class="input-item"
              placeholder="请输入用户名（3-20个字符）"
            >
          </div>
          <div class="password form-item">
            <span>密码</span>
            <input 
              v-model="formData.password" 
              type="password" 
              class="input-item"
              placeholder="请输入密码（至少3位）"
            >
          </div>
          <div class="confirm-password form-item">
            <span>确认密码</span>
            <input 
              v-model="formData.confirmPassword" 
              type="password" 
              class="input-item"
              placeholder="请再次输入密码"
              @keyup.enter="handleRegister"
            >
          </div>
          <button class="register-btn" @click="handleRegister" :disabled="loading">
            {{ loading ? '注册中...' : '注 册' }}
          </button>
        </div>
        <div class="login-link">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()

const loading = ref(false)
const formData = reactive({
  userName: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  // 验证表单
  if (!formData.userName.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (formData.userName.length < 3 || formData.userName.length > 20) {
    ElMessage.warning('用户名长度在3-20个字符')
    return
  }
  if (!formData.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }
  if (formData.password.length < 3) {
    ElMessage.warning('密码长度至少3位')
    return
  }
  if (!formData.confirmPassword.trim()) {
    ElMessage.warning('请确认密码')
    return
  }
  if (formData.password !== formData.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register({
      userName: formData.userName,
      password: formData.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
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

.box .content .register-wrapper h1 {
  text-align: center;
}

.box .content .register-wrapper .register-form .form-item {
  margin: 20px 0;
}

.box .content .register-wrapper .register-form .form-item span {
  display: block;
  margin: 5px 20px;
  font-weight: 100;
}

.box .content .register-wrapper .register-form .form-item .input-item {
  width: 100%;
  border-radius: 40px;
  padding: 20px;
  box-sizing: border-box;
  font-size: 20px;
  font-weight: 200;
}

.box .content .register-wrapper .register-form .form-item .input-item:focus {
  outline: none;
}

.box .content .register-wrapper .register-form .register-btn {
  width: 100%;
  border-radius: 40px;
  color: #fff;
  border: 0;
  font-weight: 100;
  margin-top: 10px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.box .content .register-wrapper .register-form .register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.box .content .register-wrapper .login-link {
  text-align: center;
  margin-top: 20px;
}

.box .content .register-wrapper .login-link a {
  color: rgb(59, 72, 89);
  text-decoration: none;
  margin-left: 5px;
  font-weight: bold;
}

.box .content .register-wrapper .login-link a:hover {
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

  .box .content .register-wrapper {
    width: 25vw;
    position: absolute;
    right: 15%;
    top: 50%;
    transform: translateY(-50%);
  }

  .box .content .register-wrapper h1 {
    text-align: center;
    font-size: 45px;
    color: rgb(81, 100, 115);
    margin-bottom: 40px;
  }

  .box .content .register-wrapper .register-form {
    margin: 10px 0;
  }

  .box .content .register-wrapper .register-form .form-item span {
    color: rgb(81, 100, 115);
  }

  .box .content .register-wrapper .register-form .form-item .input-item {
    height: 60px;
    border: 1px solid rgb(214, 222, 228);
  }

  .box .content .register-wrapper .register-form .register-btn {
    height: 50px;
    background-color: rgb(59, 72, 89);
    font-size: 20px;
  }

  .box .content .register-wrapper .register-form .register-btn:hover {
    background-color: rgb(45, 58, 75);
  }

  .box .content .register-wrapper .login-link {
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

  .box .content .register-wrapper {
    width: 70%;
    height: 70%;
    padding-top: 10%;
  }

  .box .content .register-wrapper h1 {
    font-size: 30px;
    color: #fff;
  }

  .box .content .register-wrapper .register-form .form-item {
    margin: 10px 0;
  }

  .box .content .register-wrapper .register-form .form-item span {
    color: rgb(113, 129, 141);
  }

  .box .content .register-wrapper .register-form .form-item .input-item {
    height: 30px;
    border: 1px solid rgb(113, 129, 141);
    background-color: transparent;
    color: #fff;
  }

  .box .content .register-wrapper .register-form .form-item .input-item::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }

  .box .content .register-wrapper .register-form .register-btn {
    height: 40px;
    background-color: rgb(235, 95, 93);
    font-size: 16px;
  }

  .box .content .register-wrapper .login-link {
    color: #fff;
  }

  .box .content .register-wrapper .login-link a {
    color: rgb(235, 95, 93);
  }
}
</style>