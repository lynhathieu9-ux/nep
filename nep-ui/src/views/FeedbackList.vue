<template>
  <div class="swiss-page-container">

    <header class="action-console glass-panel fixed-section">
      <div class="console-left">
        <h2 class="page-title">监督反馈网络</h2>
        <span class="page-subtitle">环境问题流转与实时追踪</span>
      </div>

      <div class="console-right">
        <div class="search-capsule">
          <el-icon><Search /></el-icon>
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索描述/地址..."
            clearable
            class="keyword-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <div class="search-capsule">
          <span class="filter-label">日期</span>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始"
            end-placeholder="结束"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="date-picker"
            @change="handleSearch"
          />
        </div>
        <div class="search-capsule">
          <span class="filter-label">状态</span>
          <el-select v-model="searchForm.status" placeholder="全景" clearable class="swiss-select"
            popper-class="swiss-select-dropdown" @change="handleSearch">
            <el-option label="全景" value="" />
            <el-option label="待指派" value="PENDING" />
            <el-option label="处理中" value="ASSIGNED" />
            <el-option label="已归档" value="COMPLETED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已升级" value="ESCALATED" />
          </el-select>
        </div>

        <el-button class="swiss-btn icon-btn" @click="handleReset" title="重置视图">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
        <button class="swiss-btn primary-btn" @click="$router.push('/ne/submit')">
          <el-icon><Plus /></el-icon> 提报案件
        </button>
      </div>
    </header>

    <main class="spatial-split-view stretch-section">

      <aside class="master-list-panel glass-panel scrollable-card">
        <div class="list-header panel-header">
          <span class="header-text">近期案件</span>
          <span class="item-count">{{ total }} 宗</span>
        </div>

        <div class="card-flow scroll-area" v-loading="loading">
          <div v-for="item in tableData" :key="item.id" class="feed-ticket-card"
            :class="{ 'is-active': selectedFeedback?.id === item.id }" @click="selectFeedback(item)">
            <div class="ticket-top">
              <span class="ticket-id">#{{ String(item.id).padStart(5, '0') }}</span>
              <div class="ticket-top-right">
                <el-tag v-if="isTimeout(item)" type="danger" size="small" effect="dark">超时</el-tag>
                <div class="mini-status" :class="'status-' + item.status.toLowerCase()"></div>
              </div>
            </div>
            <div class="ticket-main">
              <div class="ticket-title">{{ item.description || '常规环境监测记录' }}</div>
              <div class="ticket-meta">
                <el-icon>
                  <LocationInformation />
                </el-icon>
                <span>{{ item.specificAddress || `网格 #${item.cityId}` }}</span>
              </div>
            </div>
            <div class="ticket-bottom">
              <span class="time-stamp">{{ formatTime(item.createTime) }}</span>
              <div class="aqi-badge" :class="'aqi-level-' + item.estimatedAqiLevel">
                AQI {{ item.estimatedAqiLevel }}
              </div>
            </div>
          </div>

          <div class="empty-state-wrapper" v-if="!loading && tableData.length === 0">
            <el-empty description="暂无追踪档案" :image-size="60" />
          </div>
        </div>

        <div class="list-pagination">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total"
            layout="prev, pager, next" class="swiss-pagination" @current-change="handleSearch" />
        </div>
      </aside>

      <section class="detail-view-panel glass-panel scrollable-card">
        <template v-if="selectedFeedback">
          <div class="detail-header panel-header">
            <div class="header-left">
              <div class="detail-id-badge">案件 #{{ String(selectedFeedback.id).padStart(5, '0') }}</div>
            </div>
            <div class="header-right" style="display:flex;align-items:center;gap:12px">
              <el-button
                v-if="selectedFeedback.status === 'PENDING' && selectedFeedback.supervisorId === currentUserId"
                type="danger" plain size="small" @click="handleCancel"
              >撤回反馈</el-button>
              <span class="sub-time">立案于 {{ formatTime(selectedFeedback.createTime) }}</span>
            </div>
          </div>

          <div class="detail-body scroll-area">
            <div class="flow-tracker-container">
              <h3 class="bento-title">生命周期追踪</h3>
              <div class="timeline-track">
                <div v-for="(step, index) in flowSteps" :key="index" class="track-step" :class="{
                  'is-completed': index < currentStepIndex,
                  'is-active': index === currentStepIndex,
                  'is-pending': index > currentStepIndex
                }">
                  <div class="step-line" v-if="index < flowSteps.length - 1"></div>

                  <div class="step-node">
                    <div class="node-inner"></div>
                  </div>
                  <div class="step-label">{{ step.label }}</div>
                </div>
              </div>
            </div>

            <div class="detail-bento-grid">

              <div class="bento-card col-span-2">
                <div class="bento-icon-header"><el-icon>
                    <Location />
                  </el-icon> <span>定位坐标</span></div>
                <div class="bento-main-text">{{ selectedFeedback.specificAddress || '未记录详细物理坐标' }}</div>
                <div class="bento-sub-text">行政区划代码: {{ selectedFeedback.provinceId }} - {{ selectedFeedback.cityId }}
                </div>
              </div>

              <div class="bento-card">
                <div class="bento-icon-header"><el-icon>
                    <DataLine />
                  </el-icon> <span>AQI 评估</span></div>
                <div class="aqi-massive-display" :class="'aqi-text-' + selectedFeedback.estimatedAqiLevel">
                  {{ selectedFeedback.estimatedAqiLevel }} <span class="unit">级</span>
                </div>
              </div>

              <div class="bento-card">
                <div class="bento-icon-header"><el-icon>
                    <User />
                  </el-icon> <span>人员调度</span></div>
                <div class="personnel-info">
                  <div class="avatar-placeholder"><el-icon>
                      <Avatar />
                    </el-icon></div>
                  <div class="personnel-text">
                    <span class="p-name">{{ selectedFeedback.assignedInspectorId ? 'ID: ' +
                      selectedFeedback.assignedInspectorId : '待指派' }}</span>
                    <span class="p-role">负责网格员</span>
                  </div>
                </div>
              </div>

              <div class="bento-card col-span-2">
                <div class="bento-icon-header"><el-icon>
                    <Document />
                  </el-icon> <span>现场勘测描述</span></div>
                <div class="description-box">
                  {{ selectedFeedback.description || '监督员仅上传了坐标，未提供详细文字描述。' }}
                </div>
              </div>

              <!-- AQI 实测数据 (Feature 6) -->
              <div v-if="aqiData" class="bento-card col-span-2">
                <div class="bento-icon-header"><el-icon><DataLine /></el-icon><span>网格员实测 AQI 数据</span></div>
                <div class="aqi-detection-grid">
                  <div class="aqi-item">
                    <div class="aqi-item-label">SO₂ (二氧化硫)</div>
                    <div class="aqi-item-value" :class="'aqi-text-' + (aqiData.so2Aqi > 2 ? 5 : aqiData.so2Aqi)">等级 {{ aqiData.so2Aqi }}</div>
                  </div>
                  <div class="aqi-item">
                    <div class="aqi-item-label">CO (一氧化碳)</div>
                    <div class="aqi-item-value" :class="'aqi-text-' + (aqiData.coAqi > 2 ? 5 : aqiData.coAqi)">等级 {{ aqiData.coAqi }}</div>
                  </div>
                  <div class="aqi-item">
                    <div class="aqi-item-label">PM2.5 (悬浮颗粒物)</div>
                    <div class="aqi-item-value" :class="'aqi-text-' + (aqiData.pm25Aqi > 2 ? 5 : aqiData.pm25Aqi)">等级 {{ aqiData.pm25Aqi }}</div>
                  </div>
                  <div class="aqi-item aqi-final">
                    <div class="aqi-item-label">最终 AQI 等级</div>
                    <div class="aqi-item-value aqi-final-value" :class="'aqi-text-' + aqiData.finalAqi">{{ aqiData.finalAqi }} 级</div>
                  </div>
                </div>
                <div class="aqi-meta" v-if="aqiData.detectionTime">
                  <el-icon><Clock /></el-icon>
                  <span>检测时间: {{ formatTime(aqiData.detectionTime) }}</span>
                </div>
                <div v-if="aqiData.remark" class="aqi-remark">
                  <el-icon><Document /></el-icon>
                  <span>{{ aqiData.remark }}</span>
                </div>
              </div>

              <!-- 满意度评价 (Feature 5) -->
              <div v-if="selectedFeedback.status === 'COMPLETED'" class="bento-card col-span-2">
                <div class="bento-icon-header"><el-icon><Star /></el-icon><span>满意度评价</span></div>
                <div v-if="selectedFeedback.rating">
                  <el-rate v-model="selectedFeedback.rating" disabled show-score text-color="#ff9900" />
                  <p v-if="selectedFeedback.ratingComment" class="rating-comment">{{ selectedFeedback.ratingComment }}</p>
                  <p v-if="selectedFeedback.ratingTime" class="rating-time">评价于 {{ formatTime(selectedFeedback.ratingTime) }}</p>
                </div>
                <div v-else>
                  <el-rate v-model="newRating" show-score text-color="#ff9900" />
                  <el-input v-model="newRatingComment" type="textarea" :rows="2" placeholder="评价备注（可选）..." style="margin-top:8px" />
                  <el-button type="primary" size="small" @click="handleRate" style="margin-top:8px">提交评价</el-button>
                </div>
              </div>

              <!-- 拒绝原因 (Feature 2) -->
              <div v-if="selectedFeedback.status === 'REJECTED' && selectedFeedback.rejectReason" class="bento-card col-span-2">
                <div class="bento-icon-header"><el-icon><WarningFilled /></el-icon><span>拒绝原因</span></div>
                <div class="description-box" style="color:#D9534F">
                  {{ selectedFeedback.rejectReason }}
                </div>
              </div>

            </div>
          </div>
        </template>

        <div class="empty-state-view" v-else>
          <el-icon class="placeholder-icon">
            <Document />
          </el-icon>
          <p>请在左侧列表选择一宗案件查看详情</p>
        </div>
      </section>

    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMyFeedbackPage, rateFeedback, getFeedbackById, cancelFeedback } from '@/api/feedback'
import { getAqiByFeedbackId } from '@/api/aqi'
import {
  RefreshRight, Plus, LocationInformation, Search,
  Location, DataLine, User, Avatar, Document, Star, WarningFilled, Clock
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const searchForm = ref({ status: '', keyword: '', dateRange: null })
const selectedFeedback = ref(null)

const flowSteps = [
  { label: '立案' },
  { label: '派发' },
  { label: '接办' },
  { label: '勘测' },
  { label: '处理' },
  { label: '验收' }
]

const currentStepIndex = computed(() => {
  if (!selectedFeedback.value) return 0
  const s = selectedFeedback.value.status
  if (s === 'PENDING') return 0
  if (s === 'ASSIGNED') return 2
  if (s === 'COMPLETED') return 5
  if (s === 'REJECTED') return 1
  if (s === 'ESCALATED') return 1
  return 0
})

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

// 超时判断 (>24h 未处理的 PENDING)
function isTimeout(item) {
  if (item.status !== 'PENDING') return false
  const created = new Date(item.createTime).getTime()
  return (Date.now() - created) > 24 * 60 * 60 * 1000
}

// 满意度评价
const newRating = ref(0)
const newRatingComment = ref('')
const currentUserId = ref(Number(localStorage.getItem('userId') || 0))

async function handleRate() {
  if (newRating.value === 0) { ElMessage.warning('请选择评分'); return }
  try {
    await rateFeedback(selectedFeedback.value.id, newRating.value, newRatingComment.value)
    ElMessage.success('评价成功')
    // 刷新详情
    const res = await getFeedbackById(selectedFeedback.value.id)
    selectedFeedback.value = res.data
    handleSearch()
  } catch(e) {}
}

function selectFeedback(item) {
  selectedFeedback.value = item
}

// 获取 AQI 实测数据
const aqiData = ref(null)
watch(selectedFeedback, async (fb) => {
  aqiData.value = null
  if (fb && fb.status === 'COMPLETED') {
    try {
      const res = await getAqiByFeedbackId(fb.id)
      aqiData.value = res.data
    } catch (e) { /* AQI数据可能尚未提交 */ }
  }
})

async function handleSearch() {
  loading.value = true
  try {
    const range = searchForm.value.dateRange || []
    const res = await getMyFeedbackPage(
      currentPage.value, pageSize.value,
      searchForm.value.status, searchForm.value.keyword,
      range.length >= 1 ? range[0] : '',
      range.length >= 2 ? range[1] : ''
    )
    tableData.value = res.data
    total.value = res.total
    if (tableData.value.length > 0 && !selectedFeedback.value) {
      selectedFeedback.value = tableData.value[0]
    }
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!selectedFeedback.value) return
  try {
    await ElMessageBox.confirm(
      '撤回后该反馈将被永久删除，此操作不可恢复。确定要继续吗？',
      '确认撤回',
      { confirmButtonText: '确定撤回', cancelButtonText: '取消', type: 'warning' }
    )
    await cancelFeedback(selectedFeedback.value.id)
    ElMessage.success('反馈已撤回')
    selectedFeedback.value = null
    handleSearch()
  } catch (e) { /* 用户取消或接口报错 */ }
}

function handleReset() {
  searchForm.value = { status: '', keyword: '', dateRange: null }
  currentPage.value = 1
  selectedFeedback.value = null
  handleSearch()
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
/* ========== 全局主容器 ========== */
.swiss-page-container {
  max-width: 1440px;
  width: 100%;
  height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
  box-sizing: border-box;
  color: #1C2421;
}

.fixed-section {
  flex-shrink: 0;
}

.stretch-section {
  flex: 1;
  min-height: 0;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 8px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6);
}

.scrollable-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  min-height: 300px;
}

.scroll-area::-webkit-scrollbar {
  display: none;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(28, 36, 33, 0.06);
}

/* ========== 1. 悬浮控制台 (经典的左右分布) ========== */
.action-console {
  height: 120px;
  padding: 0 48px;
  display: flex;
  justify-content: space-between;
  /* 恢复纯粹的左右顶格布局 */
  align-items: center;
  box-sizing: border-box;
}

.console-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin: 0;
  letter-spacing: 0.5px;
  color: #1C2421;
}

.page-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #74807B;
}

.console-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
}

/* 右侧集成的筛选胶囊 */
.search-capsule {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.5);
  padding: 8px 16px 8px 20px;
  border-radius: 100px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px -4px rgba(0, 0, 0, 0.03);
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #74807B;
}

.divider { display: none; }

.keyword-input { width: 180px; }
.keyword-input :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; border-radius: 0; }

.date-picker { width: 220px; }
.date-picker :deep(.el-input__wrapper) { background: transparent !important; box-shadow: none !important; }

.swiss-btn {
  border: none;
  outline: none;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.swiss-btn.icon-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.8);
  color: #74807B;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.swiss-btn.icon-btn:hover {
  background: #FFF;
  color: #1C2421;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.swiss-btn.primary-btn {
  padding: 0 20px;
  height: 40px;
  background: #2A483A;
  color: #FFF;
  box-shadow: 0 4px 12px rgba(42, 72, 58, 0.2);
}

.swiss-btn.primary-btn:hover {
  background: #1C2421;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(42, 72, 58, 0.3);
}

/* ========== 2. 主从分栏结构 ========== */
.spatial-split-view {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  align-items: stretch;
}

.master-list-panel {
  background: rgba(255, 255, 255, 0.55);
}

.list-header .header-text {
  font-size: 14px;
  font-weight: 600;
  color: #1C2421;
}

.list-header .item-count {
  font-size: 12px;
  background: rgba(28, 36, 33, 0.05);
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
  color: #74807B;
}

.card-flow {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feed-ticket-card {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.feed-ticket-card:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px -4px rgba(0, 0, 0, 0.05);
}

.feed-ticket-card.is-active {
  background: #FFFFFF;
  border-color: rgba(42, 72, 58, 0.15);
  box-shadow: 0 8px 24px -6px rgba(42, 72, 58, 0.1), inset 0 2px 4px rgba(255, 255, 255, 1);
}

.ticket-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ticket-id {
  font-family: "SF Mono", Consolas, monospace;
  font-size: 13px;
  font-weight: 600;
  color: #2A483A;
}

.mini-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-pending {
  background: #F5A623;
  box-shadow: 0 0 6px rgba(245, 166, 35, 0.4);
}

.status-assigned {
  background: #409EFF;
  box-shadow: 0 0 6px rgba(64, 158, 255, 0.4);
}

.status-completed {
  background: #2AA876;
  box-shadow: 0 0 6px rgba(42, 168, 118, 0.4);
}

.status-rejected {
  background: #D9534F;
  box-shadow: 0 0 6px rgba(217, 83, 79, 0.4);
}

.status-escalated {
  background: #E6A23C;
  box-shadow: 0 0 6px rgba(230, 162, 60, 0.4);
}

.ticket-top-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 超时行的视觉强调 */
.feed-ticket-card:has(.ticket-top-right .el-tag--danger) {
  border-left: 3px solid #F56C6C;
}

/* 满意度评价 */
.rating-comment {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin-top: 8px;
}

.rating-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.ticket-main {
  margin-bottom: 12px;
}

.ticket-title {
  font-size: 14px;
  font-weight: 600;
  color: #1C2421;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #74807B;
}

.ticket-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-stamp {
  font-size: 12px;
  font-weight: 500;
  color: #A0AAB2;
}

.aqi-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(28, 36, 33, 0.04);
}

.empty-state-wrapper {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.list-pagination {
  padding: 12px;
  border-top: 1px solid rgba(28, 36, 33, 0.06);
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}

:deep(.swiss-pagination) {
  --el-pagination-bg-color: transparent;
}

/* === 右栏：详情视图 === */
.detail-header {
  padding: 24px 32px 20px;
}

.detail-id-badge {
  font-size: 16px;
  font-weight: 700;
  color: #2A483A;
  background: rgba(42, 72, 58, 0.08);
  padding: 6px 14px;
  border-radius: 12px;
  letter-spacing: 0.5px;
}

.sub-time {
  font-size: 13px;
  font-weight: 500;
  color: #74807B;
}

.detail-body {
  padding: 0 32px 32px;
}

.flow-tracker-container {
  margin: 24px 0 40px 0;
}

.bento-title {
  font-size: 13px;
  font-weight: 600;
  color: #74807B;
  letter-spacing: 0.5px;
  margin-bottom: 24px;
  text-transform: uppercase;
}

.timeline-track {
  display: flex;
  width: 100%;
  align-items: flex-start;
}

.track-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 2;
}

.step-node {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #F4F6F5;
  border: 2px solid rgba(28, 36, 33, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.4s ease;
}

.node-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: all 0.4s ease;
}

.step-label {
  font-size: 12px;
  font-weight: 600;
  color: #A0AAB2;
  text-align: center;
  white-space: normal;
}

.step-line {
  position: absolute;
  top: 12px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: rgba(28, 36, 33, 0.08);
  z-index: -1;
}

.track-step.is-completed .step-node {
  border-color: #2AA876;
  background: rgba(42, 168, 118, 0.1);
}

.track-step.is-completed .node-inner {
  background: #2AA876;
}

.track-step.is-completed .step-label {
  color: #1C2421;
}

.track-step.is-completed .step-line {
  background: #2AA876;
}

.track-step.is-active .step-node {
  border-color: #409EFF;
  background: rgba(64, 158, 255, 0.15);
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
  transform: scale(1.15);
}

.track-step.is-active .node-inner {
  background: #409EFF;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
}

.track-step.is-active .step-label {
  color: #409EFF;
  font-weight: 700;
}

.track-step.is-active .step-line {
  background: linear-gradient(90deg, #409EFF, rgba(28, 36, 33, 0.08));
}

.detail-bento-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.bento-card {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 20px;
}

.bento-card.col-span-2 {
  grid-column: 1 / -1;
}

.bento-icon-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #74807B;
  margin-bottom: 12px;
}

.bento-icon-header .el-icon {
  color: #2A483A;
  font-size: 16px;
}

.bento-main-text {
  font-size: 15px;
  font-weight: 600;
  color: #1C2421;
  margin-bottom: 4px;
  line-height: 1.4;
}

.bento-sub-text {
  font-size: 13px;
  color: #74807B;
}

.aqi-massive-display {
  font-size: 32px;
  font-weight: 700;
  font-family: "SF Pro Display", sans-serif;
}

.aqi-massive-display .unit {
  font-size: 15px;
  font-weight: 600;
  opacity: 0.7;
}

.aqi-text-1,
.aqi-text-2 {
  color: #2AA876;
}

.aqi-text-3,
.aqi-text-4 {
  color: #F5A623;
}

.aqi-text-5,
.aqi-text-6 {
  color: #D9534F;
}

.personnel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(42, 72, 58, 0.1);
  color: #2A483A;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
}

.personnel-text {
  display: flex;
  flex-direction: column;
}

.p-name {
  font-size: 15px;
  font-weight: 600;
  color: #1C2421;
}

.p-role {
  font-size: 12px;
  color: #74807B;
  font-weight: 500;
  margin-top: 2px;
}

/* AQI实测数据 */
.aqi-detection-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.aqi-item {
  background: rgba(28, 36, 33, 0.03);
  border-radius: 12px;
  padding: 14px;
  text-align: center;
  transition: all 0.3s;
}

.aqi-item:hover { background: rgba(28, 36, 33, 0.06); }

.aqi-item-label {
  font-size: 11px;
  color: #74807B;
  font-weight: 500;
  margin-bottom: 6px;
}

.aqi-item-value {
  font-size: 16px;
  font-weight: 700;
  font-family: "SF Pro Display", sans-serif;
}

.aqi-final {
  background: rgba(42, 168, 118, 0.08);
  border: 1px solid rgba(42, 168, 118, 0.2);
}

.aqi-final-value { font-size: 20px; }

.aqi-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #74807B;
  margin-top: 12px;
}

.aqi-remark {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: #555;
  margin-top: 8px;
  padding: 10px;
  background: rgba(28, 36, 33, 0.03);
  border-radius: 8px;
}

.description-box {
  font-size: 14px;
  line-height: 1.6;
  color: #1C2421;
  background: rgba(28, 36, 33, 0.03);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(28, 36, 33, 0.05);
}

.empty-state-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 20px;
}

.placeholder-icon {
  font-size: 48px;
  color: rgba(28, 36, 33, 0.15);
}

.empty-state-view p {
  font-size: 14px;
  font-weight: 500;
  color: #A0AAB2;
}
</style>

<style>
.swiss-select {
  width: 140px;
}

.swiss-select .el-input__wrapper {
  background: transparent !important;
  box-shadow: none !important;
}

.swiss-select .el-input__wrapper.is-focus {
  box-shadow: none !important;
}

.swiss-select-dropdown {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(24px) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 12px !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08) !important;
}

.swiss-select-dropdown .el-select-dropdown__item.selected {
  color: #2A483A !important;
  font-weight: 600 !important;
}
</style>