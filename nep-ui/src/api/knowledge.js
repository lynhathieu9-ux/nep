import request from './index'

/** 分页查询知识库 */
export function getKnowledgePage(page = 1, size = 10, params = {}) {
  return request.get('/knowledge/page', { params: { page, size, ...params } })
}

/** 获取知识详情 */
export function getKnowledgeById(id) {
  return request.get(`/knowledge/${id}`)
}

/** 热门知识推荐 */
export function getHotKnowledge(count = 5) {
  return request.get('/knowledge/hot', { params: { count } })
}

/** 点赞 */
export function likeKnowledge(id) {
  return request.post(`/knowledge/like/${id}`)
}

/** 发布知识（管理员） */
export function createKnowledge(data) {
  return request.post('/knowledge', data)
}

/** 更新知识（管理员） */
export function updateKnowledge(id, data) {
  return request.put(`/knowledge/${id}`, data)
}

/** 删除知识（管理员） */
export function deleteKnowledge(id) {
  return request.delete(`/knowledge/${id}`)
}

/** 问题⑤：下载知识库附件（返回 blob，带鉴权） */
export function downloadKnowledgeFile(path, name) {
  return request.get('/file/download', { params: { path, name }, responseType: 'blob' })
}

/** 导出知识库内容为PDF文档（无附件时自动降级） */
export function exportKnowledgePdf(id) {
  return request.get(`/knowledge/${id}/export`, { responseType: 'blob' })
}
