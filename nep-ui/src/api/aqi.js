import request from './index'

/**
 * 提交AQI检测数据
 */
export function submitAqi(data) {
  return request.post('/aqi/submit', data)
}

/**
 * 分页查询AQI列表
 */
export function getAqiPage(page = 1, size = 10) {
  return request.get('/aqi/page', { params: { page, size } })
}

/**
 * 获取AQI详情
 */
export function getAqiById(id) {
  return request.get(`/aqi/${id}`)
}

/**
 * 查询我的检测记录
 */
export function getMyDetections(inspectorId) {
  return request.get(`/aqi/my/${inspectorId}`)
}

/**
 * 获取统计数据
 */
export function getStatistics() {
  return request.get('/statistics/count')
}
