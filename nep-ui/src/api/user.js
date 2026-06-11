import request from './index'

/**
 * 用户注册
 */
export function register(data) {
  return request.post('/user/register', data)
}

/**
 * 用户登录
 */
export function login(phone, password) {
  return request.post('/user/login', null, { params: { phone, password } })
}

/**
 * 获取用户信息
 */
export function getUserById(id) {
  return request.get(`/user/${id}`)
}

/**
 * 获取用户列表
 */
export function getUserList() {
  return request.get('/user/list')
}

/**
 * 更新用户信息
 */
export function updateUser(id, data) {
  return request.put(`/user/${id}`, data)
}
