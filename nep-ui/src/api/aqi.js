import request from './index'

// ==================== 网格员(NEPG)专属 ====================

/**
 * 提交AQI检测数据（网格员）
 * @param {{feedbackId:number, so2Aqi:number, coAqi:number, pm25Aqi:number, remark?:string}} data
 */
export function submitAqi(data) {
  return request.post('/aqi/submit', data)
}

/**
 * 我的检测记录（网格员，登录态）
 */
export function getMyDetections() {
  return request.get('/aqi/my')
}

/** 别名 */
export const getMyAqiRecords = getMyDetections

/**
 * 我的检测工作量统计（网格员：总数/超标/达标）
 */
export function getMyAqiStats() {
  return request.get('/aqi/my/stats')
}

// ==================== 管理员(NEPM)专属 ====================

/**
 * 分页查询AQI列表（管理员）
 */
export function getAqiPage(page = 1, size = 10) {
  return request.get('/aqi/page', { params: { page, size } })
}

/**
 * AQI检测全局统计（管理员：总数/超标数/超标率）
 */
export function getAqiOverview() {
  return request.get('/aqi/stats/overview')
}

// ==================== 共享 ====================

/**
 * 获取AQI详情
 */
export function getAqiById(id) {
  return request.get(`/aqi/${id}`)
}

/**
 * 根据反馈ID获取AQI检测数据
 */
export function getAqiByFeedbackId(feedbackId) {
  return request.get(`/aqi/by-feedback/${feedbackId}`)
}

/**
 * 获取统计数据
 */
export function getStatistics() {
  return request.get('/statistics/count')
}
