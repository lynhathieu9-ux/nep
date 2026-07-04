<template>
  <div class="root">
    <!-- 加载中 -->
    <div v-if="loading" class="state">
      <p>正在加载文献...</p>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="errorMsg" class="state error">
      <p class="err-title">加载失败</p>
      <p>{{ errorMsg }}</p>
      <el-button @click="$router.back()">返回列表</el-button>
      <el-button type="primary" @click="fetchDetail">重新加载</el-button>
    </div>

    <!-- 文献不存在 -->
    <div v-else-if="!docData" class="state">
      <p>该文献已被撤档或不存在</p>
      <el-button @click="$router.back()">返回列表</el-button>
    </div>

    <!-- 文献内容 -->
    <div v-else class="paper">
      <!-- 返回按钮 -->
      <div class="back-bar">
        <el-button @click="$router.back()" text>← 返回知识库</el-button>
      </div>

      <!-- 文章卡片 -->
      <div class="paper-card">
        <!-- 封面图 -->
        <div class="paper-cover" v-if="docData?.coverImage">
          <img :src="docData.coverImage" :alt="docData?.title" />
        </div>

        <!-- 标题 -->
        <div class="paper-header">
          <div class="paper-code">档案编号：{{ String(docData?.id || '').padStart(6, '0') }}</div>
          <h1>{{ docData?.title || '未命名文献' }}</h1>
          <div class="paper-meta">
            <span class="tag">{{ catLabel(docData?.category) }}</span>
            <span>来源：{{ docData?.source || '东软环保公众监督系统' }}</span>
            <span>发布时间：{{ fmtDate(docData?.createTime) }}</span>
            <span>浏览 {{ docData?.viewCount || 0 }} 次</span>
          </div>
        </div>

        <!-- 摘要 -->
        <div class="paper-abstract" v-if="docData?.summary">
          <div class="abstract-label">内容提要</div>
          <p>{{ docData.summary }}</p>
        </div>

        <!-- 正文 -->
        <div class="paper-body" v-html="docData?.content || '<p style=color:#A0AAB2;text-align:center;padding:40px>暂无正文内容</p>'"></div>

        <!-- 附件 -->
        <div class="paper-attachments" v-if="attachments && attachments.length > 0">
          <div class="att-title">附件文件（{{ attachments.length }} 个）</div>
          <div class="att-row" v-for="(att, idx) in attachments" :key="idx">
            <span class="att-name">{{ att?.name || '未知文件' }}</span>
            <el-button size="small" type="primary" @click="att && downloadAttachment(att)">下载</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledgeById, downloadKnowledgeFile } from '@/api/knowledge'
import { ElMessage } from 'element-plus'

const route = useRoute()
const docData = ref(null)
const loading = ref(true)
const errorMsg = ref('')

const attachments = computed(() => {
  const doc = docData.value
  if (!doc || !doc?.attachmentUrl) return []
  return parseAttachments(doc.attachmentUrl)
})

function catLabel(cat) {
  if (!cat) return '综合文献'
  const map = { AIR: '大气环境', WATER: '水环境', SOIL: '土壤环境', NOISE: '噪声污染', ECOLOGY: '生态保护' }
  return map[cat] || cat
}

function fmtDate(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 10)
}

function parseAttachments(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter(Boolean).map(normalize) : [normalize(raw)]
  } catch { return [normalize(raw)] }
}

function normalize(item) {
  if (typeof item === 'object' && item?.url) return { url: item.url, name: item.name || getName(item.url) }
  if (typeof item === 'string') return { url: item, name: getName(item) }
  return { url: '', name: '未知文件' }
}

function getName(url) {
  if (!url) return '未知文件'
  const parts = url.replace(/\\/g, '/').split('/')
  return parts[parts.length - 1] || '下载文件'
}

async function downloadAttachment(att) {
  const url = att?.url || ''
  const name = att?.name || 'download'
  try {
    const blob = await downloadKnowledgeFile(url, name)
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob); a.download = name; a.click()
    ElMessage.success('附件下载成功')
  } catch (e) { ElMessage.error('下载失败') }
}

async function fetchDetail() {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await getKnowledgeById(route.params.id)
    docData.value = res?.data || null
  } catch (e) {
    console.error('详情加载失败:', e)
    errorMsg.value = e?.response?.data?.message || e?.message || '请求失败'
    docData.value = null
  } finally { loading.value = false }
}

onMounted(() => fetchDetail())
</script>

<style scoped>
.root { padding: 24px; color: #1C2421; min-height: 100%; }

/* 状态 */
.state { text-align: center; padding: 100px 20px; color: #74807B; }
.error { background: rgba(254,242,242,0.8); border-radius: 16px; color: #E11D48; }
.err-title { font-weight: 700; font-size: 16px; margin: 0 0 8px; }

/* 返回栏 */
.back-bar { margin-bottom: 20px; }

/* 文章卡片 */
.paper-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px -8px rgba(0,0,0,0.08);
  border: 1px solid rgba(0,0,0,0.04);
  overflow: hidden;
}

/* 封面 */
.paper-cover { border-radius: 16px 16px 0 0; overflow: hidden; }
.paper-cover img { width: 100%; max-height: 360px; object-fit: cover; display: block; }

/* 标题区 */
.paper-header {
  padding: 48px 64px 32px; text-align: center;
  border-bottom: 2px solid #1C2421;
}
.paper-code { font-family: monospace; font-size: 13px; color: #74807B; margin-bottom: 20px; letter-spacing: 1px; }
.paper-header h1 { font-size: 28px; font-weight: 700; margin: 0 0 20px; line-height: 1.4; }
.paper-meta { display: flex; justify-content: center; align-items: center; gap: 16px; font-size: 13px; color: #74807B; flex-wrap: wrap; }
.tag { background: rgba(42,72,58,0.06); color: #2A483A; padding: 4px 10px; border-radius: 6px; font-weight: 700; }

/* 摘要 */
.paper-abstract { margin: 32px 64px; background: #F8FAFC; padding: 24px; border-radius: 12px; font-size: 14px; color: #475569; line-height: 1.8; }
.abstract-label { font-weight: 700; color: #1C2421; margin-bottom: 8px; font-size: 15px; }
.paper-abstract p { margin: 0; }

/* 正文 */
.paper-body { padding: 32px 64px 48px; font-size: 16px; line-height: 2; }
.paper-body :deep(h2), .paper-body :deep(h3) { font-size: 20px; font-weight: 700; color: #1C2421; margin: 32px 0 16px; }
.paper-body :deep(p) { margin: 0 0 20px; }
.paper-body :deep(img) { max-width: 100%; border-radius: 12px; margin: 20px 0; }

/* 附件 */
.paper-attachments { margin: 0 64px 40px; padding-top: 24px; border-top: 1px dashed rgba(0,0,0,0.08); }
.att-title { font-size: 15px; font-weight: 700; color: #1C2421; margin-bottom: 14px; }
.att-row { display: flex; align-items: center; justify-content: space-between; background: #F4F6F5; padding: 12px 18px; border-radius: 10px; margin-bottom: 8px; }
.att-name { font-size: 13px; font-weight: 600; color: #1C2421; }

@media (max-width: 768px) {
  .paper-header { padding: 32px 24px; }
  .paper-header h1 { font-size: 22px; }
  .paper-abstract, .paper-body, .paper-attachments { margin-left: 24px; margin-right: 24px; }
}
</style>
