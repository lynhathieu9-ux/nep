import request from './index'

/**
 * 获取所有省份
 */
export function getProvinces() {
  return request.get('/region/provinces')
}

/**
 * 根据省份获取城市
 */
export function getCitiesByProvince(provinceId) {
  return request.get(`/region/cities/${provinceId}`)
}

/**
 * 获取所有城市
 */
export function getAllCities() {
  return request.get('/region/cities')
}
