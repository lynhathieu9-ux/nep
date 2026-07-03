import request from './index'

/** 用户注册 */
export function register(data) {
  return request.post('/user/register', data)
}

/** 用户登录 */
export function login(phone, password) {
  return request.post('/user/login', null, { params: { phone, password } })
}

/** 获取用户信息 */
export function getUserById(id) {
  return request.get(`/user/${id}`)
}

/** 获取用户列表 */
export function getUserList() {
  return request.get('/user/list')
}

/** 用户统计（管理员：总数/各角色数量） */
export function getUserStats() {
  return request.get('/user/stats')
}

/** 更新用户信息 */
export function updateUser(id, data) {
  return request.put(`/user/${id}`, data)
}

/** 修改密码 */
export function changePassword(data) {
  return request.put('/user/change-password', data)
}

/** 管理员重置用户密码（问题③） */
export function resetUserPassword(id, newPassword) {
  return request.put(`/user/${id}/reset-password`, { newPassword })
}

/** 网格员姓名列表（管理员派送用，问题①） */
export function getInspectors() {
  return request.get('/user/inspectors')
}
