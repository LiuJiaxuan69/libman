import request from './request'

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

/**
 * 用户登录
 */
export function login(credentials) {
  // 后端使用 @RequestParam，需要发送表单格式
  const params = new URLSearchParams()
  params.append('userName', credentials.userName)
  params.append('password', credentials.password)
  
  return request({
    url: '/user/login',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 用户注册
 */
export function register(userInfo) {
  // 后端使用 @RequestParam，需要发送表单格式
  const params = new URLSearchParams()
  params.append('userName', userInfo.userName)
  params.append('password', userInfo.password)
  
  return request({
    url: '/user/register',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request({
    url: '/user/logout',
    method: 'post'
  })
}

/**
 * 获取用户借阅历史
 */
export function getBorrowHistory() {
  return request({
    url: '/user/borrowHistory',
    method: 'get'
  })
}

/**
 * 获取当前借阅的图书
 */
export function getCurrentBorrows() {
  return request({
    url: '/user/currentBorrows',
    method: 'get'
  })
}

/**
 * 更新用户头像
 */
export function updateAvatar(avatarFile) {
  const formData = new FormData()
  formData.append('avatar', avatarFile)
  
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新用户昵称
 */
export function updateNickname(nickName) {
  const params = new URLSearchParams()
  params.append('nickName', nickName)
  
  return request({
    url: '/user/nickname',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 更新用户密码
 */
export function updatePassword(oldPassword, newPassword) {
  const params = new URLSearchParams()
  params.append('oldPassword', oldPassword)
  params.append('newPassword', newPassword)
  
  return request({
    url: '/user/password',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 提交用户反馈
 */
export function submitFeedback(content, rating = null) {
  const params = new URLSearchParams()
  params.append('content', content)
  if (rating !== null) {
    params.append('rating', rating)
  }
  
  return request({
    url: '/feedback/submit',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}