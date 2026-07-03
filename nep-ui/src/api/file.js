import request from './index'

/**
 * 上传图片（环保资讯配图等，问题④）
 * @param {File} file 图片文件
 * @returns Promise<Result<string>> data 为可访问的图片URL
 */
export function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/**
 * 上传知识库附件（pdf/doc等，问题⑤）
 */
export function uploadDoc(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/doc', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
