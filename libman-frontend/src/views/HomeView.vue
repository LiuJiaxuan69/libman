<template>
  <div class="home-view">
    <!-- 轮播图 -->
    <div class="carousel-section">
      <el-carousel
        :interval="3000"
        height="500px"
        arrow="never"
        indicator-position="outside">
        <el-carousel-item v-for="item in carouselImages" :key="item.id">
          <img
            :src="item.src"
            :alt="item.alt"
            class="carousel-image"
            :class="{ 'clickable': item.videoUrl || item.link }"
            @click="handleCarouselClick(item)"
          />
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 全屏视频播放器 -->
    <div v-if="isVideoPlaying" class="fullscreen-video-container" @click="handleVideoContainerClick">
      <video
        ref="videoPlayer"
        :src="currentVideoUrl"
        controls
        autoplay
        class="fullscreen-video"
        @ended="closeVideo"
        @loadedmetadata="handleVideoLoaded"
        @error="handleVideoError"
      >
        您的浏览器不支持视频播放
      </video>
      <button class="close-video-btn" @click.stop="closeVideo">
        <el-icon><Close /></el-icon>
      </button>
    </div>

    <!-- 电子资源区域 -->
    <div class="resources-section">
      <h2 class="resources-title">电子资源</h2>
      <div class="resources-grid">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-card"
          @click="handleCategoryClick(category)"
        >
          <div class="category-icon">
            <img :src="category.icon" :alt="category.name" />
          </div>
          <div class="category-name">{{ category.name }}</div>
        </div>
      </div>
    </div>

    <!-- 精选推荐卡片区域 -->
    <div class="featured-section">
      <h2 class="featured-title">精选推荐</h2>
      <div class="featured-grid">
        <div
          v-for="card in accordionCards"
          :key="card.id"
          class="featured-card"
          @click="handleCardClick(card)"
        >
          <div class="card-image-wrapper">
            <img :src="card.image" :alt="card.title" class="card-image" />
            <div class="card-overlay"></div>
          </div>
          <div class="card-content">
            <h3 class="card-title">{{ card.title }}</h3>
            <p class="card-description">{{ card.description }}</p>
            <button class="card-button">
              <span>了解更多</span>
              <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 版权信息区域 -->
    <footer class="footer-section">
      <div class="footer-content">
        <div class="footer-copyright">
          <p>© 2025 图书管理系统 版权所有</p>
          <p class="footer-team">Developed by <strong>404NotFound</strong> Team</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/category'

const router = useRouter()

// 视频播放相关
const isVideoPlaying = ref(false)
const currentVideoUrl = ref('')
const videoPlayer = ref(null)

// 轮播图数据 - 请将图片放到 public/img 目录下，视频放到 public/videos 目录下
const carouselImages = ref([
  {
    id: 1,
    src: '/img/carousel1.jpg',
    alt: '轮播图1',
    videoUrl: '/videos/book-day.mp4' // 视频播放
  },
  {
    id: 2,
    src: '/img/carousel2.jpg',
    alt: '轮播图2',
    link: 'https://baike.baidu.com/item/%E4%B8%96%E7%95%8C%E8%AF%BB%E4%B9%A6%E6%97%A5/444842' // 外部链接
  },
  {
    id: 3,
    src: '/img/carousel3.jpg',
    alt: '轮播图3',
    link: 'https://weread.qq.com/web/reader/77f320d0813ab7d74g017e0ekc81322c012c81e728d9d180' // 外部链接
  },
  {
    id: 4,
    src: '/img/carousel4.jpg',
    alt: '轮播图4',
    link: 'https://www.luoxia123.com/pinang/438256.html' // 外部链接
  }
])

// 处理轮播图点击 - 视频播放或链接跳转
const handleCarouselClick = async (item) => {
  // 如果有外部链接，直接跳转
  if (item.link) {
    window.open(item.link, '_blank')
    return
  }
  
  // 如果有视频，播放视频
  if (item.videoUrl) {
    console.log('准备播放视频:', item.videoUrl)
    console.log('完整URL:', window.location.origin + item.videoUrl)
    
    // 先测试视频是否可访问
    try {
      const response = await fetch(item.videoUrl, { method: 'HEAD' })
      console.log('视频文件检查:', response.ok ? '存在' : '不存在', 'Status:', response.status)
      if (!response.ok) {
        ElMessage.error('视频文件不存在，请检查文件路径')
        return
      }
    } catch (error) {
      console.error('无法访问视频文件:', error)
      ElMessage.error('视频文件不存在，请检查文件路径')
      return
    }
    
    currentVideoUrl.value = item.videoUrl
    isVideoPlaying.value = true
    
    // 等待 DOM 更新后请求全屏
    await nextTick()
    
    if (videoPlayer.value) {
      console.log('视频元素已加载')
      
      // 尝试进入全屏模式（延迟一点，等视频开始播放）
      setTimeout(async () => {
        try {
          if (videoPlayer.value.requestFullscreen) {
            await videoPlayer.value.requestFullscreen()
          } else if (videoPlayer.value.webkitRequestFullscreen) {
            await videoPlayer.value.webkitRequestFullscreen()
          } else if (videoPlayer.value.mozRequestFullScreen) {
            await videoPlayer.value.mozRequestFullScreen()
          } else if (videoPlayer.value.msRequestFullscreen) {
            await videoPlayer.value.msRequestFullscreen()
          }
          console.log('已进入全屏模式')
        } catch (error) {
          console.log('全屏请求失败，使用伪全屏模式:', error)
        }
      }, 500)
      
      // 监听全屏退出事件
      document.addEventListener('fullscreenchange', handleFullscreenChange)
      document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
      document.addEventListener('mozfullscreenchange', handleFullscreenChange)
      document.addEventListener('MSFullscreenChange', handleFullscreenChange)
    }
  }
}

// 处理全屏状态变化
const handleFullscreenChange = () => {
  const isFullscreen = !!(
    document.fullscreenElement ||
    document.webkitFullscreenElement ||
    document.mozFullScreenElement ||
    document.msFullscreenElement
  )
  
  // 如果退出了全屏，关闭视频
  if (!isFullscreen && isVideoPlaying.value) {
    closeVideo()
  }
}

// 关闭视频
const closeVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
    videoPlayer.value.currentTime = 0
    
    // 如果在全屏模式，退出全屏
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else if (document.webkitFullscreenElement) {
      document.webkitExitFullscreen()
    } else if (document.mozFullScreenElement) {
      document.mozCancelFullScreen()
    } else if (document.msFullscreenElement) {
      document.msExitFullscreen()
    }
  }
  
  isVideoPlaying.value = false
  currentVideoUrl.value = ''
  
  // 移除全屏监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)
}

// 点击视频容器背景关闭视频
const handleVideoContainerClick = (e) => {
  if (e.target.classList.contains('fullscreen-video-container')) {
    closeVideo()
  }
}

// 视频元数据加载完成
const handleVideoLoaded = () => {
  console.log('视频元数据已加载，准备播放')
}

// 视频加载错误处理
const handleVideoError = (e) => {
  console.error('视频加载错误详情:', e)
  const video = videoPlayer.value
  if (video && video.error) {
    const errorMessages = {
      1: '视频加载被中止',
      2: '网络错误导致视频下载失败',
      3: '视频解码失败（可能是格式不支持）',
      4: '视频源不可用或格式不支持'
    }
    const errorMsg = errorMessages[video.error.code] || '未知错误'
    console.error('错误代码:', video.error.code, '错误信息:', errorMsg)
    ElMessage.error(`视频加载失败: ${errorMsg}\n请确保视频文件在 public/videos/ 目录下`)
  }
}

// 分类名称到图标的映射
const categoryIconMap = {
  'art': '/icons/art.png',
  'biology': '/icons/biology.png',
  'chemistry': '/icons/chemistry.png',
  'computer': '/icons/computer.png',
  'computer science': '/icons/computer.png',
  'history': '/icons/history.png',
  'literature': '/icons/literature.png',
  'mathematics': '/icons/mathematics.png',
  'physics': '/icons/physics.png'
}

// 电子资源分类数据 - 从后端动态加载
const categories = ref([])

// 根据分类名称获取图标
const getCategoryIcon = (categoryName) => {
  if (!categoryName) return '/icons/art.png' // 默认图标
  
  const name = categoryName.toLowerCase().trim()
  return categoryIconMap[name] || '/icons/art.png' // 如果找不到匹配的图标，使用默认图标
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const data = await getCategoryList()
    if (data && Array.isArray(data)) {
      // 为每个分类添加图标
      categories.value = data.map(category => ({
        ...category,
        name: category.categoryName || category.category_name || category.name || `分类${category.id}`,
        icon: getCategoryIcon(category.categoryName || category.category_name || category.name)
      }))
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    // 如果加载失败，使用空数组
    categories.value = []
  }
}

// 处理分类点击 - 跳转到分类页面
const handleCategoryClick = (category) => {
  // 跳转到分类页面，并传递分类ID作为查询参数
  router.push({
    name: 'categories',
    query: { categoryId: category.id, categoryName: category.name }
  })
}

// 组件挂载时加载分类
onMounted(() => {
  loadCategories()
})

// 精选推荐卡片数据 - 请将图片放到 public/img/accordion 目录下
const accordionCards = ref([
  {
    id: 1,
    title: '红楼梦',
    description: '《红楼梦》是一部具有世界影响力的人情小说、中国封建社会的百科全书、传统文化的集大成者......',
    image: '/img/accordion/card1.jpg',
    link: 'https://baike.baidu.com/item/%E7%BA%A2%E6%A5%BC%E6%A2%A6/15311'
  },
  {
    id: 2,
    title: '三国演义',
    description: '《三国演义》描写了从东汉末年到西晋初年之间近一百年的历史风云，反映了三国时代的政治军事斗争和各类人物的生活变迁......',
    image: '/img/accordion/card2.jpg',
    link: 'https://baike.baidu.com/item/%E4%B8%89%E5%9B%BD%E6%BC%94%E4%B9%89/5782'
  },
  {
    id: 3,
    title: '西游记',
    description: '《西游记》是中国古代最著名的小说，描写了孙悟空在大闹天宫期间的冒险经历，以及他与其他主要角色的互动......',
    image: '/img/accordion/card3.jpg',
    link: 'https://baike.baidu.com/item/%E8%A5%BF%E6%B8%B8%E8%AE%B0/5723'
  },
  {
    id: 4,
    title: '水浒传',
    description: '《水浒传》是中国四大名著之一，讲述了梁山好汉在宋江、李逵、鲁智深等人的带领下，反抗官府压迫，最终接受招安的故事......',
    image: '/img/accordion/card4.jpg',
    link: 'https://baike.baidu.com/item/%E6%B0%B4%E6%B5%92%E4%BC%A0/348'
  }
])

// 处理卡片点击事件
const handleCardClick = (card) => {
  if (card.link) {
    // 在新标签页打开链接
    window.open(card.link, '_blank')
  }
}
</script>

<style scoped>
.home-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 轮播图区域 */
.carousel-section {
  padding: 30px 40px 60px;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 560px;
}

.carousel-section :deep(.el-carousel) {
  width: 100%;
  max-width: 1300px;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.carousel-image.clickable {
  cursor: pointer;
}

.carousel-image.clickable:hover {
  transform: scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.carousel-section :deep(.el-carousel__item) {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 轮播图指示器样式 */
.carousel-section :deep(.el-carousel__indicators) {
  bottom: -40px;
}

.carousel-section :deep(.el-carousel__indicator) {
  padding: 8px 4px;
}

.carousel-section :deep(.el-carousel__button) {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #c0c4cc;
  opacity: 0.5;
  transition: all 0.3s;
}

.carousel-section :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  width: 12px;
  height: 12px;
  background-color: #1A98AA;
  opacity: 1;
}

/* 全屏视频播放器样式 */
.fullscreen-video-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.98);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.fullscreen-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  outline: none;
}

.close-video-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  color: white;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  z-index: 10000;
}

.close-video-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.8);
  transform: scale(1.1);
}

.close-video-btn:active {
  transform: scale(0.95);
}

/* 电子资源区域 */
.resources-section {
  padding: 60px 20px;
  background: #f5f5f5;
}

.resources-title {
  text-align: center;
  font-size: 36px;
  font-weight: 600;
  color: #333;
  margin-bottom: 50px;
  position: relative;
}

.resources-title::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 4px;
  background: linear-gradient(135deg, #1A98AA 0%, #409eff 100%);
  border-radius: 2px;
}

.resources-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
}

.category-card {
  background: #1A98AA;
  border-radius: 16px;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.category-card:hover {
  background: #178a9c;
  border-color: #ffffff;
  box-shadow: 0 8px 24px rgba(26, 152, 170, 0.3);
  transform: translateY(-5px);
}

.category-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.category-card:hover .category-icon img {
  transform: scale(1.1);
}

.category-name {
  font-size: 18px;
  font-weight: 500;
  color: #ffffff;
  text-align: center;
  transition: color 0.3s ease;
}

.category-card:hover .category-name {
  color: #ffffff;
}

/* 精选推荐卡片区域 */
.featured-section {
  padding: 60px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.featured-title {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 40px;
  position: relative;
  letter-spacing: 1px;
}

.featured-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 70px;
  height: 3px;
  background: linear-gradient(135deg, #1A98AA 0%, #409eff 100%);
  border-radius: 2px;
}

.featured-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
}

.featured-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.featured-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 50px rgba(26, 152, 170, 0.2);
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f0f0f0;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.featured-card:hover .card-image {
  transform: scale(1.08);
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, transparent 0%, rgba(26, 152, 170, 0.1) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.featured-card:hover .card-overlay {
  opacity: 1;
}

.card-content {
  padding: 20px 24px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 10px;
  transition: color 0.3s ease;
}

.featured-card:hover .card-title {
  color: #1A98AA;
}

.card-description {
  font-size: 14px;
  color: #6c757d;
  line-height: 1.6;
  margin-bottom: 18px;
  min-height: 42px;
}

.card-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #1A98AA 0%, #409eff 100%);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 3px 12px rgba(26, 152, 170, 0.3);
}

.card-button:hover {
  background: linear-gradient(135deg, #178a9c 0%, #3a8ee6 100%);
  transform: translateX(5px);
  box-shadow: 0 5px 18px rgba(26, 152, 170, 0.4);
}

.card-button .arrow-icon {
  width: 16px;
  height: 16px;
  transition: transform 0.3s ease;
}

.card-button:hover .arrow-icon {
  transform: translateX(5px);
}

/* 版权信息区域 */
.footer-section {
  background: #1A98AA;
  color: white;
  padding: 20px 20px;
  margin-top: 0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.footer-copyright {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.footer-copyright p {
  margin: 8px 0;
  line-height: 1.6;
}

.footer-team {
  font-size: 15px;
  margin-top: 10px !important;
}

.footer-team strong {
  color: #ffffff;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .search-container {
    max-width: 800px;
  }
  
  .carousel-section :deep(.el-carousel) {
    width: 85%;
    max-width: 800px;
  }
  
  .resources-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 25px;
  }
}

@media (max-width: 900px) {
  .carousel-section :deep(.el-carousel) {
    width: 90%;
    max-width: 700px;
  }
  
  .resources-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
  
  .resources-title {
    font-size: 30px;
  }
}

@media (max-width: 768px) {
  .carousel-section {
    padding: 15px 10px 50px;
    min-height: 270px;
  }
  
  .carousel-section :deep(.el-carousel) {
    height: 200px !important;
  }
  
  .carousel-image {
    object-fit: contain;
    border-radius: 12px;
  }
  
  .carousel-section :deep(.el-carousel__indicators) {
    bottom: -35px;
  }
  
  .resources-section {
    padding: 40px 15px;
  }
  
  .resources-title {
    font-size: 26px;
    margin-bottom: 30px;
  }
  
  .resources-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }
  
  .category-card {
    padding: 30px 15px;
  }
  
  .category-icon {
    width: 60px;
    height: 60px;
    margin-bottom: 15px;
  }
  
  .category-name {
    font-size: 16px;
  }

  .featured-section {
    padding: 40px 15px;
  }

  .featured-title {
    font-size: 24px;
    margin-bottom: 30px;
  }

  .featured-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .card-image-wrapper {
    height: 180px;
  }

  .card-content {
    padding: 18px 20px;
  }

  .card-title {
    font-size: 18px;
    margin-bottom: 10px;
  }

  .card-description {
    font-size: 13px;
    margin-bottom: 16px;
    min-height: auto;
  }

  .card-button {
    padding: 9px 20px;
    font-size: 13px;
  }
  
  .close-video-btn {
    width: 40px;
    height: 40px;
    top: 15px;
    right: 15px;
    font-size: 20px;
  }
}

@media (max-width: 1200px) {
  .featured-grid {
    gap: 25px;
  }
  
  .card-image-wrapper {
    height: 180px;
  }
}

@media (max-width: 900px) {
  .featured-section {
    padding: 50px 15px;
  }

  .featured-title {
    font-size: 26px;
  }

  .featured-grid {
    gap: 20px;
  }

  .card-image-wrapper {
    height: 180px;
  }

  .card-title {
    font-size: 19px;
  }

  .footer-section {
    padding: 15px;
  }

  .footer-copyright {
    font-size: 13px;
  }

  .footer-team {
    font-size: 13px !important;
  }
}
</style>