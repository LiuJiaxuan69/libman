<template>
  <div class="navbar-container" :class="{ 'navbar-hidden': isHidden }">
    <el-menu
      :default-active="activeIndex"
      class="navbar"
      mode="horizontal"
      :ellipsis="false"
      @select="handleSelect"
    >
      <el-menu-item index="logo" class="logo" disabled>
        <img src="/img/logo.png" alt="Logo" class="logo-image" />
        <span>图书管理系统</span>
      </el-menu-item>
      
      <div class="flex-grow" />
      
      <el-menu-item index="/">首页</el-menu-item>
      <el-menu-item index="/books">图书</el-menu-item>
      <el-menu-item index="/categories">分类</el-menu-item>
      <el-menu-item index="/donate">捐赠</el-menu-item>
      <el-menu-item index="/ai">AI助手</el-menu-item>
      <el-menu-item index="/my-donations">我的捐赠</el-menu-item>
      
      <el-sub-menu index="user" class="user-menu">
        <template #title>
          <el-avatar :size="32" :src="avatarUrl" />
          <span class="username">{{ userName }}</span>
        </template>
        <el-menu-item v-if="!isLoggedIn" index="/login">登录</el-menu-item>
        <el-menu-item v-if="!isLoggedIn" index="/register">注册</el-menu-item>
        <el-menu-item v-if="isLoggedIn" index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="isLoggedIn" @click="handleLogout">退出登录</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeIndex = computed(() => route.path)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userName = computed(() => userStore.userName)
const avatarUrl = computed(() => `/api/avatars/${userStore.avatar}?t=${Date.now()}`)

const isHidden = ref(false)
let lastScrollY = 0

const handleScroll = () => {
  const currentScrollY = window.scrollY
  
  // 向下滚动超过100px时隐藏
  if (currentScrollY > 100 && currentScrollY > lastScrollY) {
    isHidden.value = true
  }
  // 向上滚动时显示
  else if (currentScrollY < lastScrollY) {
    isHidden.value = false
  }
  
  lastScrollY = currentScrollY
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const handleSelect = (index) => {
  if (index !== 'logo' && index !== 'user') {
    router.push(index)
  }
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('退出登录成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error('退出登录失败')
  }
}
</script>

<style scoped>
.navbar-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 1400px;
  z-index: 1000;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.navbar-container.navbar-hidden {
  transform: translate(-50%, -120%);
  opacity: 0;
}

.navbar {
  box-shadow: 0 8px 32px rgba(26, 152, 170, 0.3);
  background: linear-gradient(135deg, #1A98AA 0%, #178a9c 100%);
  border-bottom: none;
  position: relative;
  overflow: hidden;
  border-radius: 50px;
  backdrop-filter: blur(10px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0 20px;
}


.logo {
  font-weight: bold;
  font-size: 18px;
  cursor: default !important;
  color: #ffffff !important;
  transition: all 0.3s ease;
}

.logo-image {
  height: 32px;
  width: auto;
  margin-right: 8px;
  transition: transform 0.3s ease;
  object-fit: contain;
}

.logo:hover .logo-image {
  transform: rotate(15deg) scale(1.1);
}

.flex-grow {
  flex-grow: 1;
}

.user-menu {
  margin-left: auto;
}

.username {
  margin-left: 8px;
  color: #ffffff;
  font-weight: 500;
}

/* 菜单项样式 */
:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  border-bottom: 3px solid transparent;
}

:deep(.el-menu-item:not(.is-disabled):hover) {
  background-color: rgba(255, 255, 255, 0.15) !important;
  color: #ffffff !important;
  box-shadow: inset 0 -3px 0 0 rgba(255, 255, 255, 0.5);
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  border-bottom-color: #ffffff;
}

:deep(.el-menu-item.is-disabled) {
  opacity: 1 !important;
  cursor: default !important;
  background-color: transparent !important;
}

:deep(.el-menu-item.is-disabled:hover) {
  box-shadow: none !important;
}

/* 子菜单样式 */
:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.9) !important;
  transition: all 0.3s ease;
}

:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.15) !important;
  color: #ffffff !important;
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #ffffff !important;
}

/* 头像样式 */
:deep(.el-avatar) {
  border: 2px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

:deep(.el-sub-menu__title:hover .el-avatar) {
  border-color: #ffffff;
  transform: scale(1.05);
  box-shadow: 0 0 15px rgba(255, 255, 255, 0.5);
}

/* 下拉菜单样式 */
:deep(.el-menu--popup) {
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(102, 126, 234, 0.2);
}

:deep(.el-menu--popup .el-menu-item) {
  color: #333 !important;
  transition: all 0.3s ease;
}

:deep(.el-menu--popup .el-menu-item:hover) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #667eea !important;
  padding-left: 25px;
}

/* 整体菜单背景 */
:deep(.el-menu--horizontal) {
  border-bottom: none;
}

/* 添加发光效果 */
.navbar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.5),
    transparent
  );
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>