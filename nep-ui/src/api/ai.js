import request from './index'

/** AI普通对话 */
export function chat(message) {
  return request.post('/ai/chat', { message })
}

/** AI流式对话 - 返回fetch response用于SSE */
export function chatStream(message) {
  const token = localStorage.getItem('token')
  return fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({ message })
  })
}
