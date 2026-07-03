<template>
  <div class="page-root">
    <h2 class="page-title">环保知识库</h2>
    <p class="page-sub">系统规范、作业指导与环保法规全案检索</p>

    <div class="toolbar">
      <el-input v-model="searchQuery" placeholder="搜索文献题名..." clearable class="search-inp" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="filterType" placeholder="全部分类" clearable class="cat-sel" @change="handleSearch">
        <el-option label="全部分类" value="" />
        <el-option label="大气环境" value="AIR" />
        <el-option label="水环境" value="WATER" />
        <el-option label="土壤环境" value="SOIL" />
        <el-option label="噪声污染" value="NOISE" />
        <el-option label="生态保护" value="ECOLOGY" />
      </el-select>
      <el-button @click="handleReset" :icon="RefreshRight" circle />
    </div>

    <div v-if="loading" class="state-wrap"><p>加载中...</p></div>

    <div v-else-if="errorMsg" class="state-wrap err">
      <p class="err-title">加载失败</p>
      <p>{{ errorMsg }}</p>
      <el-button type="primary" @click="fetchData">重新加载</el-button>
    </div>

    <div v-else>
      <p class="total-hint">共 {{ total }} 条文献</p>
      <div v-for="doc in knowledgeList" :key="doc.id" class="card" @click="$router.push(goDetail(doc.id))">
        <h3 class="card-title">{{ doc.title }}</h3>
        <p class="card-desc">{{ doc.summary || '暂无摘要' }}</p>
        <div class="card-meta">
          <span>分类：{{ catLabel(doc.category) }}</span>
          <span>浏览：{{ doc.viewCount || 0 }} 次</span>
          <span>发布：{{ fmtDate(doc.createTime) }}</span>
        </div>
      </div>
      <p v-if="knowledgeList.length === 0" class="state-wrap">暂无数据</p>
      <div v-if="total > size" class="pager-box">
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
function handleSearch() { page.value = 1; fetchData() }
function handleReset() { filterType.value = ''; searchQuery.value = ''; handleSearch() }

async function downloadDoc(doc) {
  if (!doc.attachmentUrl) { ElMessage.warning('无附件'); return }
  const urls = parseAttachUrls(doc.attachmentUrl)
  if (urls.length === 0) { ElMessage.warning('无附件'); return }
  const url = urls[0]
  try {
    const blob = await downloadKnowledgeFile(url, url.split('/').pop())
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob); a.download = url.split('/').pop(); a.click()
    ElMessage.success('下载成功')
  } catch (e) { ElMessage.error('下载失败') }
}

function parseAttachUrls(raw) {
  if (!raw) return []
  try { const arr = JSON.parse(raw); return Array.isArray(arr) ? arr.filter(Boolean) : [raw] } catch { return [raw] }
}

async function fetchData() {
  loading.value = true; errorMsg.value = ''
  try {
    const params = {}
    if (filterType.value) params.category = filterType.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const res = await getKnowledgePage(page.value, size.value, params)
    knowledgeList.value = res.data || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载失败:', e)
    errorMsg.value = e?.response?.data?.message || e?.message || '请求失败'
    knowledgeList.value = []; total.value = 0
  } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-root { padding: 24px; color: #1C2421; height: 100%; overflow-y: auto; }
.page-title { font-size: 22px; font-weight: 700; margin: 0 0 4px 0; }
.page-sub { color: #74807B; font-size: 13px; margin: 0 0 20px 0; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.search-inp { width: 260px; }
.cat-sel { width: 150px; }
.state-wrap { text-align: center; padding: 60px 20px; color: #64748B; }
.state-wrap.err { background: #FEF2F2; border: 1px solid #FECDD3; border-radius: 12px; }
.err-title { color: #E11D48; font-weight: 600; margin: 0 0 8px 0; }
.total-hint { color: #64748B; font-size: 13px; margin-bottom: 12px; }
.card { background: white; padding: 16px 20px; margin-bottom: 10px; border-radius: 12px; border: 1px solid #E2E8F0; cursor: pointer; }
.card:hover { background: #F8FAFC; }
.card-title { margin: 0 0 6px 0; font-size: 16px; }
.card-desc { margin: 0; color: #64748B; font-size: 13px; }
.card-meta { margin-top: 8px; display: flex; gap: 16px; font-size: 12px; color: #94A3B8; }
.pager-box { text-align: center; margin-top: 20px; }
</style>
