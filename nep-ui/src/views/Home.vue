<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="welcome-greeting">
          <span class="welcome-emoji">👋</span>
          <span>{{ greeting }}</span>
        </div>
        <h1 class="welcome-name">{{ userStore.user?.realName || '用户' }}</h1>
        <p class="welcome-date">{{ today }} · 祝您工作愉快</p>
      </div>
      <div class="welcome-right">
        <div class="weather-card">
          <div class="weather-icon">🌤️</div>
          <div class="weather-info">
            <div class="weather-temp">空气质量监测中</div>
            <div class="weather-desc">全国 29 省 · 106 城网格覆盖</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ '--card-color': card.color }">
          <div class="stat-body">
            <div class="stat-main">
              <div class="stat-number">
                <span ref="countRefs" class="count-num">{{ card.value }}</span>
                <span class="count-unit">{{ card.unit }}</span>
              </div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
            <div class="stat-icon-box" :style="{ background: card.color }">
              <el-icon :size="26" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend" :class="card.trend > 0 ? 'up' : 'down'">
              <el-icon><component :is="card.trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(card.trend) }}%
            </span>
            <span class="stat-compare">较上周</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 + 最新反馈 -->
    <el-row :gutter="20">
      <el-col :span="10">
        <div class="quick-card">
          <div class="card-header">
            <h3>快捷操作</h3>
          </div>
          <div class="quick-grid">
            <div class="quick-item" v-for="act in quickActions" :key="act.label" @click="$router.push(act.path)">
              <div class="quick-icon" :style="{ background: act.bg }">
                <el-icon :size="24" :color="act.color"><component :is="act.icon" /></el-icon>
              </div>
              <div class="quick-info">
                <div class="quick-name">{{ act.label }}</div>
                <div class="quick-desc">{{ act.desc }}</div>
              </div>
              <el-icon class="quick-arrow" color="#ccc"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="14">
        <div class="feedback-card">
          <div class="card-header">
            <h3>最新监督反馈</h3>
            <el-button text type="primary" @click="$router.push('/feedback')">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
          </div>
          <div class="feedback-list" v-loading="feedLoading">
            <div
              class="feedback-item"
              v-for="fb in recentFeedbacks"
              :key="fb.id"
              @click="$router.push('/feedback')"
            >
              <div class="fb-left">
                <div class="fb-avatar" :class="'aqi-'+fb.estimatedAqiLevel">
                  {{ fb.estimatedAqiLevel }}
                </div>
              </div>
              <div class="fb-center">
                <div class="fb-address">
                  <el-icon><Location /></el-icon>
                  {{ fb.specificAddress || '网格区域 #' + fb.cityId }}
                </div>
                <div class="fb-desc">{{ fb.description || '空气质量监督反馈' }}</div>
              </div>
              <div class="fb-right">
                <el-tag :type="statusType(fb.status)" size="small" effect="dark" round>
                  {{ statusLabel(fb.status) }}
                </el-tag>
                <div class="fb-time">{{ formatTime(fb.createTime) }}</div>
              </div>
            </div>
            <el-empty v-if="!feedLoading && recentFeedbacks.length === 0" description="暂无反馈数据" :image-size="80" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getFeedbackPage } from '@/api/feedback'

const userStore = useUserStore()
const feedLoading = ref(false)
const recentFeedbacks = ref([])

// 问候语
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
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
  })
})

// 统计卡片
const statCards = ref([
  { label: '公众监督员', value: '12,846', unit: '人', color: '#409EFF', icon: 'User', trend: 12 },
  { label: 'AQI检测总数', value: '35,862', unit: '次', color: '#0c8c3f', icon: 'DataAnalysis', trend: 8 },
  { label: '反馈处理率', value: '87.5', unit: '%', color: '#E6A23C', icon: 'CircleCheck', trend: -3 },
  { label: '覆盖城市', value: '106', unit: '座', color: '#F56C6C', icon: 'Location', trend: 15 },
])

// 快捷操作
const quickActions = [
  { label: '提交反馈', desc: '上报空气质量信息', icon: 'EditPen', color: '#409EFF', bg: '#ecf5ff', path: '/feedback/submit' },
  { label: '反馈列表', desc: '查看历史记录', icon: 'Document', color: '#0c8c3f', bg: '#e8f5e9', path: '/feedback' },
  { label: 'AI助手', desc: '智能问答分析', icon: 'ChatDotRound', color: '#9b59b6', bg: '#f3e5f5', path: '/ai' },
  { label: '数据统计', desc: '查看统计大盘', icon: 'PieChart', color: '#E6A23C', bg: '#fdf6ec', path: '/statistics' },
  { label: '个人中心', desc: '管理账户信息', icon: 'User', color: '#F56C6C', bg: '#fef0f0', path: '/profile' },
  { label: '管理后台', desc: '系统管理端', icon: 'Setting', color: '#303133', bg: '#f4f4f5', path: '/admin/dashboard' },
]

function statusType(s) {
  return s === 'PENDING' ? 'warning' : s === 'ASSIGNED' ? 'primary' : 'success'
}

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
    const res = await getFeedbackPage(1, 5)
    recentFeedbacks.value = res.data || []
  } catch (e) {} finally { feedLoading.value = false }
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

/* ========== 欢迎横幅 ========== */
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #0c8c3f 0%, #1a6b3a 50%, #0f4c2f 100%);
  border-radius: 16px;
  padding: 32px 36px;
  margin-bottom: 24px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  right: -40px;
  top: -40px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
}

.welcome-banner::before {
  content: '';
  position: absolute;
  right: 80px;
  bottom: -60px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255,255,255,0.04);
}

.welcome-left { position: relative; z-index: 1; }
.welcome-greeting { font-size: 15px; opacity: 0.85; margin-bottom: 6px; }
.welcome-emoji { font-size: 22px; margin-right: 6px; animation: wave 1.5s infinite; display: inline-block; }
@keyframes wave { 0%,100% { transform: rotate(0); } 25% { transform: rotate(15deg); } 75% { transform: rotate(-15deg); } }

.welcome-name { font-size: 28px; font-weight: 700; margin: 0 0 4px 0; letter-spacing: 1px; }
.welcome-date { font-size: 13px; opacity: 0.7; margin: 0; }

.welcome-right { position: relative; z-index: 1; }
.weather-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(10px);
  padding: 16px 22px;
  border-radius: 14px;
}
.weather-icon { font-size: 36px; }
.weather-temp { font-size: 16px; font-weight: 600; }
.weather-desc { font-size: 12px; opacity: 0.7; margin-top: 2px; }

/* ========== 统计卡片 ========== */
.stats-row { margin-bottom: 24px; }

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
  background: var(--card-color);
  transition: width 0.3s;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px rgba(0,0,0,0.1);
}
.stat-card:hover::after { width: 6px; }

.stat-body { display: flex; justify-content: space-between; align-items: flex-start; }
.count-num { font-size: 28px; font-weight: 800; color: #1a1a1a; }
.count-unit { font-size: 14px; color: #999; margin-left: 4px; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }

.stat-icon-box {
  width: 50px; height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-footer { margin-top: 16px; padding-top: 14px; border-top: 1px solid #f5f5f5; font-size: 12px; }
.stat-trend { font-weight: 600; }
.stat-trend.up { color: #0c8c3f; }
.stat-trend.down { color: #F56C6C; }
.stat-compare { color: #ccc; margin-left: 6px; }

/* ========== 快捷操作 ========== */
.quick-card, .feedback-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
  margin-bottom: 8px;
}
.card-header h3 { font-size: 17px; font-weight: 700; margin: 0; }

.quick-grid { padding: 8px 24px 20px; }
.quick-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
}
.quick-item:hover { background: #f9fafb; transform: translateX(4px); }

.quick-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.quick-info { flex: 1; }
.quick-name { font-size: 14px; font-weight: 600; color: #303133; }
.quick-desc { font-size: 12px; color: #999; margin-top: 2px; }

/* ========== 反馈列表 ========== */
.feedback-list { padding: 0 24px 16px; max-height: 540px; overflow-y: auto; }

.feedback-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  margin-bottom: 4px;
}
.feedback-item:hover { background: #f9fafb; }

.fb-left { flex-shrink: 0; }
.fb-avatar {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 16px; color: #fff;
}
.fb-avatar.aqi-1, .fb-avatar.aqi-2 { background: #00e400; }
.fb-avatar.aqi-3 { background: #ff7e00; }
.fb-avatar.aqi-4 { background: #ff0000; }
.fb-avatar.aqi-5 { background: #99004c; }
.fb-avatar.aqi-6 { background: #7e0023; }

.fb-center { flex: 1; min-width: 0; }
.fb-address { font-size: 13px; color: #666; display: flex; align-items: center; gap: 4px; }
.fb-desc { font-size: 14px; font-weight: 500; color: #303133; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.fb-right { text-align: right; flex-shrink: 0; }
.fb-time { font-size: 11px; color: #ccc; margin-top: 4px; }

@media (max-width: 1200px) {
  .stats-row .el-col { flex: 0 0 50%; max-width: 50%; margin-bottom: 16px; }
}
</style>
