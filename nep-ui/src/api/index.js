import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器 - 自动携带JWT Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 401跳转登录，Blob下载直接放行
request.interceptors.response.use(
  response => {
    // 文件下载（Blob/PDF）直接返回，不做 JSON code 检查
    const contentType = response.headers['content-type'] || ''
    if (contentType.includes('application/vnd.openxmlformats-officedocument') ||
        contentType.includes('application/pdf') ||
        contentType.includes('application/octet-stream') ||
        response.config.responseType === 'blob') {
      return response.data
    }

    const data = response.data
    // 兼容 { code: 200 } 和直接返回数据的格式
    if (data && typeof data === 'object' && data.code !== undefined) {
      if (data.code !== 200) {
        ElMessage.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message))
      }
      return data
    }
    // 非标准格式直接返回
    return data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
