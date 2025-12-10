import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo, login as loginApi, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref(null)
  const isLoggedIn = ref(false)
  
  // 计算属性
  const userName = computed(() => userInfo.value?.userName || '未登录')
  const userId = computed(() => userInfo.value?.id)
  const avatar = computed(() => userInfo.value?.avatar || 'default.jpg')
  const nickName = computed(() => userInfo.value?.nickName || '')
  
  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const data = await getUserInfo()
      userInfo.value = data
      isLoggedIn.value = true
      return data
    } catch (error) {
      userInfo.value = null
      isLoggedIn.value = false
      throw error
    }
  }
  
  // 登录
  async function login(credentials) {
    try {
      await loginApi(credentials)
      await fetchUserInfo()
    } catch (error) {
      throw error
    }
  }
  
  // 登出
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      userInfo.value = null
      isLoggedIn.value = false
    }
  }
  
  // 更新用户信息
  function updateUserInfo(data) {
    userInfo.value = { ...userInfo.value, ...data }
  }
  
  return {
    userInfo,
    isLoggedIn,
    userName,
    userId,
    avatar,
    nickName,
    fetchUserInfo,
    login,
    logout,
    updateUserInfo
  }
})