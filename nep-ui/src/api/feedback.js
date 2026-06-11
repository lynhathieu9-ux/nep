import request from './index'

/**
 * 提交监督反馈
 */
export function submitFeedback(data) {
  return request.post('/feedback/submit', data)
}

/**
 * 分页查询反馈列表
 */
export function getFeedbackPage(page = 1, size = 10, status = '') {
  return request.get('/feedback/page', { params: { page, size, status } })
}

/**
 * 获取反馈详情
 */
export function getFeedbackById(id) {
  return request.get(`/feedback/${id}`)
}

/**
 * 管理员指派网格员
 */
export function assignInspector(id, inspectorId) {
  return request.put(`/feedback/assign/${id}`, null, { params: { inspectorId } })
}

/**
 * 查询我的反馈
 */
export function getMyFeedback(supervisorId) {
  return request.get(`/feedback/my/${supervisorId}`)
}
