import request from './index'

/** 全景统计仪表盘 — 聚合KPI+图表数据（对接Statistics.vue） */
export function getDashboard() {
  return request.get('/statistics/dashboard')
}

/** 系统总览统计 */
export function getOverview() {
  return request.get('/statistics/overview')
}

/** 反馈状态分布 */
export function getFeedbackStatus() {
  return request.get('/statistics/feedback-status')
}

/** AQI等级分布 */
export function getAqiDistribution() {
  return request.get('/statistics/aqi-distribution')
}

/** 各省份反馈统计 */
export function getProvinceFeedback() {
  return request.get('/statistics/province-feedback')
}

/** 月度反馈趋势 */
export function getMonthlyTrend() {
  return request.get('/statistics/monthly-trend')
}

/** 省分组各污染物超标累计（SO2/CO/PM2.5/综合AQI） */
export function getProvincePollutantExceed() {
  return request.get('/statistics/province-pollutant-exceed')
}

/** 全国网格覆盖率 */
export function getCoverage() {
  return request.get('/statistics/coverage')
}

/** AQI检测数量实时统计 */
export function getCount() {
  return request.get('/statistics/count')
}

/** 地图热力图 — 按城市聚合AQI数据 */
export function getMapAqi() {
  return request.get('/statistics/map-aqi')
}

/** 导出空间分析报告 (Excel) */
export function exportSpatialReport() {
  return request.get('/export/spatial-report', { responseType: 'blob' })
}

/** 导出反馈数据 */
export function exportFeedback() {
  return request.get('/export/feedback', { responseType: 'blob' })
}
