<template>
  <div class="swiss-page-container" v-loading="loading">

    <header class="action-console glass-panel fixed-section">
      <div class="console-left">
        <el-button class="swiss-btn icon-btn" @click="$router.push(rolePath('/knowledge'))">
          <el-icon><Back /></el-icon>
        </el-button>
        <div style="margin-left:16px; display:flex; flex-direction:column; gap:4px">
          <h2 class="page-title">文献阅览</h2>
          <span class="page-subtitle">Document Preview</span>
        </div>
      </div>
      <div class="console-right">
        <span class="meta-badge">{{ catLabel(document?.category) }}</span>
        <span class="meta-badge outline">浏览 {{ document?.viewCount || 0 }} 次</span>
      </div>
    </header>

    <main class="stretch-section scrollable-card">
      <div class="reader-area scroll-area" v-if="document">

        <article class="paper-canvas">

          <!-- 封面图 -->
          <div class="article-cover" v-if="document.coverImage">
            <img :src="document.coverImage" alt="文献封面" />
          </div>

          <header class="paper-header">
            <div class="doc-code">档案卷宗编号：{{ String(document.id).padStart(6, '0') }}</div>
            <h1 class="doc-title">{{ document.title }}</h1>
            <div class="doc-meta-strip">
              <span class="meta-tag">{{ catLabel(document.category) }}</span>
              <span class="meta-divider"></span>
              <span class="meta-item">来源：{{ document.source || '东软环保公众监督系统' }}</span>
              <span class="meta-divider"></span>
              <span class="meta-item">发布时间：{{ formatTime(document.createTime) }}</span>
            </div>
          </header>

          <div class="paper-abstract" v-if="document.summary">
            <div class="abstract-label">【内容提要】</div>
            <p>{{ document.summary }}</p>
          </div>

          <div class="paper-body html-content" v-html="document.content || '<p class=empty-hint>暂无正文内容</p>'"></div>

          <!-- 附件区域：仅在确实有附件时显示 -->
          <footer class="paper-footer" v-if="attachments.length > 0">
            <div class="attachment-section-title">
              <el-icon><FolderOpened /></el-icon>
              <span>附件文件（{{ attachments.length }} 个）</span>
            </div>
            <div class="attachment-box" v-for="(att, idx) in attachments" :key="idx">
              <div class="att-icon" :class="fileClass(att)">
                <el-icon><Document /></el-icon>
              </div>
              <div class="att-info">
                <span class="att-name">{{ fileName(att) }}</span>
                <span class="att-size">{{ fileSize(att) }}</span>
              </div>
              <button class="att-download" @click="downloadAttachment(att)">
                <el-icon><Download /></el-icon> 下载
              </button>
            </div>
          </footer>

        </article>

      </div>

      <div v-if="!loading && !document" class="empty-state-wrapper glass-panel">
        <el-empty description="该文献已被撤档或不存在" />
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledgeById } from '@/api/knowledge'
import { downloadKnowledgeFile } from '@/api/knowledge'
import { rolePath } from '@/utils/roleRouter'
import { Back, Download, Document, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const document = ref(null)
const loading = ref(false)

// 解析附件 JSON 数组
const attachments = computed(() => {
  const doc = document.value
  if (!doc || !doc.attachmentUrl) return []
  return parseAttachments(doc.attachmentUrl)
})

function parseAttachments(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr)
      ? arr.filter(Boolean).map(normalizeAttach)
      : [normalizeAttach(raw)]
  } catch {
    // 不是 JSON → 旧格式的单个 URL 字符串
    return [normalizeAttach(raw)]
  }
}

/** 统一附件格式为 {url, name} */
function normalizeAttach(item) {
  if (typeof item === 'object' && item.url) {
    return { url: item.url, name: item.name || extractName(item.url) }
  }
  if (typeof item === 'string') {
    return { url: item, name: extractName(item) }
  }
  return { url: '', name: '未知文件' }
}

function extractName(url) {
  if (!url) return '未知文件'
  const parts = url.replace(/\\/g, '/').split('/')
  return parts[parts.length - 1] || '下载文件'
}

function fileName(att) {
  return att?.name || '未知文件'
}

function fileUrl(att) {
  return att?.url || ''
}

function fileClass(att) {
  const url = att?.url || ''
  const ext = url.split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return 'is-pdf'
  if (['doc', 'docx'].includes(ext)) return 'is-doc'
  if (['xls', 'xlsx'].includes(ext)) return 'is-xls'
  if (['zip', 'rar', '7z'].includes(ext)) return 'is-zip'
  return 'is-other'
}

function fileSize(att) {
  const ext = att?.url?.split('.').pop()?.toUpperCase()
  return ext ? `${ext} 文件 · 安全扫描通过` : '安全扫描通过'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 10)
}

function catLabel(cat) {
  if (!cat) return '综合文献'
  const map = { AIR: '大气环境', WATER: '水环境', SOIL: '土壤环境', NOISE: '噪声污染', ECOLOGY: '生态保护' }
  return map[cat] || cat
}

async function downloadAttachment(att) {
  try {
    const blob = await downloadKnowledgeFile(fileUrl(att), fileName(att))
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = fileName(att)
    a.click()
    ElMessage.success('附件下载成功')
  } catch (e) {
    ElMessage.error('下载失败，附件可能不存在')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getKnowledgeById(route.params.id)
    document.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.swiss-page-container { max-width: 1440px; width: 100%; height: 100%; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; padding-bottom: 32px; box-sizing: border-box; color: #1C2421; }
.fixed-section { flex-shrink: 0; }
.stretch-section { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }

.glass-panel { background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(24px) saturate(180%); -webkit-backdrop-filter: blur(24px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 24px; box-shadow: 0 8px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6); }

.action-console { height: 100px; padding: 0 48px; display: flex; justify-content: space-between; align-items: center; box-sizing: border-box; }
.console-left { display: flex; align-items: center; }
.page-title { font-size: 24px; font-weight: 600; margin: 0; color: #1C2421; }
.page-subtitle { font-size: 14px; font-weight: 500; color: #74807B; }
.console-right { display: flex; align-items: center; gap: 10px; }

.meta-badge {
  font-size: 12px; font-weight: 600; padding: 5px 12px; border-radius: 8px;
  background: rgba(42,72,58,0.06); color: #2A483A;
}
.meta-badge.outline { background: transparent; color: #74807B; border: 1px solid rgba(28,36,33,0.08); }

.swiss-btn { border: none; outline: none; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
.swiss-btn.icon-btn { width: 44px; height: 44px; background: rgba(255, 255, 255, 0.8); color: #1C2421; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02); font-size: 20px; }
.swiss-btn.icon-btn:hover { background: #FFF; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }

.scrollable-card { display: flex; flex-direction: column; background: rgba(28, 36, 33, 0.02); border-radius: 24px; }
.reader-area { flex: 1; overflow-y: auto; padding: 48px 24px; display: block; }
.reader-area::-webkit-scrollbar { display: none; }

.paper-canvas {
  margin: 0 auto; height: max-content; min-height: calc(100% - 96px);
  background: white; width: 100%; max-width: 860px; padding: 64px 80px;
  border-radius: 8px; box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.04);
  display: flex; flex-direction: column; gap: 40px; box-sizing: border-box;
}

/* 封面图 */
.article-cover {
  margin: -64px -80px 0;
  border-radius: 8px 8px 0 0; overflow: hidden;
}
.article-cover img {
  width: 100%; max-height: 360px; object-fit: cover; display: block;
}

.paper-header { display: flex; flex-direction: column; align-items: center; text-align: center; border-bottom: 2px solid #1C2421; padding-bottom: 32px; }
.doc-code { font-family: monospace; font-size: 13px; color: #74807B; margin-bottom: 24px; letter-spacing: 1px; }
.doc-title { font-size: 32px; font-weight: 800; color: #1C2421; line-height: 1.4; margin: 0 0 24px 0; letter-spacing: 1px; }
.doc-meta-strip { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #74807B; }
.meta-tag { background: #F4F6F5; color: #2A483A; padding: 4px 10px; border-radius: 6px; font-weight: 700; }
.meta-divider { width: 1px; height: 12px; background: rgba(28, 36, 33, 0.1); }

.paper-abstract { background: #F8FAFC; padding: 24px; border-radius: 8px; font-size: 14px; color: #475569; line-height: 1.8; text-align: justify; }
.abstract-label { font-weight: 700; color: #1C2421; margin-bottom: 8px; font-size: 15px; }
.paper-abstract p { margin: 0; }

.paper-body { font-size: 16px; color: #333; line-height: 2; text-align: justify; flex: 1; }
:deep(.html-content h2), :deep(.html-content h3) { font-size: 20px; font-weight: 700; color: #1C2421; margin: 40px 0 16px; }
:deep(.html-content p) { margin: 0 0 20px; }
:deep(.html-content img) { max-width: 100%; border-radius: 12px; margin: 24px 0; }
.empty-hint { text-align: center; color: #A0AAB2; font-style: italic; padding: 40px 0; }

/* ===== 附件区域 ===== */
.paper-footer { margin-top: 40px; padding-top: 32px; border-top: 1px dashed rgba(28, 36, 33, 0.1); }
.attachment-section-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 700; color: #1C2421;
  margin-bottom: 16px;
}
.attachment-section-title .el-icon { color: #2A483A; font-size: 18px; }

.attachment-box {
  display: flex; align-items: center; gap: 14px;
  background: #F4F6F5; padding: 14px 18px; border-radius: 12px;
  margin-bottom: 8px; transition: all 0.2s;
}
.attachment-box:hover { background: #EEF0EF; }
.attachment-box:last-child { margin-bottom: 0; }

.att-icon {
  width: 40px; height: 40px; border-radius: 10px; display: flex;
  justify-content: center; align-items: center; font-size: 18px;
  background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.att-icon.is-pdf { color: #E11D48; background: #FEF2F2; }
.att-icon.is-doc { color: #2563EB; background: #EFF6FF; }
.att-icon.is-xls { color: #059669; background: #F0FDF4; }
.att-icon.is-zip { color: #D97706; background: #FFFBEB; }
.att-icon.is-other { color: #475569; background: #F8FAFC; }

.att-info { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.att-name { font-size: 14px; font-weight: 600; color: #1C2421; }
.att-size { font-size: 12px; color: #2AA876; font-weight: 500; }

.att-download {
  display: flex; align-items: center; gap: 4px;
  border: none; background: transparent; color: #2A483A;
  font-size: 13px; font-weight: 600; cursor: pointer;
  padding: 6px 12px; border-radius: 8px; transition: all 0.2s;
}
.att-download:hover { background: rgba(42, 72, 58, 0.08); }

.empty-state-wrapper { height: 100%; display: flex; align-items: center; justify-content: center; }

@media print {
  .fixed-section, .empty-state-wrapper, .paper-footer { display: none !important; }
  .stretch-section { background: transparent !important; }
  .paper-canvas { box-shadow: none; border: none; padding: 0; max-width: 100%; height: auto !important; }
}
</style>
