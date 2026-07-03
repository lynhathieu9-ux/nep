<template>
  <div class="premium-news-container">
    
    <!-- 头部：极简悬浮操作岛 -->
    <div class="control-island">
      <div class="island-brand">
        <div class="brand-icon"><el-icon><Reading /></el-icon></div>
        <div class="brand-text">
          <h2>环保资讯中枢</h2>
          <p>管理与发布全网环境动态、官方通告与政策指引</p>
        </div>
      </div>
      <div class="island-actions">
        <button class="apple-btn primary-btn" @click="showDialog(null)">
          <el-icon><Plus /></el-icon> 撰写新资讯
        </button>
      </div>
    </div>

    <!-- 核心表格区：极致圆润的白色穹顶卡片 -->
    <div class="data-dome">
      <el-table 
        :data="newsList" 
        v-loading="loading" 
        class="apple-table"
        :row-class-name="'apple-row'"
      >
        <el-table-column prop="id" label="档案编号" width="100">
          <template #default="{row}">
            <span class="mono-id">#{{ String(row.id).padStart(3, '0') }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="资讯主标题" min-width="280" show-overflow-tooltip>
          <template #default="{row}">
            <div class="title-cell">
              <div class="title-avatar">
                <img v-if="row.coverImage" :src="row.coverImage" alt="cover" />
                <el-icon v-else><PictureFilled /></el-icon>
              </div>
              <span class="premium-title">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="newsType" label="所属专栏" width="140">
          <template #default="{row}">
            <span class="capsule-tag" :class="row.newsType.toLowerCase()">
              {{ row.newsType === 'NEWS' ? '行业动态' : row.newsType === 'NOTICE' ? '官方通告' : '政策指引' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="展示状态" width="140">
          <template #default="{row}">
            <div class="status-badge" :class="row.status === 1 ? 'is-live' : row.status === 0 ? 'is-draft' : 'is-archived'">
              <span class="dot"></span>
              {{ row.status === 1 ? '线上公开' : row.status === 0 ? '草稿储藏' : '已归档' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="viewCount" label="触达人次" width="120">
          <template #default="{row}">
            <span class="mono-data">{{ row.viewCount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="right">
          <template #default="{row}">
            <div class="action-group">
              <el-tooltip content="编辑详情" placement="top" :show-after="300" effect="light">
                <button class="icon-action-btn" @click="showDialog(row)">
                  <el-icon><EditPen /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="永久销毁" placement="top" :show-after="300" effect="light">
                <button class="icon-action-btn danger" @click="handleDelete(row.id)">
                  <el-icon><Delete /></el-icon>
                </button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 悬浮分页器 -->
      <div class="pagination-footer" v-if="total > size">
        <el-pagination 
          v-model:current-page="page" 
          :page-size="size" 
          :total="total" 
          layout="prev, pager, next" 
          @current-change="fetchData" 
          class="apple-pagination" 
        />
      </div>
    </div>

    <!-- 撰写与编辑面板：高度定制的沉浸式表单 -->
    <el-dialog 
      v-model="dialogVisible" 
      width="760px" 
      destroy-on-close 
      class="premium-dialog"
      :show-close="false"
      align-center
    >
      <template #header>
        <div class="dialog-header">
          <div class="header-text">
            <h3>{{ isEdit ? '更新资讯档案' : '发布全新资讯' }}</h3>
            <p>{{ isEdit ? '修改已有内容的细节或变更发布状态' : '向全网用户传递极具价值的最新内容' }}</p>
          </div>
          <button class="close-btn" @click="dialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>

      <el-form :model="form" label-position="top" class="premium-form">
        <div class="form-row">
          <el-form-item label="资讯主标题" class="flex-2">
            <el-input v-model="form.title" placeholder="输入极具吸引力的主标题..." />
          </el-form-item>
          
          <el-form-item label="所属专栏" class="flex-1">
            <el-select v-model="form.newsType" style="width: 100%" popper-class="premium-popper">
              <el-option label="行业动态" value="NEWS" />
              <el-option label="官方通告" value="NOTICE" />
              <el-option label="政策指引" value="POLICY" />
            </el-select>
          </el-form-item>
        </div>
        
        <el-form-item label="内容导读 (摘要)">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="用一两句话精准勾勒核心大意..." />
        </el-form-item>
        
        <el-form-item label="正文详情">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="在此输入丰富的图文结构（支持 HTML 格式渲染）..." />
        </el-form-item>
        
        <el-form-item label="视觉焦点图 (Cover)">
          <div class="upload-bento">
            <div class="bento-input-group">
              <el-input v-model="form.coverImage" placeholder="粘贴图片URL，或直接从本地上传高质量图片">
                <template #prefix><el-icon><Link /></el-icon></template>
              </el-input>
              <el-upload :show-file-list="false" :before-upload="handleCoverUpload" accept="image/*">
                <button class="apple-btn secondary-btn" type="button" :disabled="uploading">
                  <el-icon v-if="!uploading" class="btn-icon"><Upload /></el-icon>
                  <el-icon v-else class="btn-icon is-loading"><Loading /></el-icon>
                  {{ uploading ? '上传中...' : '本地上传' }}
                </button>
              </el-upload>
            </div>
            
            <transition name="fade-slide">
              <div v-if="form.coverImage" class="bento-preview">
                <img :src="form.coverImage" alt="Focus Visual" />
                <div class="preview-mask">
                  <span class="mask-badge"><el-icon><Select /></el-icon> 视觉图已就绪</span>
                </div>
              </div>
            </transition>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <button class="apple-btn ghost-btn" @click="dialogVisible = false">放弃编辑</button>
          <button class="apple-btn primary-btn" @click="handleSave">确认发布 / 更新</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNewsPage, createNews, updateNews, deleteNews } from '@/api/news'
import { uploadImage } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Reading, Plus, EditPen, Delete, Close, Link, Upload, Loading, PictureFilled, Select 
} from '@element-plus/icons-vue'

const newsList = ref([])
const loading = ref(false)
const uploading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ title: '', newsType: 'NEWS', summary: '', content: '', coverImage: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getNewsPage(page.value, size.value)
    newsList.value = res.data || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}

function showDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row }
  } else {
    isEdit.value = false
    editId.value = null
    form.value = { title: '', newsType: 'NEWS', summary: '', content: '', coverImage: '' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (isEdit.value) {
      await updateNews(editId.value, form.value)
      ElMessage.success('档案更新成功')
    } else {
      await createNews(form.value)
      ElMessage.success('新资讯已发布')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要永久销毁这条资讯档案吗？此操作无法撤销。', '销毁确认', { 
      type: 'warning',
      customClass: 'premium-message-box',
      confirmButtonText: '确认销毁',
      cancelButtonText: '保留'
    })
    await deleteNews(id)
    ElMessage.success('档案已销毁')
    fetchData()
  } catch (e) {}
}

async function handleCoverUpload(file) {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('为保障加载性能，图片请勿超过 5MB')
    return false
  }
  uploading.value = true
  try {
    const res = await uploadImage(file)
    form.value.coverImage = res.data
    ElMessage.success('视觉图上传就绪')
  } catch (e) {
    ElMessage.error('上传受阻，请重试')
  } finally {
    uploading.value = false
  }
  return false 
}

onMounted(fetchData)
</script>

<style scoped>
/* =========================================================================
   极致圆润与高端质感 (Hyper-Rounded & Premium Aesthetic)
   ========================================================================= */

.premium-news-container {
  /* 彻底剥离背板颜色，让容器变为纯粹的布局骨架，完美融入外层框架 */
  background: transparent;
  padding: 24px 0 0 0; /* 仅保留顶部适当间距，消融边界感 */
  min-height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
  color: #1D1D1F;
}

/* ---------- 1. 操作岛 (Control Island) ---------- */
.control-island {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #FFFFFF;
  padding: 24px 32px;
  border-radius: 24px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.island-brand { display: flex; align-items: center; gap: 20px; }
.brand-icon {
  width: 52px; height: 52px;
  background: linear-gradient(135deg, #1D1D1F, #434344);
  color: #FFFFFF;
  border-radius: 16px;
  display: flex; justify-content: center; align-items: center;
  font-size: 24px;
  box-shadow: 0 8px 20px rgba(29, 29, 31, 0.2);
}
.brand-text h2 { margin: 0 0 6px 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }
.brand-text p { margin: 0; font-size: 13px; color: #86868B; font-weight: 500; }

/* ---------- 2. 交互按钮 (Fluid Buttons) ---------- */
.apple-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  padding: 12px 24px; border-radius: 14px; border: none;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); /* 物理弹簧 */
}
.primary-btn { background: #1D1D1F; color: #FFFFFF; }
.primary-btn:hover { background: #434344; transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.12); }
.primary-btn:active { transform: translateY(1px); }

.secondary-btn { background: #F1F5F9; color: #1D1D1F; padding: 12px 20px; }
.secondary-btn:hover { background: #E2E8F0; transform: translateY(-1px); }

.ghost-btn { background: transparent; color: #86868B; }
.ghost-btn:hover { background: #F1F5F9; color: #1D1D1F; }

/* ---------- 3. 穹顶数据表 (Data Dome) ---------- */
.data-dome {
  background: #FFFFFF;
  border-radius: 24px;
  padding: 12px 24px 24px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.03);
}

:deep(.apple-table) {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #FFFFFF;
  border-radius: 16px;
}
:deep(.apple-table .el-table__inner-wrapper::before) { display: none; }
:deep(.apple-table th.el-table__cell) {
  padding: 20px 0;
  border-bottom: 1px solid rgba(0,0,0,0.03) !important;
  font-size: 13px; font-weight: 600; color: #86868B;
}
:deep(.apple-table td.el-table__cell) {
  padding: 20px 0;
  border-bottom: 1px solid rgba(0,0,0,0.02) !important;
  transition: background 0.3s ease;
}
:deep(.apple-table__row:hover > td.el-table__cell) { background-color: #F8FAFC !important; }

/* 单元格微排版 */
.mono-id { font-family: "SF Pro Text", monospace; color: #94A3B8; font-weight: 600; }
.mono-data { font-family: "SF Pro Text", monospace; font-feature-settings: "tnum"; color: #475569; font-weight: 600; }

.title-cell { display: flex; align-items: center; gap: 12px; }
.title-avatar {
  width: 40px; height: 40px; border-radius: 10px; background: #F1F5F9; color: #94A3B8;
  display: flex; justify-content: center; align-items: center; font-size: 18px; overflow: hidden;
}
.title-avatar img { width: 100%; height: 100%; object-fit: cover; }
.premium-title { font-size: 15px; font-weight: 600; color: #1D1D1F; }

/* 胶囊标签 (Pill Tags) */
.capsule-tag {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600;
}
.capsule-tag.news { background: #F0F9FF; color: #0284C7; }
.capsule-tag.notice { background: #FFFBEB; color: #D97706; }
.capsule-tag.policy { background: #F0FDF4; color: #059669; }

/* 呼吸脉冲状态 */
.status-badge { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.status-badge .dot { width: 8px; height: 8px; border-radius: 50%; }
.is-live { color: #10B981; }
.is-live .dot { background: #10B981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2); }
.is-draft { color: #64748B; }
.is-draft .dot { background: #94A3B8; box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.2); }
.is-archived { color: #EF4444; }
.is-archived .dot { background: #EF4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2); }

/* 图标级按键 */
.action-group { display: flex; justify-content: flex-end; gap: 8px; padding-right: 12px; }
.icon-action-btn {
  width: 36px; height: 36px; border-radius: 12px; border: none;
  background: #F1F5F9; color: #64748B; cursor: pointer;
  display: flex; justify-content: center; align-items: center; font-size: 16px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.icon-action-btn:hover { background: #1D1D1F; color: #FFFFFF; transform: scale(1.08) translateY(-2px); }
.icon-action-btn.danger:hover { background: #FEF2F2; color: #EF4444; }

/* 分页器 */
.pagination-footer { margin-top: 32px; display: flex; justify-content: center; }
:deep(.apple-pagination .el-pager li) {
  background: transparent !important; font-weight: 600; color: #86868B;
  border-radius: 10px; min-width: 36px; height: 36px; line-height: 36px;
}
:deep(.apple-pagination .el-pager li.is-active) { background: #1D1D1F !important; color: #FFF; }
:deep(.apple-pagination .btn-prev), :deep(.apple-pagination .btn-next) { background: transparent !important; border-radius: 10px; }

/* ---------- 4. iOS 沉浸式表单与弹窗 ---------- */
:deep(.premium-dialog) {
  border-radius: 28px;
  box-shadow: 0 32px 80px rgba(0,0,0,0.15);
  overflow: hidden; padding: 0; background: #FFFFFF;
}
:deep(.premium-dialog .el-dialog__header) { display: none; }
:deep(.premium-dialog .el-dialog__body) { padding: 0; }
:deep(.premium-dialog .el-dialog__footer) { padding: 0; }

.dialog-header {
  padding: 32px 40px;
  display: flex; justify-content: space-between; align-items: flex-start;
  border-bottom: 1px solid rgba(0,0,0,0.03); background: #FAFAFC;
}
.header-text h3 { margin: 0 0 8px 0; font-size: 24px; font-weight: 700; color: #1D1D1F; letter-spacing: -0.5px; }
.header-text p { margin: 0; font-size: 14px; color: #86868B; font-weight: 500; }
.close-btn {
  width: 36px; height: 36px; border-radius: 18px; border: none; background: #E2E8F0; color: #64748B;
  display: flex; justify-content: center; align-items: center; cursor: pointer; font-size: 16px;
  transition: all 0.2s;
}
.close-btn:hover { background: #CBD5E1; color: #1D1D1F; transform: rotate(90deg); }

.premium-form { padding: 32px 40px; }
.form-row { display: flex; gap: 24px; }
.flex-2 { flex: 2; } .flex-1 { flex: 1; }

:deep(.premium-form .el-form-item__label) {
  font-size: 13px; font-weight: 600; color: #475569; padding-bottom: 8px;
}
:deep(.premium-form .el-input__wrapper),
:deep(.premium-form .el-textarea__inner) {
  background-color: #F8FAFC;
  box-shadow: none !important;
  border-radius: 14px;
  padding: 12px 16px;
  border: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  color: #1D1D1F; font-size: 14px;
}
:deep(.premium-form .el-textarea__inner) { font-family: inherit; }
:deep(.premium-form .el-input__wrapper:hover),
:deep(.premium-form .el-textarea__inner:hover) { background-color: #F1F5F9; }
:deep(.premium-form .el-input__wrapper.is-focus),
:deep(.premium-form .el-textarea__inner:focus) {
  background-color: #FFFFFF;
  border-color: #1D1D1F;
  box-shadow: 0 0 0 4px rgba(29, 29, 31, 0.08) !important;
}

/* 封面上传便当盒 (Bento Box) */
.upload-bento { display: flex; flex-direction: column; gap: 16px; }
.bento-input-group { display: flex; gap: 12px; }
.bento-preview {
  position: relative; width: 100%; height: 220px; border-radius: 16px; overflow: hidden;
  background: #F1F5F9; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.04);
}
.bento-preview img { width: 100%; height: 100%; object-fit: cover; }
.preview-mask {
  position: absolute; bottom: 0; left: 0; right: 0; padding: 20px;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
}
.mask-badge { display: inline-flex; align-items: center; gap: 6px; background: rgba(255,255,255,0.2); backdrop-filter: blur(8px); color: #FFF; padding: 6px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }

.dialog-footer {
  padding: 24px 40px; background: #FAFAFC; border-top: 1px solid rgba(0,0,0,0.03);
  display: flex; justify-content: flex-end; gap: 12px;
}

/* 动效 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(10px) scale(0.98); }
</style>

<style>
/* 重写下拉框与全局 MessageBox 质感 */
.premium-popper {
  border-radius: 16px !important;
  box-shadow: 0 16px 40px rgba(0,0,0,0.1) !important;
  border: 1px solid rgba(0,0,0,0.04) !important;
  padding: 8px !important;
}
.premium-message-box {
  border-radius: 24px !important;
  padding: 32px !important;
  border: none !important;
  box-shadow: 0 32px 80px rgba(0,0,0,0.12) !important;
}
.premium-message-box .el-message-box__btns .el-button--primary {
  background: #1D1D1F !important;
  border: none !important;
  border-radius: 10px !important;
  padding: 10px 20px !important;
  height: auto !important;
}
</style>