<template>
  <div id="app">
    <!-- 导航栏 - 在登录/注册页面隐藏 -->
    <NavBar v-if="!hideNavBar" />
    
    <!-- 主内容区域 - 使用keep-alive缓存组件 -->
    <div :class="{ 'main-content': !hideNavBar }">
      <RouterView v-slot="{ Component, route }">
        <Transition :name="route.meta.transition || 'fade'" mode="out-in">
          <keep-alive :include="['HomeView', 'BooksView', 'CategoriesView', 'ProfileView', 'DonateView', 'AiAssistantView']">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </Transition>
      </RouterView>
    </div>
    
    <!-- 浮动快捷操作组件 - 在登录/注册页面隐藏 -->
    <FloatingActions v-if="!hideNavBar" />
  </div>
</template>

<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'
import NavBar from '@/components/NavBar.vue'
import FloatingActions from '@/components/FloatingActions.vue'

const route = useRoute()

// 判断是否隐藏导航栏（登录/注册页面）
const hideNavBar = computed(() => {
  return route.meta.hideNavBar === true
})

// 移除 onMounted 中的用户信息获取，由路由守卫统一处理
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: auto;
  overflow-x: hidden;
  overflow-y: auto;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
  background-color: #f5f5f5;
  overflow-x: hidden;
}

.main-content {
  padding-top: 100px;
}

/* 路由过渡动画 - 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 路由过渡动画 - 滑动效果 */
.slide-left-enter-active,
.slide-left-leave-active {
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

/* 路由过渡动画 - 右滑效果 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0, 0.1, 1);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 路由过渡动画 - 缩放效果 */
.scale-enter-active,
.scale-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0, 0.1, 1);
}

.scale-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.scale-leave-to {
  opacity: 0;
  transform: scale(1.1);
}
</style>
