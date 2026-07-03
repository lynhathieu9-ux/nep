<template>
  <div class="swiss-dashboard">
    <!-- 顶部欢迎区 (晨曦绿 + 光影呼吸动态) -->
    <section class="hero-section hero-premium-card fixed-section">
      <!-- 动态呼吸光晕层 -->
      <div class="hero-breathe-glow glow-1"></div>
      <div class="hero-breathe-glow glow-2"></div>

      <div class="hero-content">
        <h1 class="hero-greeting">{{ greeting }} <span class="hero-name">{{ userName }}</span></h1>
        <p class="hero-date">{{ today }} · 您的每一次反馈，都在守护碧水蓝天</p>
      </div>
      <div class="hero-status">
        <div class="status-icon-ring">
          <el-icon>
            <Odometer />
          </el-icon>
        </div>
        <div class="status-info">
          <div class="status-title">环保中枢在线</div>
          <div class="status-subtitle">数据云端实时同步</div>
        </div>
      </div>
    </section>

    <!-- 关键数据矩阵 -->
    <section class="stats-grid fixed-section">
      <div class="stat-glass-card" @click="$router.push('/ne/feedbacks')">
        <div class="stat-header">
          <span class="stat-label">我的反馈</span>
          <div class="stat-icon-wrapper"><el-icon>
              <Document />
            </el-icon></div>
        </div>
        <div class="stat-body">
          <div class="stat-value"><span class="value-number">{{ stats.myFeedbacks }}</span></div>
        </div>
        <div class="stat-footer">
          <span class="trend-context">累计提交监督记录</span>
        </div>
      </div>

      <div class="stat-glass-card" @click="$router.push('/ne/feedbacks')">
        <div class="stat-header">
          <span class="stat-label">待处理</span>
          <div class="stat-icon-wrapper" style="color:#F5A623; background:rgba(245,166,35,0.1)"><el-icon>
              <Warning />
            </el-icon></div>
        </div>
        <div class="stat-body">
          <div class="stat-value"><span class="value-number" style="color:#F5A623">{{ stats.pendingCount }}</span></div>
        </div>
        <div class="stat-footer">
          <span class="trend-context">流转中案件</span>
        </div>
      </div>

      <div class="stat-glass-card" @click="$router.push('/ne/feedbacks')">
        <div class="stat-header">
          <span class="stat-label">已办结</span>
          <div class="stat-icon-wrapper" style="color:#2AA876; background:rgba(42,168,118,0.1)"><el-icon>
              <CircleCheck />
            </el-icon></div>
        </div>
        <div class="stat-body">
          <div class="stat-value"><span class="value-number" style="color:#2AA876">{{ stats.completedCount }}</span>
          </div>
        </div>
        <div class="stat-footer">
          <span class="trend-context">成功处理案件</span>
        </div>
      </div>

      <div class="stat-glass-card" style="cursor: default;">
        <div class="stat-header">
          <span class="stat-label">全网AQI勘测</span>
          <div class="stat-icon-wrapper" style="color:#409EFF; background:rgba(64,158,255,0.1)"><el-icon>
              <DataAnalysis />
            </el-icon></div>
        </div>
        <div class="stat-body">
          <div class="stat-value"><span class="value-number" style="color:#409EFF">{{ stats.aqiTotal }}</span></div>
        </div>
        <div class="stat-footer">
          <span class="trend-context">全国累计环境探测</span>
        </div>
      </div>
    </section>

    <!-- 三栏结构 -->
    <section class="content-split-grid stretch-section">

      <!-- 1. 左侧：快捷操作网格 -->
      <div class="action-bento glass-panel scrollable-card">
        <div class="panel-header">
          <h3 class="panel-title">中枢调度舱</h3>
        </div>
        <div class="bento-grid scroll-area">
          <div class="bento-item" @click="$router.push('/ne/submit')">
            <div class="bento-icon"><el-icon>
                <EditPen />
              </el-icon></div>
            <span class="bento-label">新案提报</span>
          </div>
          <div class="bento-item" @click="$router.push('/ne/feedbacks')">
            <div class="bento-icon"><el-icon>
                <DataLine />
              </el-icon></div>
            <span class="bento-label">追踪进度</span>
          </div>
          <div class="bento-item" @click="$router.push('/ne/news')">
            <div class="bento-icon"><el-icon>
                <Reading />
              </el-icon></div>
            <span class="bento-label">环境资讯</span>
          </div>
          <div class="bento-item" @click="$router.push('/ne/knowledge')">
            <div class="bento-icon"><el-icon>
                <Collection />
              </el-icon></div>
            <span class="bento-label">知识百科</span>
          </div>
        </div>
      </div>

      <!-- 2. 中间：最近反馈流 -->
      <div class="feedback-stream glass-panel scrollable-card">
        <div class="panel-header">
          <h3 class="panel-title">近期提报流转</h3>
          <button class="text-link-btn" @click="$router.push('/ne/feedbacks')">
            归档室 <el-icon>
              <ArrowRight />
            </el-icon>
          </button>
        </div>
        <div class="stream-list scroll-area" v-loading="feedLoading">
          <div class="stream-item" v-for="fb in recentFeedbacks" :key="fb.id" @click="$router.push('/ne/feedbacks')">
            <div class="aqi-indicator" :class="'aqi-level-' + fb.estimatedAqiLevel">
              <div class="aqi-dot"></div>
            </div>
            <div class="stream-content">
              <div class="stream-desc">{{ fb.description || '无详细描述' }}</div>
              <div class="stream-meta">
                <el-icon>
                  <LocationInformation />
                </el-icon>
                <span>{{ fb.specificAddress || '区域信标待定' }}</span>
              </div>
            </div>
            <div class="stream-status">
              <span class="status-pill" :class="'status-' + fb.status.toLowerCase()">
                {{ statusLabel(fb.status) }}
              </span>
              <span class="stream-time">{{ formatTime(fb.createTime) }}</span>
            </div>
          </div>

          <div v-if="!feedLoading && recentFeedbacks.length === 0" class="empty-guide-state">
            <div class="empty-icon-ring"><el-icon>
                <Monitor />
              </el-icon></div>
            <p class="empty-title">您还没有提报过环境问题</p>
            <p class="empty-sub">作为公众监督员，您可以随时记录异常排放与污染。</p>
            <button class="swiss-action-btn" @click="$router.push('/ne/submit')">启动首次勘测</button>
          </div>
        </div>
      </div>

      <!-- 3. 右侧：嵌入式生态日历 -->
      <div class="calendar-bento scrollable-card">
        <div class="scroll-area custom-calendar-wrapper">
          <el-calendar ref="calendarRef" v-model="currentDate">
            <template #date-cell="{ data }">
              <div
                class="cal-day-cell"
                :class="{ 'has-feedback': hasFeedback(data.day) }"
              >
                <span class="cal-day-num">{{ data.day.split('-').pop() }}</span>
                <span v-if="hasFeedback(data.day)" class="cal-feedback-dot" title="当天有反馈记录"></span>
              </div>
            </template>
            <template #header="{ date }">
              <div class="embedded-cal-header">
                <span class="cal-title">{{ date }}</span>
                <div class="cal-actions">
                  <button class="cal-arrow-btn" @click="selectDate('prev-month')" title="上个月">
                    <el-icon><ArrowLeft /></el-icon>
                  </button>
                  <button class="cal-today-btn" @click="selectDate('today')">
                    <span class="live-dot"></span> 今天
                  </button>
                  <button class="cal-arrow-btn" @click="selectDate('next-month')" title="下个月">
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                </div>
              </div>
            </template>
          </el-calendar>
        </div>
      </div>

    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyFeedback } from '@/api/feedback'
import { getOverview } from '@/api/statistics'
import {
  DataAnalysis, CircleCheck, ArrowRight, ArrowLeft,
  Odometer, LocationInformation, EditPen, Document,
  Warning, Monitor, DataLine, Reading, Collection
} from '@element-plus/icons-vue'

import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
const userName = computed(() => userStore.userName || '公众监督员')
const feedLoading = ref(false)
const recentFeedbacks = ref([])
const currentDate = ref(new Date())
const calendarRef = ref(null)
const feedbackDates = ref([])
const stats = ref({ myFeedbacks: 0, pendingCount: 0, completedCount: 0, aqiTotal: 0 })

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，'
  if (h < 12) return '晨安，'
  if (h < 14) return '午安，'
  if (h < 18) return '下午好，'
  return '晚安，'
})

const today = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    month: 'long', day: 'numeric', weekday: 'long'
  })
})

function statusLabel(s) {
  return s === 'PENDING' ? '排队中' : s === 'ASSIGNED' ? '处理中' : '已闭环'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

// 触发日历月份切换
const selectDate = (val) => {
  if (!calendarRef.value) return
  calendarRef.value.selectDate(val)
}

// 判断日期是否有反馈记录（日历小圆点标记）
const hasFeedback = (dateStr) => {
  return feedbackDates.value.includes(dateStr)
}

onMounted(async () => {
  feedLoading.value = true
  const uid = localStorage.getItem('userId')

  if (uid) {
    try {
      const res = await getMyFeedback()
      const data = res.data || []
      recentFeedbacks.value = data.slice(0, 6)
      stats.value.myFeedbacks = data.length
      stats.value.pendingCount = data.filter(f => f.status === 'PENDING').length
      stats.value.completedCount = data.filter(f => f.status === 'COMPLETED').length
      // 提取反馈日期用于日历标记
      feedbackDates.value = [...new Set(
        data.filter(f => f.createTime).map(f => f.createTime.substring(0, 10))
      )]
    } catch (e) { }
  }

  try {
    const r = await getOverview()
    if (r.data) stats.value.aqiTotal = r.data.totalDetections || 0
  } catch (e) { } finally {
    feedLoading.value = false
  }
})
</script>

<style scoped>
/* ========== 全局架构 ========== */
.swiss-dashboard {
  max-width: 1440px;
  width: 100%;
  height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
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
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
}

.scroll-area::-webkit-scrollbar {
  display: none;
}

/* ========== 1. 欢迎区 (晨曦绿 + 呼吸动态) ========== */
.hero-premium-card {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 36px 48px;
  /* 更清透、明亮的生态晨曦绿 */
  background: linear-gradient(135deg, #43C6AC 0%, #191654 150%);
  border-radius: 24px;
  box-shadow: 0 16px 40px -8px rgba(67, 198, 172, 0.4);
  color: #FFFFFF;
  overflow: hidden;
}

/* 呼吸光影层 */
.hero-breathe-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  z-index: 0;
  pointer-events: none;
  animation: breathe 8s infinite alternate cubic-bezier(0.45, 0.05, 0.55, 0.95);
}

.glow-1 {
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.2);
  top: -100px;
  left: -50px;
}

.glow-2 {
  width: 400px;
  height: 400px;
  background: rgba(133, 199, 122, 0.25);
  bottom: -150px;
  right: -50px;
  animation-delay: -4s;
}

@keyframes breathe {
  0% {
    transform: scale(0.9) translate(0, 0);
    opacity: 0.5;
  }

  100% {
    transform: scale(1.1) translate(5%, 5%);
    opacity: 1;
  }
}

.hero-content,
.hero-status {
  position: relative;
  z-index: 1;
}

.hero-greeting {
  font-size: 26px;
  font-weight: 400;
  margin: 0 0 8px 0;
  letter-spacing: 0.5px;
  color: rgba(255, 255, 255, 0.9);
}

.hero-name {
  font-weight: 600;
  color: #FFFFFF;
}

.hero-date {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  letter-spacing: 0.5px;
}

.hero-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 100px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
}

.status-icon-ring {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #FFFFFF;
  color: #2AA876;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
}

.status-title {
  font-size: 14px;
  font-weight: 600;
  color: #FFFFFF;
}

.status-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 2px;
}

/* ========== 2. 核心指标卡 ========== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-glass-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  padding: 24px;
  transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  cursor: pointer;
}

.stat-glass-card:hover {
  background: #FFF;
  transform: translateY(-4px);
  box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.08);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-label {
  font-size: 14px;
  font-weight: 600;
  color: #74807B;
}

.stat-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(28, 36, 33, 0.04);
  color: #2A483A;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
}

.stat-body {
  margin-bottom: 16px;
}

.value-number {
  font-size: 32px;
  font-weight: 700;
  color: #1C2421;
}

.stat-footer {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
}

.trend-context {
  color: #A0AAB2;
}

/* ========== 3. 分栏内容区 (新三栏布局) ========== */
.content-split-grid {
  display: grid;
  /* 左边快捷方式，中间反馈流压缩，右边嵌入式日历 */
  grid-template-columns: minmax(280px, 1fr) 1.5fr minmax(320px, 1.2fr);
  gap: 24px;
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(28, 36, 33, 0.06);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.text-link-btn {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: #2A483A;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

/* 调度便当盒 */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 24px;
  align-content: start;
}

.bento-item {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.bento-item:hover {
  background: #FFF;
  box-shadow: 0 8px 24px -6px rgba(0, 0, 0, 0.06);
  transform: scale(1.02);
}

.bento-icon {
  font-size: 26px;
  color: #2A483A;
}

.bento-label {
  font-size: 13px;
  font-weight: 600;
  color: #1C2421;
}

/* 近期案件流 */
.stream-list {
  padding: 12px 28px 28px;
}

.stream-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(28, 36, 33, 0.06);
  cursor: pointer;
  transition: all 0.3s;
}

.stream-item:last-child {
  border-bottom: none;
}

.stream-item:hover {
  padding: 16px 12px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  border-bottom-color: transparent;
}

.aqi-indicator {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(28, 36, 33, 0.03);
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.aqi-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.aqi-level-1 .aqi-dot,
.aqi-level-2 .aqi-dot {
  background: #2AA876;
  box-shadow: 0 0 10px rgba(42, 168, 118, 0.4);
}

.aqi-level-3 .aqi-dot {
  background: #F5A623;
  box-shadow: 0 0 10px rgba(245, 166, 35, 0.4);
}

.aqi-level-4 .aqi-dot,
.aqi-level-5 .aqi-dot,
.aqi-level-6 .aqi-dot {
  background: #D9534F;
  box-shadow: 0 0 10px rgba(217, 83, 79, 0.4);
}

.stream-content {
  flex: 1;
  min-width: 0;
}

.stream-desc {
  font-size: 14px;
  font-weight: 500;
  color: #1C2421;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.stream-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #74807B;
}

.stream-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.status-pill {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-pending {
  background: rgba(245, 166, 35, 0.15);
  color: #F5A623;
}

.status-assigned {
  background: rgba(64, 158, 255, 0.15);
  color: #409EFF;
}

.status-completed {
  background: rgba(42, 168, 118, 0.15);
  color: #2AA876;
}

.stream-time {
  font-size: 12px;
  color: #A0AAB2;
}

/* 零数据空状态指引 */
.empty-guide-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 250px;
  text-align: center;
}

.empty-icon-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(42, 72, 58, 0.05);
  color: #2A483A;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 32px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #1C2421;
  margin: 0 0 8px;
}

.empty-sub {
  font-size: 13px;
  color: #74807B;
  margin: 0 0 24px;
  max-width: 300px;
  line-height: 1.5;
}

.swiss-action-btn {
  background: #2A483A;
  color: #FFF;
  border: none;
  padding: 10px 24px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.swiss-action-btn:hover {
  background: #1C2421;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(42, 72, 58, 0.3);
}

/* =========================================
   专属定制：无边界嵌入式日历
   ========================================= */
.calendar-bento {
  background: transparent;
  border: none;
  box-shadow: none;
}

.custom-calendar-wrapper {
  padding: 0;
}

/* 自定义日历头部控制器 */
.embedded-cal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.cal-title {
  font-size: 18px;
  font-weight: 700;
  color: #1C2421;
  letter-spacing: 0.5px;
}

.cal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cal-arrow-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(28, 36, 33, 0.05);
  color: #74807B;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s;
}

.cal-arrow-btn:hover {
  background: #FFF;
  color: #1C2421;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* "今日" 按钮 */
.cal-today-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 32px;
  border-radius: 16px;
  border: none;
  background: #FFF;
  color: #2A483A;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all 0.2s;
}

.cal-today-btn:hover {
  box-shadow: 0 4px 16px rgba(42, 72, 58, 0.15);
  transform: translateY(-1px);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2AA876;
  box-shadow: 0 0 0 0 rgba(42, 168, 118, 0.5);
  animation: live-pulse 2s infinite;
}

@keyframes live-pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(42, 168, 118, 0.5);
  }

  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(42, 168, 118, 0);
  }

  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(42, 168, 118, 0);
  }
}

/* 覆盖 Element Plus 日历原生样式 */
.custom-calendar-wrapper :deep(.el-calendar) {
  background: transparent;
}

.custom-calendar-wrapper :deep(.el-calendar__header) {
  border: none;
  padding: 12px 16px 24px;
}

.custom-calendar-wrapper :deep(.el-calendar-table td) {
  border: none;
}

.custom-calendar-wrapper :deep(.el-calendar-table th) {
  padding-bottom: 12px;
  font-weight: 600;
  color: #74807B;
}

.custom-calendar-wrapper :deep(.el-calendar-table .el-calendar-day) {
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 14px;
  transition: all 0.2s;
  font-size: 14px;
  font-weight: 500;
  margin: 2px 4px;
}

.custom-calendar-wrapper :deep(.el-calendar-table .el-calendar-day:hover) {
  background: rgba(255, 255, 255, 0.6);
}

/* 今天：绿色数字 */
.custom-calendar-wrapper :deep(.el-calendar-table td.is-today .el-calendar-day) {
  color: #2AA876;
  font-weight: 700;
}

.custom-calendar-wrapper :deep(.el-calendar-table td.is-selected .el-calendar-day) {
  background: #2A483A;
  color: #FFF;
  box-shadow: 0 6px 16px rgba(42, 72, 58, 0.25);
}

/* 日历日期单元格 */
.cal-day-cell {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.cal-day-num {
  line-height: 1;
}

/* 有反馈记录的小圆点 */
.cal-feedback-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #409EFF;
  animation: dot-appear 0.3s ease-out;
}

@keyframes dot-appear {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* 今天所在的单元格有反馈时小圆点变绿 */
.custom-calendar-wrapper :deep(.el-calendar-table td.is-today) .cal-feedback-dot {
  background: #2AA876;
  box-shadow: 0 0 4px rgba(42, 168, 118, 0.5);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-split-grid {
    grid-template-columns: 1fr 1.5fr;
  }

  .calendar-bento {
    display: none;
  }
}
</style>