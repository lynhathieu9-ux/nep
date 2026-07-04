<template>
  <div class="root">
    <!-- 页头：毛玻璃卡片 -->
    <div class="top-bar">
      <div class="top-left">
        <h2 class="top-title">环保知识库</h2>
        <p class="top-sub">系统规范、作业指导与环保法规全案检索</p>
      </div>
      <div class="top-right">
        <div class="search-box">
          <el-input v-model="searchQuery" placeholder="搜索文献题名..." clearable class="s-input" @keyup.enter="handleSearch" @clear="handleSearch" />
        </div>
        <div class="filter-box">
          <span class="f-label">类目</span>
          <el-select v-model="filterType" placeholder="全部分类" clearable class="s-select" @change="handleSearch">
            <el-option label="全部分类" value="" />
            <el-option label="大气环境" value="AIR" />
            <el-option label="水环境" value="WATER" />
            <el-option label="土壤环境" value="SOIL" />
            <el-option label="噪声污染" value="NOISE" />
            <el-option label="生态保护" value="ECOLOGY" />
          </el-select>
        </div>
        <el-button class="reset-btn" @click="handleReset" :icon="RefreshRight" circle />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="state">
      <p>正在加载知识库...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="errorMsg" class="state error">
      <p class="err-title">加载失败</p>
      <p class="err-msg">{{ errorMsg }}</p>
      <el-button type="primary" @click="fetchData">重新加载</el-button>
    </div>

    <!-- 空数据 -->
    <div v-else-if="!knowledgeList || knowledgeList.length === 0" class="state">
      <p>暂无数据</p>
    </div>

    <!-- 数据列表 -->
    <div v-else class="content-card">
      <p class="count">共 {{ total || 0 }} 条文献</p>

      <div class="card-grid">
        <div
          v-for="doc in knowledgeList"
          :key="doc?.id"
          class="card"
          @click="doc?.id && $router.push(goDetail(doc.id))"
        >
          <!-- 封面 -->
          <div class="card-cover">
            <img v-if="doc?.coverImage" :src="doc.coverImage" :alt="doc?.title" />
            <div v-else class="cover-fallback">
              <span class="file-badge" :class="fileCls(doc?.attachmentUrl)">{{ fileCls(doc?.attachmentUrl) }}</span>
            </div>
            <span class="cover-cat">{{ catLabel(doc?.category) }}</span>
          </div>

          <!-- 正文 -->
          <div class="card-body">
            <h3 class="card-title">{{ doc?.title || '未命名文献' }}</h3>
            <p class="card-desc">{{ doc?.summary || '暂无文献摘要' }}</p>
          </div>

          <!-- 底部 -->
          <div class="card-foot">
            <span class="meta-item">{{ fmtDate(doc?.createTime) }}</span>
            <span class="meta-item">浏览 {{ doc?.viewCount || 0 }}</span>
            <span class="meta-actions">
              <el-button size="small" @click.stop="doc?.id && $router.push(goDetail(doc.id))">预览</el-button>
              <el-button size="small" type="primary" @click.stop="doc && downloadDoc(doc)">下载</el-button>
            </span>
          </div>
        </div>
      </div>

      <div v-if="total > size" class="pager">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="fetchData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgePage, downloadKnowledgeFile } from '@/api/knowledge'
import { rolePath } from '@/utils/roleRouter'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const knowledgeList = ref([])
const loading = ref(true)
const errorMsg = ref('')
const page = ref(1)
const size = ref(15)
const total = ref(0)
const filterType = ref('')
const searchQuery = ref('')

function goDetail(id) { return rolePath('/knowledge/' + id) }

function catLabel(cat) {
  const map = { AIR: '大气环境', WATER: '水环境', SOIL: '土壤环境', NOISE: '噪声污染', ECOLOGY: '生态保护' }
  return map[cat] || cat || '综合'
}
function fmtDate(t) { if (!t) return '-'; return String(t).replace('T', ' ').substring(0, 10) }

function fileCls(rawUrl) {
  if (!rawUrl) return 'DOC'
  let url = rawUrl
  try { const arr = JSON.parse(rawUrl); if (Array.isArray(arr) && arr.length > 0) url = arr[0] } catch {}
  const ext = url.split('.').pop()?.toLowerCase() || ''
  if (['pdf'].includes(ext)) return 'PDF'
  if (['xls', 'xlsx'].includes(ext)) return 'XLS'
  if (['doc', 'docx'].includes(ext)) return 'DOC'
  if (['zip', 'rar', '7z'].includes(ext)) return 'ZIP'
  return 'DOC'
}

function handleSearch() { page.value = 1; fetchData() }
function handleReset() { filterType.value = ''; searchQuery.value = ''; handleSearch() }

async function downloadDoc(doc) {
  if (!doc?.attachmentUrl) { ElMessage.warning('该文献暂无可下载的附件'); return }
  const urls = parseAttachUrls(doc.attachmentUrl)
  if (!urls || urls.length === 0) { ElMessage.warning('该文献暂无可下载的附件'); return }
  const url = typeof urls[0] === 'object' ? urls[0].url : urls[0]
  const filename = url?.split('/')?.pop() || doc?.title || 'download'
  try {
    const blob = await downloadKnowledgeFile(url, filename)
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob); a.download = filename; a.click()
    ElMessage.success('附件下载成功')
  } catch (e) { ElMessage.error('下载失败，附件可能不存在') }
}

function parseAttachUrls(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return [typeof arr === 'string' ? arr : (arr?.url || '')]
    return arr.filter(Boolean).map(item => typeof item === 'object' && item.url ? item.url : typeof item === 'string' ? item : '').filter(Boolean)
  } catch { return [raw] }
}

async function fetchData() {
  loading.value = true; errorMsg.value = ''
  try {
    const params = {}
    if (filterType.value) params.category = filterType.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const res = await getKnowledgePage(page.value, size.value, params)
    knowledgeList.value = res?.data || []
    total.value = res?.total || 0
  } catch (e) {
    console.error('加载失败:', e)
    errorMsg.value = e?.response?.data?.message || e?.message || '请求失败'
    knowledgeList.value = []; total.value = 0
  } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.root { padding: 24px; color: #1C2421; min-height: 100%; }

/* 顶部栏 — 毛玻璃 */
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24px 32px; margin-bottom: 20px;
  background: rgba(255,255,255,0.6); backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.8); border-radius: 24px;
  box-shadow: 0 8px 32px -8px rgba(28,36,33,0.04);
}
.top-left { display: flex; flex-direction: column; gap: 4px; }
.top-title { font-size: 22px; font-weight: 700; margin: 0; }
.top-sub { font-size: 13px; color: #74807B; margin: 0; }
.top-right { display: flex; align-items: center; gap: 10px; }

.search-box {
  background: rgba(255,255,255,0.5); padding: 5px 14px;
  border-radius: 100px; border: 1px solid rgba(255,255,255,0.7);
}
.s-input { width: 190px; }
.s-input :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; }

.filter-box { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.5); padding: 5px 14px; border-radius: 100px; border: 1px solid rgba(255,255,255,0.7); }
.f-label { font-size: 12px; font-weight: 600; color: #74807B; white-space: nowrap; }
.s-select { width: 120px; }
.s-select :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; }

.reset-btn { background: rgba(255,255,255,0.8); color: #74807B; border: none; }
.reset-btn:hover { background: #fff; color: #1C2421; }

/* 状态 */
.state { text-align: center; padding: 80px 20px; color: #74807B; }
.error { background: rgba(254,242,242,0.8); border-radius: 16px; color: #E11D48; }
.err-title { font-weight: 700; font-size: 16px; margin: 0 0 8px; }
.err-msg { color: #64748B; margin: 0 0 12px; }

/* 内容卡片容器 */
.content-card {
  background: rgba(255,255,255,0.5); backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.8); border-radius: 24px;
  padding: 24px 28px;
  box-shadow: 0 8px 32px -8px rgba(28,36,33,0.04);
}
.count { color: #74807B; font-size: 13px; margin: 0 0 20px; }

/* 卡片网格 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

/* 单张卡片 */
.card {
  background: #fff; border-radius: 16px; overflow: hidden;
  border: 1px solid rgba(0,0,0,0.04); cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px -8px rgba(0,0,0,0.1); }

/* 封面 */
.card-cover {
  position: relative; height: 150px; overflow: hidden;
  background: linear-gradient(135deg, #E8F5E9 0%, #E0F2F1 100%);
}
.card-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.file-badge {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800; background: #fff; box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.file-badge.PDF { color: #E11D48; }
.file-badge.DOC { color: #2563EB; }
.file-badge.XLS { color: #059669; }
.file-badge.ZIP { color: #D97706; }

.cover-cat {
  position: absolute; top: 12px; left: 12px;
  font-size: 11px; font-weight: 700; color: #2A483A;
  background: rgba(255,255,255,0.9); backdrop-filter: blur(8px);
  padding: 4px 10px; border-radius: 8px;
}

/* 正文 */
.card-body { padding: 20px 20px 0; }
.card-title {
  margin: 0 0 8px; font-size: 15px; font-weight: 600;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; line-height: 1.5;
}
.card-desc {
  margin: 0; color: #74807B; font-size: 13px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部 */
.card-foot {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 20px; margin-top: 14px;
  border-top: 1px solid rgba(0,0,0,0.04);
}
.meta-item { font-size: 12px; color: #94A3B8; }
.meta-actions { margin-left: auto; display: flex; gap: 6px; }

.pager { display: flex; justify-content: center; padding: 24px 0 4px; }
</style>
