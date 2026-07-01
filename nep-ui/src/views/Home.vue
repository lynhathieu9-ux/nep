<template>
  <div class="home-dashboard">
    <!-- 欢迎横幅 -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-greeting">{{ greeting }} <span class="hero-name">{{ userStore.user?.realName || '用户' }}</span></h1>
        <p class="hero-date">{{ today }} · 环保监测网络运行良好</p>
      </div>
      <div class="hero-status">
        <div class="status-dot"></div>
        <div class="status-info">
          <div class="status-title">实时监测中</div>
          <div class="status-subtitle">已覆盖全国 106 座城市</div>
        </div>
      </div>
    </section>

    <!-- 统计数据 KPI -->
    <section class="stats-grid">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-header">
          <span class="stat-label">{{ card.label }}</span>
          <div class="stat-icon">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
        </div>
        <div class="stat-value">
          <span class="value-number">{{ card.value }}</span>
          <span class="value-unit">{{ card.unit }}</span>
        </div>
        <div class="stat-footer">
          <span class="trend" :class="card.trend > 0 ? 'trend-up' : 'trend-down'">
            <el-icon><component :is="card.trend > 0 ? 'TopRight' : 'BottomRight'" /></el-icon>
            {{ Math.abs(card.trend) }}%
          </span>
          <span class="trend-label">较上周</span>
        </div>
      </div>
    </section>

    <!-- 快捷操作 + 最新反馈 -->
    <section class="content-split">
      <!-- 快捷操作 -->
      <div class="panel-card">
        <div class="panel-header">
          <h3 class="panel-title">快捷操作</h3>
        </div>
        <div class="action-grid">
          <div class="action-item" v-for="act in quickActions" :key="act.label" @click="$router.push(act.path)">
            <div class="action-icon"><el-icon><component :is="act.icon" /></el-icon></div>
            <span class="action-label">{{ act.label }}</span>
          </div>
        </div>
      </div>

      <!-- 最新反馈流 -->
      <div class="panel-card">
        <div class="panel-header">
          <h3 class="panel-title">最新监督反馈</h3>
          <button class="link-btn" @click="$router.push('/feedback')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="feedback-list" v-loading="feedLoading">
          <div
            class="feedback-item"
            v-for="fb in recentFeedbacks"
            :key="fb.id"
            @click="$router.push('/feedback')"
          >
            <div class="aqi-dot-indicator" :class="'aqi-' + fb.estimatedAqiLevel"></div>
            <div class="feedback-body">
              <div class="feedback-desc">{{ fb.description || '常规空气质量记录' }}</div>
              <div class="feedback-meta text-meta">
                <el-icon><LocationInformation /></el-icon>
                {{ fb.specificAddress || '网格区域 #' + fb.cityId }}
              </div>
            </div>
            <div class="feedback-status">
              <span class="status-badge" :class="'badge-' + fb.status.toLowerCase()">
                {{ statusLabel(fb.status) }}
              </span>
              <span class="text-meta">{{ formatTime(fb.createTime) }}</span>
            </div>
          </div>
          <div v-if="!feedLoading && recentFeedbacks.length === 0" class="empty-inline">
            <p class="text-meta">暂无最新反馈</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getFeedbackPage } from '@/api/feedback'
import {
  User, DataAnalysis, CircleCheck, Location, ArrowRight,
  TopRight, BottomRight, Odometer, LocationInformation,
  EditPen, Document, ChatDotRound, PieChart, Setting
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const feedLoading = ref(false)
const recentFeedbacks = ref([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，'
  if (h < 12) return '早上好，'
  if (h < 14) return '中午好，'
  if (h < 18) return '下午好，'
  return '晚上好，'
})

const today = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    month: 'long', day: 'numeric', weekday: 'long'
  })
})

const statCards = ref([
  { label: '公众监督员', value: '12,846', unit: '人', icon: User, trend: 12 },
  { label: 'AQI 监测总数', value: '35,862', unit: '次', icon: DataAnalysis, trend: 8 },
  { label: '反馈处理率', value: '87.5', unit: '%', icon: CircleCheck, trend: -3 },
  { label: '覆盖城市', value: '106', unit: '座', icon: Location, trend: 15 },
])

const quickActions = [
  { label: '提交反馈', icon: EditPen, path: '/feedback/submit' },
  { label: '反馈列表', icon: Document, path: '/feedback' },
  { label: 'AI 助手', icon: ChatDotRound, path: '/ai' },
  { label: '数据大盘', icon: PieChart, path: '/statistics' },
  { label: '个人中心', icon: User, path: '/profile' },
  { label: '管理后台', icon: Setting, path: '/admin/dashboard' },
]

function statusLabel(s) {
  return s === 'PENDING' ? '待指派' : s === 'ASSIGNED' ? '处理中' : '已完成'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  feedLoading.value = true
  try {
    const res = await getFeedbackPage(1, 10)
    recentFeedbacks.value = res.data || []
  } catch (e) {} finally { feedLoading.value = false }
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.home-dashboard {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== 欢迎横幅 ===== */
.hero-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32px 40px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}
.hero-greeting { font-size: 24px; font-weight: 400; margin: 0 0 6px; color: var(--text-secondary); }
.hero-name { font-weight: 600; color: var(--text-primary); }
.hero-date { font-size: 14px; color: var(--text-secondary); margin: 0; }

.hero-status {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 20px; background: #F8FAF8;
  border: 1px solid var(--border-color); border-radius: 20px;
}
.status-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--color-primary); box-shadow: 0 0 8px rgba(82, 155, 46, 0.4);
  animation: pulse-dot 2s infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.status-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.status-subtitle { font-size: 12px; color: var(--text-meta); }

/* ===== KPI 卡片网格 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
  transition: all var(--transition-normal);
  cursor: pointer;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.stat-label { font-size: 13px; font-weight: 500; color: var(--text-secondary); }
.stat-icon {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--color-primary-bg); color: var(--color-primary);
  display: flex; justify-content: center; align-items: center; font-size: 16px;
}

.value-number { font-size: 30px; font-weight: 700; color: var(--text-primary); }
.value-unit { font-size: 13px; color: var(--text-secondary); margin-left: 4px; font-weight: 500; }

.stat-footer { display: flex; align-items: center; gap: 6px; margin-top: 12px; font-size: 12px; }
.trend { display: inline-flex; align-items: center; gap: 2px; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.trend-up { background: rgba(82, 155, 46, 0.1); color: var(--color-primary); }
.trend-down { background: rgba(217, 83, 79, 0.1); color: #D9534F; }
.trend-label { color: var(--text-meta); }

/* ===== 双栏内容区 ===== */
.content-split {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* ===== 面板卡片 ===== */
.panel-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  flex-shrink: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--divider);
}
.panel-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

.link-btn {
  background: transparent; border: none; cursor: pointer;
  font-size: 13px; font-weight: 500; color: var(--color-primary);
  display: flex; align-items: center; gap: 4px;
}
.link-btn:hover { opacity: 0.75; }

/* ===== 快捷操作网格 ===== */
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 18px 20px;
  flex: 1;
  align-content: start;
}

.action-item {
  background: #F8FAF8;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all var(--transition-normal);
}
.action-item:hover {
  background: var(--bg-card);
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}
.action-icon { font-size: 22px; color: var(--color-primary); }
.action-label { font-size: 12px; font-weight: 500; color: var(--text-primary); }

/* ===== 反馈流列表 ===== */
.feedback-list {
  padding: 8px 20px 20px;
  flex: 1;
  overflow-y: auto;
}

.feedback-item {
  display: flex; align-items: center; gap: 14px; padding: 14px 0;
  border-bottom: 1px solid var(--divider); cursor: pointer;
  transition: padding var(--transition-fast);
}
.feedback-item:last-child { border-bottom: none; }
.feedback-item:hover { padding-left: 6px; padding-right: 6px; }

/* AQI 指示点 */
.aqi-dot-indicator {
  width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0;
}
.aqi-1, .aqi-2 { background: #2AA876; box-shadow: 0 0 8px rgba(42,168,118,0.4); }
.aqi-3 { background: #F5A623; box-shadow: 0 0 8px rgba(245,166,35,0.4); }
.aqi-4, .aqi-5, .aqi-6 { background: #D9534F; box-shadow: 0 0 8px rgba(217,83,79,0.4); }

.feedback-body { flex: 1; min-width: 0; }
.feedback-desc {
  font-size: 14px; font-weight: 500; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;
}
.feedback-meta { display: flex; align-items: center; gap: 4px; font-size: 12px; }

.feedback-status {
  display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0;
}
.status-badge { padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.badge-pending { background: rgba(245, 166, 35, 0.1); color: #B87A14; }
.badge-assigned { background: rgba(64, 158, 255, 0.1); color: #2E7BC4; }
.badge-completed { background: rgba(82, 155, 46, 0.1); color: #3B7620; }

.empty-inline { text-align: center; padding: 32px 0; }

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .content-split { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .home-dashboard { padding: 16px; gap: 16px; }
  .hero-section { flex-direction: column; gap: 16px; padding: 24px; text-align: center; }
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
