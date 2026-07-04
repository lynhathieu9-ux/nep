<template>
  <div class="manage-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h3>📚 文献发布管理</h3>
        <span class="header-sub">发布环保知识、规章条例、作业标准等文献，支持附件上传</span>
      </div>
      <el-button type="primary" size="large" @click="openDialog(null)">
        <el-icon><Plus /></el-icon> 发布文献
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="list" v-loading="loading" stripe class="knowledge-table">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="title" label="文献标题" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="title-link" @click="previewArticle(row)">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="catTagType(row.category)" effect="light">
            {{ catLabel(row.category) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="120" show-overflow-tooltip />
      <el-table-column prop="viewCount" label="浏览" width="75" align="center" sortable />
      <el-table-column prop="status" label="状态" width="85" align="center">
        <template #default="{ row }">
          <span class="status-dot" :class="row.status === 1 ? 'published' : 'draft'"></span>
          {{ row.status === 1 ? '已发布' : '草稿' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="130" align="center">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" link @click="previewArticle(row)">预览</el-button>
          <el-popconfirm title="确定删除该文献？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-box" v-if="total > size">
      <el-pagination
        v-model:current-page="page" :page-size="size" :total="total"
        layout="total, prev, pager, next" @current-change="fetchList" background
      />
    </div>

    <!-- ==================== 发布 / 编辑对话框 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑文献' : '发布新文献'"
      width="820px"
      destroy-on-close
      top="4vh"
      class="knowledge-dialog"
    >
      <el-form :model="form" label-width="80px" label-position="right">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="文献标题" required>
              <el-input v-model="form.title" placeholder="请输入文献标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" required>
              <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
                <el-option label="大气环境" value="AIR" />
                <el-option label="水环境" value="WATER" />
                <el-option label="土壤环境" value="SOIL" />
                <el-option label="噪声污染" value="NOISE" />
                <el-option label="生态保护" value="ECOLOGY" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="来源/作者">
              <el-input v-model="form.source" placeholder="如：中国环境科学研究院" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio-button :value="1">发布</el-radio-button>
                <el-radio-button :value="0">存为草稿</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="内容摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简要概述文献内容，将展示在列表页" maxlength="500" show-word-limit />
        </el-form-item>

        <!-- 封面图上传 -->
        <el-form-item label="封面图片">
          <div class="cover-section">
            <!-- 已上传预览 -->
            <div class="cover-preview" v-if="form.coverImage">
              <img :src="form.coverImage" alt="封面预览" />
              <div class="cover-actions">
                <el-button size="small" circle @click="form.coverImage = ''">
                  <el-icon><Delete /></el-icon>
                </el-button>
                <span class="cover-hint">封面已就绪</span>
              </div>
            </div>
            <!-- 未上传 -->
            <div class="cover-upload-area" v-else>
              <el-upload
                :show-file-list="false"
                :before-upload="handleCoverUpload"
                accept="image/*"
                drag
              >
                <el-icon :size="32"><Plus /></el-icon>
                <span class="upload-text">点击或拖拽上传封面图</span>
                <span class="upload-sub">JPG / PNG / WebP，建议 1200×630，≤ 5MB</span>
              </el-upload>
            </div>
          </div>
        </el-form-item>

        <!-- 正文内容 -->
        <el-form-item label="正文内容">
          <el-input v-model="form.content" type="textarea" :rows="10"
            placeholder="支持 HTML 格式。可粘贴图文内容，或直接编写 HTML 代码。" />
        </el-form-item>

        <!-- 附件上传（支持多个） -->
        <el-form-item label="附件文件">
          <div class="attachment-section">
            <!-- 已上传附件列表 -->
            <div class="attachment-list" v-if="attachList.length > 0">
              <div class="attach-row" v-for="(url, idx) in attachList" :key="idx">
                <el-icon class="attach-row-icon"><Link /></el-icon>
                <span class="attach-row-name">{{ urlToName(url) }}</span>
                <el-button size="small" type="danger" link @click="removeAttachment(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <!-- 上传按钮 -->
            <el-upload
              :show-file-list="false"
              :before-upload="handleAttachUpload"
              :disabled="uploadingAttach"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.zip,.rar,.7z,.txt,.html"
            >
              <el-button size="small" :loading="uploadingAttach" type="primary" plain>
                <el-icon><Upload /></el-icon>
                {{ attachList.length > 0 ? '继续添加附件' : '上传附件' }}
              </el-button>
            </el-upload>
            <span class="upload-hint">支持 PDF / Word / Excel / ZIP 等，上限 50MB · 可上传多个</span>
          </div>
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="tagsInput" placeholder="多个标签以逗号分隔，如：PM2.5, 防护指南, 大气污染" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? '更新文献' : '发布文献' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Upload, Link, Delete } from '@element-plus/icons-vue'
import { getKnowledgePage, createKnowledge, updateKnowledge, deleteKnowledge } from '@/api/knowledge'
import { uploadImage, uploadDoc } from '@/api/file'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const tagsInput = ref('')
const uploadingAttach = ref(false)

const emptyForm = () => ({
  title: '', category: 'AIR', source: '', status: 1,
  summary: '', content: '', coverImage: '', attachmentUrl: '', tags: ''
})
const form = ref(emptyForm())

// 附件列表：{url, name} 对象数组
const attachList = ref([])

function urlToName(att) {
  // att 可能是 {url, name} 对象，也可能是旧格式的纯URL字符串
  if (typeof att === 'object' && att.name) return att.name
  if (typeof att === 'string') {
    const parts = att.replace(/\\/g, '/').split('/')
    return parts[parts.length - 1] || att
  }
  return '未知文件'
}

function urlToValue(att) {
  return typeof att === 'object' ? att.url : att
}

// ============ 分类 ============
function catLabel(cat) {
  const map = { AIR: '大气环境', WATER: '水环境', SOIL: '土壤环境', NOISE: '噪声污染', ECOLOGY: '生态保护' }
  return map[cat] || cat || '-'
}
function catTagType(cat) {
  const map = { AIR: 'primary', WATER: 'info', SOIL: 'warning', NOISE: 'danger', ECOLOGY: 'success' }
  return map[cat] || 'info'
}

// ============ 格式化 ============
function fmt(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 10)
}

// ============ 数据加载 ============
async function fetchList() {
  loading.value = true
  try {
    const res = await getKnowledgePage(page.value, size.value)
    list.value = res.data || []
    total.value = res.total || 0
  } catch (e) { /* 静默 */ } finally { loading.value = false }
}

// ============ 对话框 ============
function openDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = {
      title: row.title || '',
      category: row.category || 'AIR',
      source: row.source || '',
      status: row.status ?? 1,
      summary: row.summary || '',
      content: row.content || '',
      coverImage: row.coverImage || '',
      attachmentUrl: row.attachmentUrl || '',
      tags: row.tags || ''
    }
    tagsInput.value = parseTags(row.tags)
    // 解析已有附件为数组
    attachList.value = parseAttachArray(row.attachmentUrl)
  } else {
    isEdit.value = false
    editId.value = null
    form.value = emptyForm()
    tagsInput.value = ''
    attachList.value = []
  }
  dialogVisible.value = true
}

/** 解析 attachmentUrl（JSON 数组 或 单个URL）为数组 */
function parseAttachArray(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.filter(Boolean) : [raw]
  } catch {
    return [raw]
  }
}

function parseTags(tagsStr) {
  if (!tagsStr) return ''
  try {
    const arr = JSON.parse(tagsStr)
    return Array.isArray(arr) ? arr.join(', ') : tagsStr
  } catch {
    return tagsStr.replace(/[\[\]"]/g, '')
  }
}

// ============ 图片上传 ============
async function handleCoverUpload(file) {
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片大小不能超过5MB'); return false }
  try {
    const res = await uploadImage(file)
    form.value.coverImage = res.data
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
  return false // 阻止 el-upload 默认上传
}

// ============ 附件上传 ============
async function handleAttachUpload(file) {
  if (file.size > 50 * 1024 * 1024) { ElMessage.warning('附件大小不能超过50MB'); return false }
  uploadingAttach.value = true
  try {
    const res = await uploadDoc(file)
    // 保存 {url, name} 对象，保留原始文件名
    attachList.value.push({ url: res.data, name: file.name })
    ElMessage.success('附件上传成功')
  } catch (e) {
    ElMessage.error('附件上传失败')
  } finally {
    uploadingAttach.value = false
  }
  return false
}

function removeAttachment(idx) {
  attachList.value.splice(idx, 1)
}

// ============ 保存 / 删除 ============
async function handleSave() {
  if (!form.value.title.trim()) { ElMessage.warning('请输入文献标题'); return }
  saving.value = true
  try {
    const payload = { ...form.value }
    // 附件数组序列化为 JSON 字符串
    payload.attachmentUrl = attachList.value.length > 0
      ? JSON.stringify(attachList.value) : ''
    // 处理标签
    if (tagsInput.value.trim()) {
      const arr = tagsInput.value.split(/[,，]/).map(t => t.trim()).filter(Boolean)
      payload.tags = JSON.stringify(arr)
    } else {
      payload.tags = '[]'
    }
    if (isEdit.value) {
      await updateKnowledge(editId.value, payload)
      ElMessage.success('文献更新成功')
    } else {
      await createKnowledge(payload)
      ElMessage.success('文献发布成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteKnowledge(id)
    ElMessage.success('文献已删除')
    fetchList()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ============ 预览 ============
function previewArticle(row) {
  router.push(`/admin/knowledge/${row.id}`)
}

onMounted(fetchList)
</script>

<style scoped>
.manage-page {
  padding: 24px 28px; height: 100%;
  display: flex; flex-direction: column; gap: 20px;
  overflow-y: auto;
  background: linear-gradient(180deg, #F8FAFC 0%, #F4F6F5 100%);
}
.manage-page::-webkit-scrollbar { display: none; }

/* 页头 */
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24px 32px; flex-shrink: 0;
  background: rgba(255,255,255,0.7); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.9); border-radius: 20px;
  box-shadow: 0 4px 24px -6px rgba(0,0,0,0.04);
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.header-left h3 { margin: 0; font-size: 22px; font-weight: 700; color: #1C2421; letter-spacing: 0.3px; }
.header-sub { font-size: 13px; color: #74807B; font-weight: 500; }

/* 表格 */
.knowledge-table {
  flex: 1; min-height: 0;
  background: rgba(255,255,255,0.7); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.9); border-radius: 20px;
  box-shadow: 0 4px 24px -6px rgba(0,0,0,0.04);
  padding: 8px 12px;
}

/* 表头美化 */
.knowledge-table :deep(.el-table__header) { }
.knowledge-table :deep(.el-table__header th) {
  background: rgba(28,36,33,0.02); color: #74807B; font-weight: 600; font-size: 12px;
  letter-spacing: 0.3px; border-bottom: 1px solid rgba(28,36,33,0.06);
}

/* 行美化 */
.knowledge-table :deep(.el-table__body) { cursor: default; }
.knowledge-table :deep(.el-table__body tr) {
  transition: background 0.2s;
}
.knowledge-table :deep(.el-table__body tr:hover) {
  background: rgba(42,72,58,0.03);
}

/* 单元格 */
.knowledge-table :deep(.el-table__body td) {
  border-bottom: 1px solid rgba(28,36,33,0.03);
  padding: 14px 0;
}

/* stripe 美化 */
.knowledge-table :deep(.el-table__body tr.el-table__row--striped) {
  background: rgba(28,36,33,0.015);
}

.title-link {
  color: #1C2421; font-weight: 600; cursor: pointer;
  transition: color 0.2s, transform 0.2s;
}
.title-link:hover { color: #2A483A; }

.status-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  margin-right: 5px; vertical-align: middle;
  transition: all 0.3s;
}
.status-dot.published { background: #2AA876; box-shadow: 0 0 0 3px rgba(42,168,118,0.12); }
.status-dot.draft { background: #A0AAB2; }

.pagination-box { display: flex; justify-content: center; padding: 12px 0 4px; }

/* ===== 对话框内样式 ===== */
.cover-section {  }
.cover-preview { display: flex; align-items: flex-start; gap: 16px; }
.cover-preview img {
  width: 260px; height: 148px; border-radius: 12px;
  object-fit: cover; border: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.cover-actions { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.cover-hint { font-size: 12px; color: #2AA876; font-weight: 600; }

.cover-upload-area { width: 100%; }
:deep(.cover-upload-area .el-upload) { width: 100%; }
:deep(.cover-upload-area .el-upload-dragger) {
  width: 100%; height: 120px; border: 2px dashed rgba(28,36,33,0.12);
  border-radius: 14px; background: rgba(28,36,33,0.01);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  transition: all 0.25s;
}
:deep(.cover-upload-area .el-upload-dragger:hover) {
  border-color: #2A483A; background: rgba(42,72,58,0.03); transform: translateY(-2px);
}
.upload-text { font-size: 13px; font-weight: 500; color: #74807B; }
.upload-sub { font-size: 11px; color: #A0AAB2; }

.attachment-section { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.current-attachment {
  display: flex; align-items: center; gap: 8px;
  background: #F4F6F5; padding: 6px 12px; border-radius: 8px;
  font-size: 13px; color: #2A483A;
}
.att-filename { font-family: monospace; font-weight: 500; }
.upload-hint { font-size: 12px; color: #A0AAB2; }

/* 覆盖 el-dialog 顶部间距 */
:deep(.knowledge-dialog .el-dialog__body) { padding-top: 8px; }

/* el-tag 分类美化 */
.knowledge-table :deep(.el-tag) { border-radius: 8px; font-weight: 600; padding: 3px 10px; letter-spacing: 0.3px; }
</style>
