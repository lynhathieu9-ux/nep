<template>
  <div class="swiss-page-container">
    
    <header class="action-console glass-panel fixed-section">
      <div class="console-left">
        <h2 class="page-title">反馈工单管理</h2>
        <span class="page-subtitle">公众监督反馈的统一调度与全生命周期追踪</span>
      </div>

      <div class="console-right">
        <div class="search-capsule" :class="{ 'is-active': statusFilter }">
          <span class="filter-label">工单状态</span>
          <el-select v-model="statusFilter" placeholder="全部工单" clearable class="swiss-select" popper-class="swiss-select-dropdown" @change="handleFilterChange">
            <el-option label="全部工单" value="" />
            <el-option label="待指派" value="PENDING" />
            <el-option label="已指派" value="ASSIGNED" />
            <el-option label="检测中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="超时预警" value="ESCALATED" />
          </el-select>
        </div>
        <el-button class="swiss-btn icon-btn" @click="refreshData" title="刷新列表">
          <el-icon :class="{ 'is-spinning': refreshing }"><RefreshRight /></el-icon>
        </el-button>
      </div>
    </header>

    <transition name="slide-down">
      <div v-if="selectedIds.length > 0" class="batch-console glass-panel">
        <div class="batch-left">
          <div class="selection-count">{{ selectedIds.length }}</div>
          <span class="batch-text">个工单已选中</span>
        </div>
        <div class="batch-right">
          <!-- 问题①：批量派送改为按网格员姓名选择 -->
          <el-select v-model="batchInspectorName" placeholder="选择网格员" filterable style="width:180px">
            <el-option v-for="ins in inspectorOptions" :key="ins.id"
                       :label="ins.realName + (ins.employeeCode ? ('（'+ins.employeeCode+'）') : '')"
                       :value="ins.realName" />
          </el-select>
          <button class="action-btn primary" @click="handleBatchAssign">
            <el-icon><Position /></el-icon> 批量派送
          </button>
          <button class="action-btn outline" @click="selectedIds = []">取消选中</button>
        </div>
      </div>
    </transition>

    <main class="roster-workspace stretch-section glass-panel" v-loading="loading">
      
      <div class="roster-header-row roster-grid">
        <div class="col-checkbox">
          <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" />
        </div>
        <div class="col-id">工单编号</div>
        <div class="col-supervisor">提报人</div>
        <div class="col-address">隐患地址</div>
        <div class="col-aqi">预估 AQI</div>
        <div class="col-status">工单状态</div>
        <div class="col-action">操作</div>
      </div>

      <div class="roster-scroll-area">
        
        <div v-if="feedbacks.length === 0 && !loading" class="empty-state">
          <el-icon class="empty-icon"><Tickets /></el-icon>
          <p>当前视图下没有任何监督工单</p>
        </div>

        <div class="roster-list" v-else>
          <div 
            class="roster-row roster-grid" 
            v-for="row in feedbacks" 
            :key="row.id"
            :class="{ 'is-timeout': isTimeout(row) }"
          >
            
            <div class="col-checkbox" @click.stop>
              <el-checkbox v-model="selectedIds" :label="row.id" :value="row.id"><span style="display:none"></span></el-checkbox>
            </div>

            <div class="col-id">
              <span class="mono-id">TKT-{{ String(row.id).padStart(4, '0') }}</span>
              <div v-if="isTimeout(row)" class="timeout-pulse" title="已超时 24 小时未处理"></div>
            </div>

            <div class="col-supervisor">
              <div class="user-pill"><el-icon><UserFilled /></el-icon> {{ row.supervisorId }}</div>
            </div>

            <div class="col-address">
              <el-tooltip :content="row.specificAddress" placement="top" popper-class="alpine-tooltip" :show-after="300">
                <span class="address-text">{{ row.specificAddress }}</span>
              </el-tooltip>
            </div>

            <div class="col-aqi">
              <span class="aqi-level-badge" :class="getAqiClass(row.estimatedAqiLevel)">
                L{{ row.estimatedAqiLevel }}
              </span>
            </div>

            <div class="col-status">
              <div class="status-cell">
                <span class="status-pill" :class="row.status.toLowerCase()">
                  <span class="pill-dot"></span>
                  {{ getStatusText(row) }}
                </span>
                <span class="status-inspector" v-if="row.assignedInspectorId && (row.status === 'ASSIGNED' || row.status === 'PROCESSING')">
                  {{ inspectorNameById(row.assignedInspectorId) }}
                </span>
              </div>
            </div>

            <div class="col-action" @click.stop>
              <!-- 待指派 / 超时预警：选人 + 指派 + 推荐 -->
              <template v-if="row.status === 'PENDING' || row.status === 'ESCALATED'">
                <el-select v-model="row._inspectorName" :placeholder="row.status === 'ESCALATED' ? '紧急指派' : '选择网格员'"
                           filterable size="small" class="inline-select">
                  <el-option v-for="ins in inspectorOptions" :key="ins.id"
                             :label="ins.realName" :value="ins.realName" />
                </el-select>
                <div class="action-btn-group">
                  <button class="assign-btn" title="确认指派" @click="handleAssign(row)">
                    <el-icon><Position /></el-icon>
                    <span>指派</span>
                  </button>
                  <button class="recommend-btn" title="智能推荐最合适的网格员" @click="openRecommend(row)">
                    <el-icon><MagicStick /></el-icon>
                  </button>
                </div>
              </template>

              <!-- 已指派 / 检测中：转派 -->
              <template v-else-if="row.status === 'ASSIGNED' || row.status === 'PROCESSING'">
                <button class="transfer-btn" @click="openTransfer(row)">
                  <el-icon><Switch /></el-icon>
                  <span>转派</span>
                </button>
              </template>

              <!-- 已完成 -->
              <span v-else class="action-done">-</span>

              <!-- 通用：备注 -->
              <button class="note-btn" title="工单备注" @click="openNotes(row)">
                <el-icon><ChatDotRound /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="list-pagination" v-if="total > size">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="prev, pager, next" class="swiss-pagination" @current-change="fetch" />
      </div>
    </main>

    <!-- ====== 智能推荐网格员 ====== -->
    <el-dialog v-model="recommendVisible" width="600px" class="recommend-dialog" destroy-on-close :close-on-click-modal="false"
               :show-close="false">
      <template #header>
        <div class="rec-header">
          <div class="rec-header-left">
            <div class="rec-icon-box">
              <el-icon :size="20"><MagicStick /></el-icon>
            </div>
            <div>
              <div class="rec-title">智能推荐指派</div>
              <div class="rec-subtitle">系统根据网格区域和负载自动推荐最佳人选</div>
            </div>
          </div>
          <button class="rec-close" @click="recommendVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
      </template>

      <div class="rec-body" v-loading="recommendLoading">
        <!-- 策略标签 -->
        <div class="rec-strategy" v-if="recommendType">
          <div class="strategy-badge" :class="recommendType.toLowerCase()">
            <span class="strategy-dot"></span>
            {{ recommendTypeText }}
          </div>
          <span class="strategy-desc">本地优先 → 就近异地 → 全局兜底</span>
        </div>

        <!-- 空态 -->
        <div v-if="!recommendLoading && recommendList.length === 0" class="rec-empty">
          <el-icon :size="36"><UserFilled /></el-icon>
          <p>当前区域暂无可用网格员</p>
          <span>建议扩大搜索范围或手动指派其他区域网格员</span>
        </div>

        <!-- 推荐列表 -->
        <div class="rec-list" v-else-if="!recommendLoading">
          <div class="rec-card" v-for="(ins, idx) in recommendList" :key="ins.inspectorId"
               :style="{ animationDelay: (idx * 0.06) + 's' }">
            <div class="rec-rank">{{ idx + 1 }}</div>
            <div class="rec-avatar">{{ (ins.realName || '网')[0] }}</div>
            <div class="rec-info">
              <div class="rec-name">{{ ins.realName || ('网格员 #' + ins.inspectorId) }}</div>
              <div class="rec-meta">
                <span v-if="ins.employeeCode">工号 {{ ins.employeeCode }}</span>
                <span v-if="ins.phone"> · {{ ins.phone }}</span>
                <span v-if="ins.cityName"> · {{ ins.cityName }}</span>
              </div>
            </div>
            <button class="rec-assign-btn" @click="handleRecommendAssign(ins.inspectorId)">
              <el-icon><Position /></el-icon>
              <span>指派</span>
            </button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="工单转派" width="440px" class="swiss-dialog" destroy-on-close :close-on-click-modal="false">
      <div class="dialog-form-content">
        <div class="info-row">
          <span class="info-label">原负责网格员</span>
          <span class="info-value">{{ inspectorNameById(currentTransferRow?.assignedInspectorId) }}</span>
        </div>
        <div class="input-group">
          <label>转派给</label>
          <!-- 以姓名为主选择新任网格员 -->
          <el-select v-model="transferTargetName" placeholder="选择新任网格员" filterable style="width:100%">
            <el-option v-for="ins in inspectorOptions" :key="ins.id"
                       :label="ins.realName" :value="ins.realName">
              <span>{{ ins.realName }}</span>
              <span style="float:right;color:#95a5a6;font-size:12px">{{ ins.employeeCode || ('ID:'+ins.id) }}</span>
            </el-option>
          </el-select>
        </div>
      </div>
      <template #footer>
        <div class="dialog-actions">
          <button class="action-btn ghost" @click="transferVisible = false">取消</button>
          <button class="action-btn primary" @click="handleTransfer"><el-icon><Switch /></el-icon> 确认转派</button>
        </div>
      </template>
    </el-dialog>

    <!-- ====== 工单备忘录 ====== -->
    <el-dialog v-model="notesVisible" width="580px" class="notes-dialog" destroy-on-close :close-on-click-modal="false"
               :show-close="false">
      <template #header>
        <div class="notes-dialog-header">
          <div class="notes-header-left">
            <div class="notes-icon-box">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </div>
            <div>
              <div class="notes-title">工单备注</div>
              <div class="notes-subtitle">{{ currentNotes.length }} 条记录</div>
            </div>
          </div>
          <button class="notes-close-btn" @click="notesVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
      </template>

      <!-- 记录列表 -->
      <div class="notes-body">
        <div v-if="currentNotes.length === 0" class="notes-empty">
          <div class="empty-illustration">
            <el-icon :size="40"><ChatDotRound /></el-icon>
          </div>
          <p class="empty-title">暂无备注记录</p>
          <p class="empty-desc">添加第一条追踪记录，开始管理工单进展</p>
        </div>
        <div v-else class="notes-list">
          <div class="note-item" v-for="note in currentNotes" :key="note.id">
            <div class="note-avatar">{{ (note.userName || '管')[0] }}</div>
            <div class="note-card">
              <div class="note-card-header">
                <span class="note-card-author">{{ note.userName }}</span>
                <span class="note-card-time">{{ formatTime(note.createTime) }}</span>
              </div>
              <div class="note-card-body">{{ note.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="notes-footer">
        <div class="composer-box">
          <el-input
            v-model="newNoteContent"
            type="textarea"
            :rows="2"
            placeholder="记录工单进展或备注..."
            class="composer-input"
            @keyup.enter.ctrl="handleAddNote"
          />
          <button class="composer-send" @click="handleAddNote" :disabled="!newNoteContent.trim()">
            <el-icon :size="18"><Position /></el-icon>
          </button>
        </div>
        <span class="composer-hint">Ctrl + Enter 快捷发送</span>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFeedbackPage, assignInspector, transferFeedback, batchAssignFeedback, recommendInspectors, assignByName } from '@/api/feedback'
import { getInspectors } from '@/api/user'
import { getNotesByFeedback, addNote } from '@/api/feedbackNote'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
import { useUserStore } from '@/stores/user'
import {
  RefreshRight, User, Position, Tickets, UserFilled,
  Switch, ChatDotRound, MagicStick, Close
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const currentUserId = ref(Number(localStorage.getItem('userId') || 0))
const currentUserName = computed(() => userStore.userName || '管理员')

const feedbacks = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

// 批量指派逻辑
const selectedIds = ref([])
const batchInspectorId = ref(null)
// 问题①：网格员姓名列表 + 批量派送选中的姓名
const inspectorOptions = ref([])
const batchInspectorName = ref('')

const isAllSelected = computed(() => feedbacks.value.length > 0 && selectedIds.value.length === feedbacks.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < feedbacks.value.length)

const toggleSelectAll = (val) => {
  if (val) selectedIds.value = feedbacks.value.map(f => f.id)
  else selectedIds.value = []
}

// 转派状态
const transferVisible = ref(false)
const currentTransferRow = ref(null)
const transferTargetName = ref('')

// 智能推荐指派状态
const recommendVisible = ref(false)
const recommendLoading = ref(false)
const recommendList = ref([])
const recommendType = ref('')
const recommendTypeText = ref('')
const currentRecommendRow = ref(null)

const recommendTagType = computed(() => {
  if (recommendType.value === 'LOCAL') return 'success'
  if (recommendType.value === 'REMOTE_PROVINCE') return 'warning'
  return 'danger'
})

// 备注状态
const notesVisible = ref(false)
const currentNotesFeedbackId = ref(null)
const currentNotes = ref([])
const newNoteContent = ref('')

// ====== 辅助渲染函数 ======
function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

function isTimeout(row) {
  if (row.status !== 'PENDING') return false
  const created = new Date(row.createTime).getTime()
  return (Date.now() - created) > 24 * 60 * 60 * 1000
}

function getAqiClass(level) {
  if (level <= 2) return 'aqi-safe'
  if (level <= 4) return 'aqi-warn'
  return 'aqi-danger'
}

function getStatusText(row) {
  const map = {
    'PENDING': '待指派',
    'ASSIGNED': '已指派',
    'PROCESSING': '检测中',
    'COMPLETED': '已完成',
    'ESCALATED': '超时预警'
  }
  return map[row.status] || row.status
}

// ====== 业务逻辑 ======
function handleFilterChange() {
  // 同步 URL，确保顶部栏跳转和下拉筛选行为一致
  router.replace({ query: statusFilter.value ? { status: statusFilter.value } : {} })
  page.value = 1
  fetch()
}

async function fetch(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const r = await getFeedbackPage(page.value, size.value, statusFilter.value)
    feedbacks.value = r.data.map(f => ({ ...f, _inspectorName: '' }))
    total.value = r.total
    selectedIds.value = []
  } catch (e) { /* 静默 */ }
  if (showLoading) loading.value = false
}

async function handleAssign(row) {
  // 以姓名为主进行指派（ID 仅辅助），调用按姓名派送接口
  if (!row._inspectorName) { ElMessage.warning('请选择要承接的网格员'); return }
  try {
    await assignByName([row.id], row._inspectorName)
    ElMessage.success(`已派送给 ${row._inspectorName}`)
    fetch()
  } catch (e) {}
}

// 打开智能推荐弹窗
async function openRecommend(row) {
  currentRecommendRow.value = row
  recommendVisible.value = true
  recommendLoading.value = true
  recommendList.value = []
  recommendType.value = ''
  try {
    const res = await recommendInspectors(row.id)
    recommendList.value = res.data.inspectors || []
    recommendType.value = res.data.assignType
    recommendTypeText.value = res.data.assignTypeText
  } catch (e) {
    recommendList.value = []
  } finally {
    recommendLoading.value = false
  }
}

// 从推荐列表选中并指派
async function handleRecommendAssign(inspectorId) {
  try {
    await assignInspector(currentRecommendRow.value.id, inspectorId)
    ElMessage.success('工单调度成功')
    recommendVisible.value = false
    fetch()
  } catch (e) {}
}

async function handleBatchAssign() {
  // 问题①：按姓名批量派送
  if (!batchInspectorName.value) { ElMessage.warning('请选择承接网格员'); return }
  try {
    const res = await assignByName(selectedIds.value, batchInspectorName.value)
    ElMessage.success(`派送完成: 成功 ${res.data.successCount} / 共 ${res.data.totalCount} 条`)
    batchInspectorName.value = ''
    selectedIds.value = []
    fetch()
  } catch (e) {}
}

function openTransfer(row) {
  currentTransferRow.value = row
  transferTargetName.value = ''
  transferVisible.value = true
}

// 根据ID反查网格员姓名（辅助展示，ID为辅、姓名为主）
function inspectorNameById(id) {
  if (!id) return '未指派'
  const ins = inspectorOptions.value.find(i => i.id === id)
  return ins ? ins.realName : ('网格员 #' + id)
}

async function handleTransfer() {
  if (!transferTargetName.value) { ElMessage.warning('请选择新任网格员'); return }
  const target = inspectorOptions.value.find(i => i.realName === transferTargetName.value)
  if (!target) { ElMessage.warning('未找到该网格员'); return }
  try {
    await transferFeedback(currentTransferRow.value.id, target.id)
    ElMessage.success(`已转派给 ${transferTargetName.value}`)
    transferVisible.value = false
    fetch()
  } catch (e) {}
}

async function openNotes(row) {
  currentNotesFeedbackId.value = row.id
  notesVisible.value = true
  newNoteContent.value = ''
  try {
    const res = await getNotesByFeedback(row.id)
    currentNotes.value = res.data || []
  } catch (e) { currentNotes.value = [] }
}

async function handleAddNote() {
  const content = newNoteContent.value.trim()
  if (!content) { ElMessage.warning('请输入备注内容'); return }
  try {
    await addNote({
      feedbackId: currentNotesFeedbackId.value,
      userId: currentUserId.value,
      userName: currentUserName.value,
      content
    })
    ElMessage.success('备注已添加')
    newNoteContent.value = ''
    // 刷新列表
    const res = await getNotesByFeedback(currentNotesFeedbackId.value)
    currentNotes.value = res.data || []
  } catch (e) {}
}

// 问题①：加载网格员姓名列表供派送下拉
async function loadInspectors() {
  try {
    const res = await getInspectors()
    inspectorOptions.value = res.data || []
  } catch (e) { inspectorOptions.value = [] }
}

const refreshing = ref(false)
async function refreshData() {
  refreshing.value = true
  loading.value = true
  page.value = 1
  const minWait = new Promise(r => setTimeout(r, 500))
  await Promise.all([fetch(false), minWait])
  loading.value = false
  refreshing.value = false
}

// URL 参数 → 筛选 + 拉数据（挂载时立即执行，URL 变化时重新执行）
watch(() => route.query.status, (s) => {
  const valid = ['PENDING','ASSIGNED','PROCESSING','COMPLETED','ESCALATED']
  statusFilter.value = (s && valid.includes(s)) ? s : ''
  page.value = 1
  fetch()
}, { immediate: true })

onMounted(() => {
  loadInspectors()
})
</script>

<style scoped>
/* ========== 全局架构 ========== */
.swiss-page-container { max-width: 1440px; width: 100%; height: 100%; margin: 0 auto; display: flex; flex-direction: column; padding-bottom: 32px; box-sizing: border-box; color: #1C2421; position: relative; }
.fixed-section { flex-shrink: 0; margin-bottom: 24px; }
.stretch-section { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.glass-panel { background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px) saturate(180%); -webkit-backdrop-filter: blur(24px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 24px; box-shadow: 0 8px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6); }

/* 控制台 */
.action-console { height: 100px; padding: 0 48px; display: flex; justify-content: space-between; align-items: center; box-sizing: border-box; }
.console-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 24px; font-weight: 600; margin: 0; color: #1C2421; }
.page-subtitle { font-size: 14px; font-weight: 500; color: #74807B; }
.console-right { display: flex; align-items: center; gap: 16px; }

.search-capsule { display: flex; align-items: center; gap: 12px; background: rgba(255, 255, 255, 0.5); padding: 8px 16px 8px 20px; border-radius: 100px; border: 1px solid rgba(255, 255, 255, 0.8); box-shadow: 0 4px 16px -4px rgba(0, 0, 0, 0.03); }
.filter-label { font-size: 13px; font-weight: 600; color: #74807B; }

.swiss-btn { border: none; outline: none; border-radius: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
.swiss-btn.icon-btn { width: 44px; height: 44px; background: rgba(255, 255, 255, 0.8); color: #1C2421; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02); font-size: 20px; }
.swiss-btn.icon-btn:hover { background: #FFF; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }

/* ========== 悬浮批量操作舱 ========== */
.batch-console {
  position: absolute; top: 110px; left: 50%; transform: translateX(-50%); z-index: 50;
  display: flex; justify-content: space-between; align-items: center; padding: 12px 24px;
  border-radius: 100px; background: rgba(15, 23, 42, 0.85); border-color: rgba(255,255,255,0.1);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15); width: 600px; color: white;
}
.batch-left { display: flex; align-items: center; gap: 12px; }
.selection-count { background: #2AA876; width: 28px; height: 28px; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 14px; }
.batch-text { font-size: 14px; font-weight: 600; }
.batch-right { display: flex; align-items: center; gap: 12px; }

/* 极简自定义输入框 */
.minimal-input-wrapper { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.1); padding: 6px 12px; border-radius: 8px; transition: all 0.3s; }
.minimal-input-wrapper:focus-within { background: rgba(255,255,255,0.2); }
.minimal-input { border: none; background: transparent; color: white; font-size: 13px; width: 100px; outline: none; font-family: monospace; }
.minimal-input::placeholder { color: rgba(255,255,255,0.4); }
.minimal-input-wrapper.full { background: rgba(0,0,0,0.04); border: 1px solid rgba(0,0,0,0.08); width: 100%; }
.minimal-input-wrapper.full input { color: #1C2421; width: 100%; font-size: 14px; }

/* 动作按钮 */
.action-btn { height: 36px; padding: 0 16px; border-radius: 10px; display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.3s; border: none; }
.action-btn.primary { background: #2AA876; color: white; }
.action-btn.primary:hover { background: #1f8a5f; }
.action-btn.outline { background: transparent; color: white; border: 1px solid rgba(255,255,255,0.2); }
.action-btn.outline:hover { background: rgba(255,255,255,0.1); }
.action-btn.outline.small { background: white; color: #1C2421; border-color: rgba(28,36,33,0.1); }
.action-btn.outline.small:hover { border-color: #1C2421; }
.action-btn.ghost { background: transparent; color: #74807B; }
.action-btn.ghost:hover { background: rgba(0,0,0,0.05); color: #1C2421; }

/* ====== 操作按钮组 ====== */
.inline-select { width: 136px; }
.inline-select :deep(.el-input__wrapper) { border-radius: 8px; }
.inline-select :deep(.el-input__inner) { font-size: 12px; }
.action-btn-group { display: flex; gap: 4px; }
.assign-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 30px; padding: 0 10px; border-radius: 8px; border: none;
  background: #1C2421; color: #fff; font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
}
.assign-btn:hover { background: #2A483A; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(42,72,58,0.25); }
.assign-btn:active { transform: scale(0.96); }

.recommend-btn {
  width: 30px; height: 30px; border-radius: 8px; border: none;
  background: rgba(103,194,58,0.1); color: #67C23A;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; font-size: 15px;
}
.recommend-btn:hover { background: rgba(103,194,58,0.2); transform: scale(1.08); }

.transfer-btn {
  display: inline-flex; align-items: center; gap: 5px;
  height: 30px; padding: 0 12px; border-radius: 8px;
  border: 1px solid rgba(28,36,33,0.12); background: #fff;
  color: #74807B; font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all 0.2s; white-space: nowrap;
}
.transfer-btn:hover { border-color: #1C2421; color: #1C2421; background: #F8FAFC; }

.note-btn {
  width: 28px; height: 28px; border-radius: 6px; border: none;
  background: transparent; color: #A0AAB2;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; font-size: 15px;
  margin-left: 4px;
}
.note-btn:hover { background: rgba(0,0,0,0.05); color: #1C2421; }

/* 动画 */
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }

/* ========== 3. 调度矩阵 (Roster Grid) ========== */
.roster-workspace { display: flex; flex-direction: column; }
.roster-grid {
  display: grid;
  /* 精确比例划分: 选 | ID | 提报人 | 地址 | AQI | 状态 | 操作 */
  grid-template-columns: 40px 100px 120px 2fr 100px 180px 180px;
  align-items: center; gap: 16px; padding: 0 32px;
}

.roster-header-row { height: 56px; border-bottom: 1px solid rgba(28, 36, 33, 0.08); flex-shrink: 0; font-size: 12px; font-weight: 600; color: #A0AAB2; letter-spacing: 0.5px; }
.roster-scroll-area { flex: 1; overflow-y: auto; padding-top: 8px; }
.roster-scroll-area::-webkit-scrollbar { display: none; }

.roster-row { height: 64px; border-radius: 12px; transition: all 0.3s ease; border: 1px solid transparent; }
.roster-row:hover { background: rgba(255, 255, 255, 0.8); border-color: rgba(28, 36, 33, 0.04); box-shadow: 0 4px 12px -4px rgba(0,0,0,0.02); }

/* 异常超时状态隐喻 */
.roster-row.is-timeout { background: rgba(225, 29, 72, 0.02); }
.roster-row.is-timeout:hover { background: rgba(225, 29, 72, 0.04); border-color: rgba(225, 29, 72, 0.1); }
.timeout-pulse { width: 8px; height: 8px; border-radius: 50%; background: #E11D48; display: inline-block; margin-left: 8px; box-shadow: 0 0 0 0 rgba(225, 29, 72, 0.4); animation: pulse-red 2s infinite; }
@keyframes pulse-red { 0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(225, 29, 72, 0.4); } 70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(225, 29, 72, 0); } 100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(225, 29, 72, 0); } }

/* 单元格内容排版 */
.mono-id { font-family: monospace; font-size: 13px; color: #1C2421; font-weight: 700; }
.user-pill { display: inline-flex; align-items: center; gap: 6px; background: rgba(28,36,33,0.04); padding: 4px 10px; border-radius: 8px; font-size: 12px; font-family: monospace; font-weight: 600; color: #74807B; }
.address-text { font-size: 14px; font-weight: 500; color: #1C2421; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; cursor: default; }

.aqi-level-badge { padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 800; font-family: monospace; border: 1px solid; }
.aqi-safe { background: #F0FDF4; color: #059669; border-color: #BBF7D0; }
.aqi-warn { background: #FFFBEB; color: #D97706; border-color: #FDE68A; }
.aqi-danger { background: #FEF2F2; color: #E11D48; border-color: #FECDD3; }

/* 胶囊状态标识 */
.status-cell { display: flex; flex-direction: column; gap: 3px; align-items: flex-start; }
.status-inspector { font-size: 11px; color: #A0AAB2; font-weight: 500; }
.status-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 10px; white-space: nowrap; }
.pill-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
.status-pill.pending { background: #FFFBEB; color: #D97706; }
.status-pill.assigned { background: #EFF6FF; color: #2563EB; }
.status-pill.processing { background: #F5F3FF; color: #7C3AED; }
.status-pill.completed { background: #F0FDF4; color: #059669; }
.status-pill.escalated { background: #FEF2F2; color: #E11D48; }

/* 操作区 */
.col-action { display: flex; align-items: center; gap: 12px; justify-content: flex-end; }
.inline-assign { display: flex; align-items: center; gap: 6px; }
.action-done { font-size: 13px; color: #A0AAB2; padding: 0 8px; }
.inline-input { width: 60px; height: 32px; border-radius: 8px; border: 1px solid rgba(28,36,33,0.1); background: rgba(255,255,255,0.5); padding: 0 8px; font-family: monospace; font-size: 12px; text-align: center; outline: none; transition: border-color 0.3s; }
.inline-input:focus { border-color: #1C2421; background: white; }

.list-pagination { display: flex; justify-content: center; margin-top: 24px; padding: 16px; }
:deep(.swiss-pagination) { --el-pagination-bg-color: transparent; }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 120px 0; color: #A0AAB2; font-size: 14px; }
.empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.3; }

/* ========== 弹窗深度定制 (Swiss Dialogs) ========== */
:deep(.swiss-dialog) { background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(32px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 24px; box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.1); }
:deep(.swiss-dialog .el-dialog__header) { padding: 32px 32px 16px; margin: 0; }
:deep(.swiss-dialog .el-dialog__title) { font-size: 18px; font-weight: 700; color: #1C2421; }
:deep(.swiss-dialog .el-dialog__body) { padding: 0 32px; }
:deep(.swiss-dialog .el-dialog__footer) { padding: 24px 32px 32px; border-top: 1px solid rgba(28, 36, 33, 0.04); }

.dialog-form-content { display: flex; flex-direction: column; gap: 24px; padding: 16px 0; }
.info-row { display: flex; justify-content: space-between; align-items: center; background: #F8FAFC; padding: 12px 16px; border-radius: 12px; }
.info-label { font-size: 13px; color: #74807B; font-weight: 600; }
.info-value { font-size: 14px; color: #1C2421; font-family: monospace; font-weight: 700; }
.input-group label { display: block; font-size: 13px; color: #74807B; font-weight: 600; margin-bottom: 8px; }

.dialog-actions { display: flex; justify-content: flex-end; gap: 12px; }

/* ====== 备忘录弹窗 ====== */
:deep(.notes-dialog) {
  background: #fff; border-radius: 24px;
  box-shadow: 0 24px 64px -16px rgba(0,0,0,0.15);
}
:deep(.notes-dialog .el-dialog__header) { padding: 0; margin: 0; border: none; }
:deep(.notes-dialog .el-dialog__body) { padding: 0; }

.notes-dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24px 28px 16px;
}
.notes-header-left { display: flex; align-items: center; gap: 14px; }
.notes-icon-box {
  width: 42px; height: 42px; border-radius: 14px;
  background: rgba(42, 72, 58, 0.08); color: #2A483A;
  display: flex; align-items: center; justify-content: center;
}
.notes-title { font-size: 18px; font-weight: 700; color: #1C2421; line-height: 1.3; }
.notes-subtitle { font-size: 12px; color: #A0AAB2; font-weight: 500; }
.notes-close-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: rgba(28,36,33,0.04); color: #74807B;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
.notes-close-btn:hover { background: rgba(28,36,33,0.1); color: #1C2421; }

/* 记录列表区 */
.notes-body {
  max-height: 360px; overflow-y: auto; padding: 8px 28px 0;
}
.notes-body::-webkit-scrollbar { width: 3px; }
.notes-body::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 2px; }

/* 空态 */
.notes-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 48px 20px; text-align: center;
}
.empty-illustration {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(42,72,58,0.04), rgba(42,72,58,0.08));
  display: flex; align-items: center; justify-content: center;
  color: #2A483A; margin-bottom: 16px;
}
.empty-title { font-size: 15px; font-weight: 600; color: #1C2421; margin: 0 0 6px; }
.empty-desc { font-size: 13px; color: #A0AAB2; margin: 0; }

/* 记录条目 */
.notes-list { display: flex; flex-direction: column; gap: 12px; padding-bottom: 8px; }
.note-item { display: flex; gap: 12px; }
.note-avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #2A483A, #3D6B54);
  color: #fff; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(42,72,58,0.15);
}
.note-card {
  flex: 1; background: #F8FAFC; border-radius: 16px;
  padding: 14px 18px; min-width: 0;
  border: 1px solid rgba(28,36,33,0.04);
  transition: all 0.2s;
}
.note-card:hover { background: #F1F5F9; }
.note-card-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 6px; }
.note-card-author { font-size: 13px; font-weight: 700; color: #1C2421; }
.note-card-time { font-size: 11px; color: #A0AAB2; font-family: 'SF Mono', monospace; }
.note-card-body { font-size: 14px; color: #334155; line-height: 1.65; word-break: break-word; }

/* 底部输入区 */
.notes-footer { padding: 16px 28px 24px; }
.composer-box {
  display: flex; gap: 10px; align-items: flex-end;
  background: #F4F6F5; border-radius: 18px;
  padding: 6px 6px 6px 18px;
  border: 1px solid transparent;
  transition: all 0.25s;
}
.composer-box:focus-within {
  background: #fff; border-color: rgba(42,72,58,0.15);
  box-shadow: 0 0 0 3px rgba(42,72,58,0.04);
}
.composer-input { flex: 1; }
.composer-input :deep(.el-textarea__inner) {
  background: transparent !important; border: none !important;
  box-shadow: none !important; padding: 4px 0; font-size: 14px;
  color: #1C2421; resize: none; line-height: 1.5;
}
.composer-input :deep(.el-textarea__inner::placeholder) { color: #A0AAB2; }
.composer-send {
  width: 38px; height: 38px; border-radius: 12px; flex-shrink: 0;
  background: #2A483A; color: #fff; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
.composer-send:hover:not(:disabled) { background: #1C2421; transform: scale(1.05); }
.composer-send:disabled { background: #D0D5D9; cursor: not-allowed; }
.composer-hint { font-size: 11px; color: #C5CAD0; margin-top: 8px; display: block; text-align: right; }

/* 工具提示 */
.alpine-tooltip.el-popper { background: #1C2421 !important; color: white !important; font-weight: 600 !important; border: none !important; border-radius: 8px !important; padding: 8px 12px !important; box-shadow: 0 8px 24px rgba(28, 36, 33, 0.2) !important; }
.alpine-tooltip .el-popper__arrow::before { background: #1C2421 !important; border: none !important; }

/* 复选框深度美化 */
:deep(.el-checkbox__inner) { border-radius: 6px; border: 1px solid rgba(28,36,33,0.2); width: 18px; height: 18px; transition: all 0.2s; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner), :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) { background-color: #1C2421; border-color: #1C2421; }
:deep(.el-checkbox__inner::after) { top: 2px; left: 6px; width: 4px; height: 8px; }
</style>
<style>
.swiss-select { width: 140px; }
.swiss-select .el-input__wrapper { background: transparent !important; box-shadow: none !important; }
.swiss-select-dropdown { background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(24px) !important; border: 1px solid rgba(255, 255, 255, 0.9) !important; border-radius: 12px !important; box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08) !important; }
.swiss-select-dropdown .el-select-dropdown__item.selected { color: #2A483A !important; font-weight: 600 !important; }

/* ====== 智能推荐对话框 ====== */
:deep(.recommend-dialog) { background: #fff; border-radius: 24px; box-shadow: 0 24px 64px -16px rgba(0,0,0,0.12); }
:deep(.recommend-dialog .el-dialog__header) { padding: 0; margin: 0; border: none; }
:deep(.recommend-dialog .el-dialog__body) { padding: 0; }

.rec-header { display: flex; justify-content: space-between; align-items: center; padding: 24px 28px 16px; }
.rec-header-left { display: flex; align-items: center; gap: 14px; }
.rec-icon-box {
  width: 42px; height: 42px; border-radius: 14px; display: flex;
  align-items: center; justify-content: center;
  background: linear-gradient(135deg, rgba(103,194,58,0.1), rgba(103,194,58,0.2));
  color: #67C23A;
}
.rec-title { font-size: 18px; font-weight: 700; color: #1C2421; line-height: 1.3; }
.rec-subtitle { font-size: 12px; color: #A0AAB2; font-weight: 500; }
.rec-close {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: rgba(28,36,33,0.04); color: #74807B;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
.rec-close:hover { background: rgba(28,36,33,0.1); color: #1C2421; }

.rec-body { padding: 0 28px 24px; min-height: 120px; }

/* 策略标签 */
.rec-strategy { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid rgba(28,36,33,0.05); }
.strategy-badge {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; border-radius: 8px;
  font-size: 12px; font-weight: 700;
}
.strategy-badge.local { background: rgba(103,194,58,0.1); color: #67C23A; }
.strategy-badge.remote_province { background: rgba(245,166,35,0.1); color: #D97706; }
.strategy-badge.remote { background: rgba(64,158,255,0.1); color: #2563EB; }
.strategy-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.strategy-desc { font-size: 12px; color: #A0AAB2; font-weight: 500; }

/* 空态 */
.rec-empty {
  display: flex; flex-direction: column; align-items: center; padding: 40px 20px;
  text-align: center; color: #A0AAB2;
}
.rec-empty .el-icon { margin-bottom: 12px; opacity: 0.4; }
.rec-empty p { font-size: 14px; font-weight: 600; color: #74807B; margin: 0 0 4px; }
.rec-empty span { font-size: 12px; }

/* 推荐卡片列表 */
.rec-list { display: flex; flex-direction: column; gap: 10px; }
.rec-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; background: #F8FAFC; border-radius: 14px;
  border: 1px solid rgba(28,36,33,0.04);
  animation: cardSlideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: all 0.25s;
}
.rec-card:hover {
  background: #fff; border-color: rgba(103,194,58,0.2);
  box-shadow: 0 4px 16px -4px rgba(103,194,58,0.12);
  transform: translateX(4px);
}
@keyframes cardSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.rec-rank {
  width: 28px; height: 28px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800; background: rgba(28,36,33,0.05); color: #74807B;
}
.rec-card:first-child .rec-rank { background: #67C23A; color: #fff; }

.rec-avatar {
  width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #2A483A, #3D6B54);
  color: #fff; font-size: 15px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.rec-info { flex: 1; min-width: 0; }
.rec-name { font-size: 14px; font-weight: 700; color: #1C2421; margin-bottom: 2px; }
.rec-meta { font-size: 11px; color: #A0AAB2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.rec-assign-btn {
  display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0;
  height: 32px; padding: 0 16px; border-radius: 10px; border: none;
  background: linear-gradient(135deg, #1C2421, #2A483A); color: #fff;
  font-size: 12px; font-weight: 700; cursor: pointer;
  transition: all 0.2s;
}
.rec-assign-btn:hover { transform: scale(1.04); box-shadow: 0 4px 12px rgba(42,72,58,0.3); }
.rec-assign-btn:active { transform: scale(0.96); }

/* 筛选激活时边框高亮 */
.search-capsule.is-active {
  border-color: rgba(42, 72, 58, 0.2) !important;
  box-shadow: 0 4px 16px -4px rgba(42, 72, 58, 0.08) !important;
}

/* 刷新旋转 */
.is-spinning { animation: spin 0.6s linear; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>