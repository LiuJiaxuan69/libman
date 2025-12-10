import request from './request'

/**
 * 获取所有分类
 */
export function getCategoryList() {
  return request({
    url: '/category/list',
    method: 'get'
  })
}

/**
 * 添加分类
 */
export function addCategory(category) {
  return request({
    url: '/category/add',
    method: 'post',
    data: category
  })
}

/**
 * 删除分类
 */
export function deleteCategory(categoryId) {
  return request({
    url: '/category/delete',
    method: 'post',
    data: categoryId
  })
}