import request from './index'

// ==================== 监督员(NEPS)专属 ====================

/**
 * 提交监督反馈
 * @param {{provinceId:number, cityId:number, specificAddress:string, estimatedAqiLevel:number, description:string}} data
 */
export function submitFeedback(data) {
  return request.post('/feedback/submit', data)
}

/**
 * 我的反馈列表（来源登录态，无需传ID）
 */
export function getMyFeedback() {
  return request.get('/feedback/my')
}

/**
 * 我的反馈分页（支持状态/关键词/日期筛选，自动按登录态过滤本人数据）
 */
export function getMyFeedbackPage(page = 1, size = 10, status = '', keyword = '', startDate = '', endDate = '') {
  return request.get('/feedback/my/page', { params: { page, size, status, keyword, startDate, endDate } })
}

/**
 * 我的反馈统计（总数/各状态数量）
 */
export function getMyFeedbackStats() {
  return request.get('/feedback/my/stats')
}

/**
 * 修改待指派反馈（仅PENDING）
 */
export function updateFeedback(id, data) {
  return request.put(`/feedback/${id}`, data)
}

/**
 * 撤回反馈（仅PENDING状态，归属由后端按登录态校验）
 */
export function cancelFeedback(id) {
  return request.put(`/feedback/cancel/${id}`)
}

/**
 * 评价已完成反馈（1-5星）
 */
export function rateFeedback(id, rating, ratingComment = '') {
  return request.put(`/feedback/rate/${id}`, { rating, ratingComment })
}

// ==================== 共享 ====================

/**
 * 获取反馈详情
 */
export function getFeedbackById(id) {
  return request.get(`/feedback/${id}`)
}

// ==================== 网格员(NEPG)专属 ====================

/**
 * 我的指派任务列表（网格员，仅指派给本人的工单）
 */
export function getAssignedToMe(status = '') {
  return request.get('/feedback/assigned', { params: { status } })
}

/**
 * 我的任务统计（网格员：待检测/已完成数量）
 */
export function getAssignedStats() {
  return request.get('/feedback/assigned/stats')
}

// ==================== 管理员(NEPM)专属 ====================

/**
 * 分页查询全部反馈（支持关键词/日期/状态/优先级筛选）
 */
export function getFeedbackPage(page = 1, size = 10, status = '', keyword = '', startDate = '', endDate = '', assignedInspectorId = null) {
  const params = { page, size, status, keyword, startDate, endDate }
  if (assignedInspectorId) params.assignedInspectorId = assignedInspectorId
  return request.get('/feedback/page', { params })
}

/**
 * 管理员指派网格员
 */
export function assignInspector(id, inspectorId) {
  return request.put(`/feedback/assign/${id}`, null, { params: { inspectorId } })
}

/**
 * 反馈全局统计（管理员：总数/各状态数量）
 */
export function getFeedbackOverview() {
  return request.get('/feedback/stats/overview')
}

/**
 * 为反馈推荐网格员（本地优先→就近异地→全局，管理员）
 */
export function recommendInspectors(feedbackId) {
  return request.get(`/assignment/recommend/${feedbackId}`)
}

/**
 * 管理员转派反馈（后续阶段完善后端）
 */
export function transferFeedback(id, toInspectorId) {
  return request.put(`/feedback/transfer/${id}`, null, { params: { toInspectorId } })
}

/**
 * 管理员批量指派（后续阶段完善后端）
 */
export function batchAssignFeedback(ids, inspectorId) {
  return request.post('/feedback/batch-assign', { ids, inspectorId })
}

/**
 * 网格员拒绝反馈（后续阶段完善后端）
 */
export function rejectFeedback(id, inspectorId, reason) {
  return request.put(`/feedback/reject/${id}`, null, { params: { inspectorId, reason } })
}

/**
 * 网格员接受任务（ASSIGNED→PROCESSING，问题⑦）
 */
export function acceptTask(id) {
  return request.put(`/feedback/accept/${id}`)
}

/**
 * 按姓名派送任务（支持批量反馈，问题①）
 * @param {number[]} feedbackIds 反馈ID数组
 * @param {string} inspectorName 网格员姓名
 */
export function assignByName(feedbackIds, inspectorName) {
  return request.post('/feedback/assign-by-name', { feedbackIds, inspectorName })
}
