<template>
  <div class="swiss-page-container">
    
    <header class="action-console glass-panel fixed-section">
      <div class="console-left">
        <h2 class="page-title">环保知识库</h2>
        <span class="page-subtitle">系统规范、作业指导与环保法规全案检索</span>
      </div>

      <div class="console-right">
        <div class="search-capsule">
          <el-icon><Search /></el-icon>
          <el-input
            v-model="searchQuery"
            placeholder="搜索文献题名或发文字号..."
            clearable
            class="keyword-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <div class="search-capsule">
          <span class="filter-label">类目</span>
          <el-select v-model="filterType" placeholder="全部分类" clearable class="swiss-select" popper-class="swiss-select-dropdown" @change="handleSearch">
            <el-option label="全部分类" value="" />
            <el-option label="作业标准" value="STANDARD" />
            <el-option label="法律法规" value="LAW" />
            <el-option label="应急预案" value="EMERGENCY" />
          </el-select>
        </div>
        <el-button class="swiss-btn icon-btn" @click="handleReset" title="重置检索">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
      </div>
    </header>

    <main class="stretch-section glass-panel scrollable-card" v-loading="loading">
      
      <div class="list-header-row">
        <div class="col-icon"></div>
        <div class="col-title">文献题名</div>
        <div class="col-category">所属类目</div>
        <div class="col-date">颁布时间</div>
        <div class="col-actions">操作</div>
      </div>

      <div class="scroll-area">
        <div v-if="knowledgeList.length === 0 && !loading" class="empty-state-wrapper">
          <el-empty description="未检索到相关文献" :image-size="80" />
        </div>

        <div v-else class="document-list">
          <div 
            v-for="doc in knowledgeList" 
            :key="doc.id" 
            class="document-row"
            @click="$router.push(rolePath(`/knowledge/${doc.id}`))"
          >
            <div class="col-icon">
              <div class="cover-thumb" v-if="doc.coverImage">
                <img :src="doc.coverImage" alt="封面" />
              </div>
              <div class="format-badge" v-else :class="getFileFormat(doc.attachmentUrl)">
                {{ getFileFormat(doc.attachmentUrl) }}
              </div>
            </div>
            
            <div class="col-title">
              <span class="doc-name">{{ doc.title }}</span>
              <span class="doc-summary">{{ doc.summary || '暂无文献摘要' }}</span>
            </div>
            
            <div class="col-category">
              <span class="cat-tag">{{ typeLabel(doc.category) }}</span>
            </div>
            
            <div class="col-date">
              <span class="date-text">{{ formatTime(doc.createTime) }}</span>
            </div>
            
            <div class="col-actions">
              <button class="action-btn outline" @click.stop="$router.push(rolePath(`/knowledge/${doc.id}`))" title="预览">
                <el-icon><View /></el-icon>
              </button>
              <button class="action-btn primary" @click.stop="downloadDoc(doc)" title="下载">
                <el-icon><Download /></el-icon>
              </button>
            </div>
          </div>
        </div>

        <div class="list-pagination" v-if="total > size">
          <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="prev, pager, next" class="swiss-pagination" @current-change="fetchKnowledge" />
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgePage, downloadKnowledgeFile } from '@/api/knowledge'
import { rolePath } from '@/utils/roleRouter'
import { Search, RefreshRight, View, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const knowledgeList = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(15)
const total = ref(0)

const filterType = ref('')
const searchQuery = ref('')

// 辅助方法：从附件URL获取文件后缀标签
function getFileFormat(rawUrl) {
  if (!rawUrl) return 'DOC'
  // 支持 JSON 数组 → 取第一个附件展示
  let url = rawUrl
  try {
    const arr = JSON.parse(rawUrl)
    if (Array.isArray(arr) && arr.length > 0) url = arr[0]
  } catch { /* 单个URL直接用 */ }
  const ext = url.split('.').pop()?.toLowerCase() || ''
  if (['pdf'].includes(ext)) return 'PDF'
  if (['xls', 'xlsx'].includes(ext)) return 'XLS'
  if (['doc', 'docx'].includes(ext)) return 'DOC'
  if (['zip', 'rar', '7z'].includes(ext)) return 'ZIP'
  return 'DOC'
}

function typeLabel(type) {
  if (type === 'STANDARD') return '作业标准'
  if (type === 'LAW') return '法律法规'
  if (type === 'EMERGENCY') return '应急预案'
  return '未分类文献'
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 10)
}

function handleSearch() {
  page.value = 1
  fetchKnowledge()
}

function handleReset() {
  filterType.value = ''
  searchQuery.value = ''
  handleSearch()
}

async function downloadDoc(doc) {
  if (!doc.attachmentUrl) {
    ElMessage.warning('该文献暂无可下载的附件')
    return
  }
  // 解析附件（JSON 数组或单个URL）
  const urls = parseAttachUrls(doc.attachmentUrl)
  if (urls.length === 0) {
    ElMessage.warning('该文献暂无可下载的附件')
    return
  }
  // 只有一个附件 → 直接下载；多个 → 下载第一个（用户可进详情页逐个下载）
  const url = urls[0]
  const filename = url.split('/').pop() || doc.title
  try {
    const blob = await downloadKnowledgeFile(url, filename)
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    a.click()
    ElMessage.success('附件下载成功')
  } catch (e) {
    ElMessage.error('下载失败，附件可能不存在')
  }
}

function parseAttachUrls(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return [typeof arr === 'string' ? arr : (arr?.url || '')]
    return arr.filter(Boolean).map(item =>
      typeof item === 'object' && item.url ? item.url :
      typeof item === 'string' ? item : ''
    ).filter(Boolean)
  } catch {
    return [raw]
  }
}

async function fetchKnowledge() {
  loading.value = true
  try {
    const params = {}
    if (filterType.value) params.category = filterType.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const res = await getKnowledgePage(page.value, size.value, params)
    knowledgeList.value = res.data || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}

onMounted(() => fetchKnowledge())
</script>

<style scoped>
.swiss-page-container { max-width: 1440px; width: 100%; height: 100%; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; padding-bottom: 32px; box-sizing: border-box; color: #1C2421; }
.fixed-section { flex-shrink: 0; }
.stretch-section { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.glass-panel { background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(24px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 24px; box-shadow: 0 8px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6); }

.action-console { height: 100px; padding: 0 48px; display: flex; justify-content: space-between; align-items: center; box-sizing: border-box; }
.console-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 24px; font-weight: 600; margin: 0; color: #1C2421; }
.page-subtitle { font-size: 14px; font-weight: 500; color: #74807B; }
.console-right { display: flex; align-items: center; gap: 16px; }

.search-capsule { display: flex; align-items: center; gap: 12px; background: rgba(255, 255, 255, 0.5); padding: 8px 16px 8px 20px; border-radius: 100px; border: 1px solid rgba(255, 255, 255, 0.8); box-shadow: 0 4px 16px -4px rgba(0, 0, 0, 0.03); }
.filter-label { font-size: 13px; font-weight: 600; color: #74807B; }
.keyword-input { width: 220px; }
.keyword-input :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; border-radius: 0; }

.swiss-btn { border: none; outline: none; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
.swiss-btn.icon-btn { width: 40px; height: 40px; background: rgba(255, 255, 255, 0.8); color: #74807B; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02); font-size: 18px; }
.swiss-btn.icon-btn:hover { background: #FFF; color: #1C2421; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }

/* ========== 高阶列表排版 ========== */
.list-header-row {
  display: flex; align-items: center; padding: 0 48px; height: 56px; flex-shrink: 0;
  border-bottom: 1px solid rgba(28, 36, 33, 0.06);
  font-size: 12px; font-weight: 600; color: #A0AAB2; letter-spacing: 0.5px;
}
.scrollable-card { display: flex; flex-direction: column; }
.scroll-area { flex: 1; overflow-y: auto; padding: 16px 48px 48px; }
.scroll-area::-webkit-scrollbar { display: none; }

/* 严格控制列宽比例 */
.col-icon { width: 64px; flex-shrink: 0; }
.col-title { flex: 1; min-width: 0; padding-right: 24px; display: flex; flex-direction: column; gap: 4px; }
.col-category { width: 140px; flex-shrink: 0; }
.col-date { width: 120px; flex-shrink: 0; }
.col-actions { width: 100px; flex-shrink: 0; display: flex; gap: 8px; justify-content: flex-end; }

.document-list { display: flex; flex-direction: column; gap: 8px; }
.document-row {
  display: flex; align-items: center; padding: 16px 0; border-radius: 16px;
  background: transparent; cursor: pointer; transition: all 0.3s ease;
  border-bottom: 1px solid rgba(28, 36, 33, 0.03);
}
.document-row:last-child { border-bottom: none; }
.document-row:hover { background: rgba(255, 255, 255, 0.8); transform: scale(1.005); box-shadow: 0 8px 24px -8px rgba(0, 0, 0, 0.04); padding: 16px; margin: 0 -16px; }

/* 封面缩略图 */
.cover-thumb {
  width: 52px; height: 36px; border-radius: 6px; overflow: hidden;
  border: 1px solid rgba(0,0,0,0.06);
}
.cover-thumb img {
  width: 100%; height: 100%; object-fit: cover;
}

/* 拟物化微标 */
.format-badge {
  width: 44px; height: 44px; border-radius: 12px; display: flex; justify-content: center; align-items: center;
  font-size: 11px; font-weight: 800; font-family: "SF Pro Rounded", sans-serif; letter-spacing: 0.5px;
}
.format-badge.PDF { background: #FEF2F2; color: #E11D48; border: 1px solid #FECDD3; }
.format-badge.DOC { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
.format-badge.XLS { background: #F0FDF4; color: #059669; border: 1px solid #BBF7D0; }
.format-badge.TXT { background: #F8FAFC; color: #475569; border: 1px solid #E2E8F0; }

.doc-name { font-size: 15px; font-weight: 600; color: #1C2421; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-summary { font-size: 12px; color: #A0AAB2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.cat-tag { font-size: 12px; font-weight: 600; color: #2A483A; background: rgba(42, 72, 58, 0.06); padding: 4px 10px; border-radius: 8px; }
.date-text { font-size: 13px; color: #74807B; font-family: monospace; font-weight: 500; }

.action-btn { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 16px; border: none; cursor: pointer; transition: all 0.3s; opacity: 0; }
.document-row:hover .action-btn { opacity: 1; }
.action-btn.primary { background: #2A483A; color: white; }
.action-btn.primary:hover { background: #1C2421; box-shadow: 0 4px 12px rgba(42, 72, 58, 0.2); transform: translateY(-1px); }
.action-btn.outline { background: white; color: #74807B; border: 1px solid rgba(28,36,33,0.1); }
.action-btn.outline:hover { color: #1C2421; border-color: #1C2421; }

.list-pagination { display: flex; justify-content: center; margin-top: 32px; }
:deep(.swiss-pagination) { --el-pagination-bg-color: transparent; }
.empty-state-wrapper { height: 100%; display: flex; justify-content: center; align-items: center; }
</style>
<style>
.swiss-select { width: 140px; }
.swiss-select .el-input__wrapper { background: transparent !important; box-shadow: none !important; }
.swiss-select-dropdown { background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(24px) !important; border: 1px solid rgba(255, 255, 255, 0.9) !important; border-radius: 12px !important; box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08) !important; }
.swiss-select-dropdown .el-select-dropdown__item.selected { color: #2A483A !important; font-weight: 600 !important; }
</style>