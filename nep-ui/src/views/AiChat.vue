<template>
  <div class="ai-page">
    <div class="ai-layout">
      <!-- 主对话区 -->
      <div class="chat-main">
        <!-- 顶部栏 -->
        <div class="chat-topbar">
          <div class="topbar-left">
            <div class="ai-avatar">🤖</div>
            <div>
              <div class="ai-name">AI环保助手</div>
              <div class="ai-status">
                <span class="dot"></span>
                {{ streaming ? '正在输入...' : '在线 · DeepSeek' }}
              </div>
            </div>
          </div>
          <div class="topbar-right">
            <el-button text circle :icon="Delete" @click="clearChat" title="清空对话"/>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="chat-messages" ref="msgBox">
          <!-- 空状态 -->
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">🌿</div>
            <h2>AI环保助手</h2>
            <p>基于 DeepSeek 大模型，为您解答环保相关问题</p>
            <div class="suggestion-grid">
              <div
                v-for="s in suggestions"
                :key="s.title"
                class="suggestion-card"
                @click="sendSuggestion(s.prompt)"
              >
                <div class="sug-icon">{{ s.icon }}</div>
                <div class="sug-title">{{ s.title }}</div>
                <div class="sug-desc">{{ s.desc }}</div>
              </div>
            </div>
          </div>

          <!-- 消息列表 -->
          <div v-for="(msg, idx) in messages" :key="idx" class="msg-wrapper">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="msg-row user-row">
              <div class="msg-bubble user-bubble">
                <div class="msg-text">{{ msg.content }}</div>
              </div>
              <el-avatar :size="34" icon="UserFilled" class="msg-avatar"/>
            </div>

            <!-- AI消息 -->
            <div v-else class="msg-row ai-row">
              <el-avatar :size="34" class="msg-avatar ai-avatar-bg">AI</el-avatar>
              <div class="msg-bubble ai-bubble">
                <div class="msg-text" v-html="renderContent(msg.content)"/>
                <!-- 打字光标 -->
                <span v-if="idx === messages.length-1 && streaming" class="typing-cursor">|</span>
              </div>
              <!-- 操作按钮 -->
              <div v-if="msg.content && !streaming" class="msg-actions">
                <el-button text size="small" :icon="CopyDocument" @click="copyText(msg.content)" title="复制"/>
              </div>
            </div>
          </div>

          <!-- 加载占位 -->
          <div v-if="waiting" class="msg-row ai-row">
            <el-avatar :size="34" class="msg-avatar ai-avatar-bg">AI</el-avatar>
            <div class="msg-bubble ai-bubble thinking-bubble">
              <span class="dot-typing"></span>
              <span class="dot-typing"></span>
              <span class="dot-typing"></span>
            </div>
          </div>

          <div ref="scrollAnchor"></div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <el-input
              ref="inputRef"
              v-model="input"
              :disabled="streaming"
              placeholder="输入环保相关问题，Enter 发送，Shift+Enter 换行"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              class="chat-input"
              @keydown.enter.exact="handleEnter"
              @keydown.enter.shift="handleShiftEnter"
            />
            <el-button
              type="primary"
              :icon="streaming ? VideoPause : Promotion"
              :disabled="!input.trim() || streaming"
              @click="send"
              class="send-btn"
              size="large"
            >
            </el-button>
          </div>
          <div class="input-hint">
            <span>💡 Enter 发送 · Shift+Enter 换行</span>
            <span>{{ input.length }}/2000</span>
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">💡 快捷提问</div>
        <div class="sidebar-prompts">
          <div
            v-for="p in quickPrompts"
            :key="p"
            class="prompt-item"
            @click="sendSuggestion(p)"
          >
            {{ p }}
          </div>
        </div>

        <div class="sidebar-divider"></div>

        <div class="sidebar-header">📋 对话记录</div>
        <div class="history-list">
          <div
            v-for="(h, i) in history"
            :key="i"
            class="history-item"
            :class="{ active: i === history.length - 1 }"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ h.title }}</span>
          </div>
          <el-empty v-if="history.length === 0" description="暂无记录" :image-size="40" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from 'vue'
import { chat, chatStream } from '@/api/ai'
import { ElMessage } from 'element-plus'

const input = ref('')
const streaming = ref(false)
const waiting = ref(false)
const messages = ref([])
const msgBox = ref(null)
const inputRef = ref(null)
const scrollAnchor = ref(null)
const history = ref([])

// 欢迎建议
const suggestions = [
  { icon: '📊', title: 'AQI指数计算', desc: '如何计算AQI空气质量指数？', prompt: '请详细解释AQI空气质量指数的计算方法' },
  { icon: '🌫️', title: 'PM2.5危害', desc: 'PM2.5对健康有什么影响？', prompt: 'PM2.5对人体健康具体有哪些危害？如何防护？' },
  { icon: '🛡️', title: '环保监督流程', desc: '公众如何参与环保监督？', prompt: '介绍一下公众参与环保监督的流程和方式' },
  { icon: '📈', title: '空气质量趋势', desc: '中国近年空气质量变化', prompt: '中国近年来空气质量改善情况和未来趋势' },
]

// 侧边栏快捷提问
const quickPrompts = [
  '什么是空气质量指数AQI？',
  '二氧化硫的主要来源有哪些？',
  'PM2.5和PM10有什么区别？',
  '如何减少碳排放？',
  '一氧化碳中毒的症状是什么？',
  '环保监督员的主要职责是什么？',
  '中国106个大城市是哪些？',
  '空气质量分为几个等级？',
]

// ========== 渲染内容 ==========
function renderContent(text) {
  if (!text) return ''
  // 简单Markdown：粗体、代码块、换行
  let html = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
    // 处理编号列表 1. 2. 3.
    .replace(/(\d+)\.\s/g, '<br>$1. ')
  return html
}

// ========== 发送消息 ==========
async function sendSuggestion(prompt) {
  input.value = prompt
  await send()
}

async function handleEnter() {
  if (!streaming.value && input.value.trim()) {
    await send()
  }
}

function handleShiftEnter() {
  // 自然换行，不做处理
}

async function send() {
  const msg = input.value.trim()
  if (!msg || streaming.value) return

  input.value = ''
  waiting.value = true
  messages.value.push({ role: 'user', content: msg })
  await scrollToBottom()

  try {
    streaming.value = true
    waiting.value = false
    messages.value.push({ role: 'ai', content: '' })
    await scrollToBottom()

    const response = await chatStream(msg)
    if (!response.ok) throw new Error('Stream failed')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.substring(5).trim()
        if (data === '[DONE]') break
        if (data.startsWith('[ERROR]')) {
          messages.value[messages.value.length - 1].content = '抱歉，AI服务请求失败：' + data.substring(7)
          break
        }
        if (data) {
          messages.value[messages.value.length - 1].content += data
          await scrollToBottom()
        }
      }
    }
  } catch (e) {
    // 回退普通请求
    try {
      const res = await chat(msg)
      messages.value[messages.value.length - 1].content = res.data || '抱歉，AI服务暂不可用'
    } catch (e2) {
      messages.value[messages.value.length - 1].content = '抱歉，AI服务请求失败，请检查网络或API配置。'
    }
  } finally {
    streaming.value = false
    waiting.value = false
    saveHistory()
    await scrollToBottom()
    nextTick(() => inputRef.value?.focus())
  }
}

// ========== 工具方法 ==========
function copyText(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.info('复制失败，请手动选择复制')
  })
}

function clearChat() {
  messages.value = []
  ElMessage.success('对话已清空')
}

function saveHistory() {
  const firstUserMsg = messages.value.find(m => m.role === 'user')
  if (firstUserMsg) {
    history.value.push({
      title: firstUserMsg.content.substring(0, 30) + (firstUserMsg.content.length > 30 ? '...' : '')
    })
  }
}

async function scrollToBottom() {
  await nextTick()
  scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
}

watch(messages, () => scrollToBottom(), { deep: true })

onMounted(() => {
  nextTick(() => inputRef.value?.focus())
})
</script>

<style scoped>
.ai-page { max-width: 1300px; margin: 0 auto; height: calc(100vh - 120px); }

.ai-layout {
  display: flex;
  height: 100%;
  gap: 20px;
}

/* ========== 主对话区 ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

/* 顶部 */
.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
  background: #fafbfc;
}
.topbar-left { display: flex; align-items: center; gap: 12px; }
.ai-avatar { font-size: 28px; }
.ai-name { font-size: 15px; font-weight: 700; }
.ai-status { font-size: 12px; color: #0c8c3f; display: flex; align-items: center; gap: 6px; }
.dot { width: 8px; height: 8px; background: #0c8c3f; border-radius: 50%; display: inline-block; animation: pulse 2s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }

/* 消息区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

/* 空状态 */
.empty-state { text-align: center; padding: 40px 20px; }
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-state h2 { font-size: 22px; font-weight: 700; color: #1a1a1a; margin: 0 0 8px 0; }
.empty-state > p { font-size: 14px; color: #999; margin-bottom: 32px; }

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 560px;
  margin: 0 auto;
}
.suggestion-card {
  text-align: left;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
}
.suggestion-card:hover {
  border-color: #409EFF;
  background: #f5f8ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(64,158,255,0.1);
}
.sug-icon { font-size: 24px; margin-bottom: 4px; }
.sug-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 2px; }
.sug-desc { font-size: 12px; color: #999; }

/* 消息行 */
.msg-wrapper { margin-bottom: 24px; }
.msg-row { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 20px; }
.user-row { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.ai-avatar-bg { background: linear-gradient(135deg, #0c8c3f, #1a6b3a) !important; color: #fff !important; font-size: 14px; font-weight: 700; }

.msg-bubble {
  max-width: 75%;
  padding: 14px 18px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  position: relative;
}

.user-bubble {
  background: linear-gradient(135deg, #409EFF, #337ecc);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(64,158,255,0.25);
}

.ai-bubble {
  background: #f7f8fa;
  color: #303133;
  border-bottom-left-radius: 4px;
  border: 1px solid #f0f0f0;
}

.msg-text :deep(strong) { color: #0c8c3f; }
.msg-text :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', monospace;
}
.msg-text :deep(pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}
.msg-text :deep(pre code) { background: none; color: inherit; }

.typing-cursor {
  display: inline-block;
  animation: blink 0.8s infinite;
  font-weight: 700;
  color: #409EFF;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.msg-actions { display: flex; gap: 4px; margin-top: 4px; opacity: 0; transition: opacity 0.2s; }
.msg-row:hover .msg-actions { opacity: 1; }

/* 思考动画 */
.thinking-bubble { display: flex; gap: 6px; align-items: center; padding: 18px; }
.dot-typing {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #ccc;
  animation: dotBounce 1.4s infinite;
}
.dot-typing:nth-child(2) { animation-delay: 0.2s; }
.dot-typing:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce { 0%,80%,100%{transform:scale(0.6);opacity:0.4} 40%{transform:scale(1);opacity:1} }

/* 输入区 */
.chat-input-area {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafbfc;
}
.input-wrapper { display: flex; gap: 10px; align-items: flex-end; }
.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  padding: 12px 16px;
  transition: all 0.3s;
}
.chat-input :deep(.el-textarea__inner:focus) { border-color: #409EFF; box-shadow: 0 0 0 2px rgba(64,158,255,0.1); }
.send-btn {
  width: 46px; height: 46px;
  border-radius: 12px;
  flex-shrink: 0;
}

.input-hint {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: #ccc;
  padding: 0 4px;
}

/* ========== 侧边栏 ========== */
.chat-sidebar {
  width: 270px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  padding: 20px;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-header { font-size: 14px; font-weight: 700; color: #303133; margin-bottom: 12px; }

.sidebar-prompts { display: flex; flex-direction: column; gap: 6px; margin-bottom: 4px; }
.prompt-item {
  font-size: 13px;
  color: #666;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}
.prompt-item:hover { background: #f5f7fa; color: #409EFF; }

.sidebar-divider { height: 1px; background: #f0f0f0; margin: 20px 0; }

.history-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #666;
  padding: 8px 10px; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
}
.history-item:hover, .history-item.active { background: #f5f7fa; color: #409EFF; }
.history-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 1000px) {
  .chat-sidebar { display: none; }
}
</style>
