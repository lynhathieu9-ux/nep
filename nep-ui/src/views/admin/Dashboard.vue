<template>
  <!-- 剥离一切多余外观，只作为隐形 Flex 布局主干 -->
  <div class="admin-dashboard-premium" v-loading="loading">
    
    <!-- 顶部核心指标区 -->
    <div class="stats-grid">
      <div class="premium-card stat-card">
        <div class="card-header-mini">
          <span class="header-title">注册用户总数</span>
          <div class="icon-box bg-slate"><el-icon><User /></el-icon></div>
        </div>
        <div class="stat-value">{{ stats.userTotal }}</div>
        <div class="stat-meta text-slate">
          监督员 {{ stats.supervisor }} <span class="divider"></span> 网格员 {{ stats.inspector }}
        </div>
      </div>

      <div class="premium-card stat-card">
        <div class="card-header-mini">
          <span class="header-title">监督反馈总数</span>
          <div class="icon-box bg-amber"><el-icon><Document /></el-icon></div>
        </div>
        <div class="stat-value">{{ stats.feedbackTotal }}</div>
        <div class="stat-meta text-amber">
          待指派 {{ stats.pending }} 条
        </div>
      </div>

      <div class="premium-card stat-card">
        <div class="card-header-mini">
          <span class="header-title">AQI 检测总数</span>
          <div class="icon-box bg-rose"><el-icon><Odometer /></el-icon></div>
        </div>
        <div class="stat-value">{{ stats.aqiTotal }}</div>
        <div class="stat-meta text-rose">
          超标 {{ stats.aqiExceed }} 次 ({{ stats.aqiExceedRate }})
        </div>
      </div>

      <div class="premium-card stat-card">
        <div class="card-header-mini">
          <span class="header-title">已完成工单</span>
          <div class="icon-box bg-emerald"><el-icon><CircleCheck /></el-icon></div>
        </div>
        <div class="stat-value">{{ stats.completed }}</div>
        <div class="stat-meta text-emerald">
          升级中 {{ stats.escalated }} 条
        </div>
      </div>
    </div>

    <!-- 下部复合内容区（完美贴合首屏剩余空间） -->
    <div class="content-grid">
      
      <!-- 左侧：320px 黄金紧凑宽度 -->
      <div class="left-column">
        <!-- 状态分布卡片 -->
        <div class="premium-card flex-card bar-chart-card">
          <div class="section-header">
            <div class="header-icon"><el-icon><DataLine /></el-icon></div>
            <h3 class="section-title">工单流转状态</h3>
          </div>
          <div class="status-bars">
            <div class="status-bar-item" v-for="s in statusList" :key="s.key">
              <div class="bar-label">{{ s.label }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(s.value), background: s.color }"></div>
              </div>
              <div class="bar-value" :style="{ color: s.color }">{{ s.value }}</div>
            </div>
          </div>
        </div>

        <!-- 快捷操作卡片 -->
        <div class="premium-card fixed-card action-card">
          <div class="section-header">
            <div class="header-icon"><el-icon><Lightning /></el-icon></div>
            <h3 class="section-title">快捷操作</h3>
          </div>
          <div class="action-buttons">
            <button class="fluent-btn btn-slate" @click="$router.push('/admin/users')">
              <el-icon><UserFilled /></el-icon> 用户管理
            </button>
            <button class="fluent-btn btn-amber" @click="$router.push('/admin/feedbacks')">
              <el-icon><Position /></el-icon> 反馈指派
            </button>
            <button class="fluent-btn btn-emerald" @click="$router.push('/admin/statistics')">
              <el-icon><TrendCharts /></el-icon> 数据统计
            </button>
            <button class="fluent-btn btn-blue" @click="$router.push('/nepv/dashboard')">
              <el-icon><Monitor /></el-icon> 数据大屏
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：地图区域（引入绝对定位骨架，根治截断） -->
      <div class="right-column">
        <div class="premium-card flex-card map-card">
          <div class="section-header">
            <div class="header-icon"><el-icon><MapLocation /></el-icon></div>
            <h3 class="section-title">全国污染热点概览</h3>
            <span class="header-subtitle">点击省份区域下钻探测</span>
          </div>
          <!-- 绝对定位约束舱：物理级隔离 Canvas 的胡乱撑开 -->
          <div class="map-bounding-box">
            <MapView class="absolute-map" />
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserStats } from '@/api/user'
import { getFeedbackOverview } from '@/api/feedback'
import { getAqiOverview } from '@/api/aqi'
import MapView from '@/components/MapView.vue'

import { 
  User, Document, Odometer, CircleCheck, 
  DataLine, Lightning, MapLocation, 
  UserFilled, Position, TrendCharts, Monitor 
} from '@element-plus/icons-vue'

const loading = ref(false)
const stats = ref({
  userTotal: 0, supervisor: 0, inspector: 0,
  feedbackTotal: 0, pending: 0, assigned: 0, completed: 0, escalated: 0,
  aqiTotal: 0, aqiExceed: 0, aqiExceedRate: '0.0%'
})

const statusList = computed(() => [
  { key: 'pending', label: '待指派', value: stats.value.pending, color: '#D97706' },
  { key: 'assigned', label: '检测中', value: stats.value.assigned, color: '#0284C7' },
  { key: 'completed', label: '已完成', value: stats.value.completed, color: '#059669' },
  { key: 'escalated', label: '已升级', value: stats.value.escalated, color: '#E11D48' }
])

const maxStatus = computed(() => Math.max(1, ...statusList.value.map(s => s.value)))
const barWidth = (v) => `${Math.round((v / maxStatus.value) * 100)}%`

onMounted(async () => {
  loading.value = true
  try {
    const r = await getUserStats()
    stats.value.userTotal = r.data.total
    stats.value.supervisor = r.data.supervisor
    stats.value.inspector = r.data.inspector
  } catch (e) {}
  try {
    const r = await getFeedbackOverview()
    stats.value.feedbackTotal = r.data.total
    stats.value.pending = r.data.pending
    stats.value.assigned = r.data.assigned
    stats.value.completed = r.data.completed
    stats.value.escalated = r.data.escalated
  } catch (e) {}
  try {
    const r = await getAqiOverview()
    stats.value.aqiTotal = r.data.total
    stats.value.aqiExceed = r.data.exceed
    stats.value.aqiExceedRate = r.data.exceedRate
  } catch (e) {}
  loading.value = false
})
</script>

<style scoped>
/* ========== 全局容器与重置 ========== */
.admin-dashboard-premium {
  height: 100%; 
  display: flex;
  flex-direction: column;
  gap: 16px; 
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
  color: #1D1D1F;
}

/* ========== 高定悬浮卡片基类 ========== */
.premium-card {
  background: #FFFFFF;
  border-radius: 16px; 
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.02);
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  /* 物理弹簧曲线微交互 */
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.4s ease;
}
.premium-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* ========== 错野进场动效 (Staggered Entry) ========== */
@keyframes slideUpFade {
  0% { opacity: 0; transform: translateY(24px); }
  100% { opacity: 1; transform: translateY(0); }
}

.stats-grid .stat-card { animation: slideUpFade 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) backwards; }
.stats-grid .stat-card:nth-child(1) { animation-delay: 0.1s; }
.stats-grid .stat-card:nth-child(2) { animation-delay: 0.2s; }
.stats-grid .stat-card:nth-child(3) { animation-delay: 0.3s; }
.stats-grid .stat-card:nth-child(4) { animation-delay: 0.4s; }

.left-column .premium-card { animation: slideUpFade 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) backwards; }
.left-column .premium-card:nth-child(1) { animation-delay: 0.5s; }
.left-column .premium-card:nth-child(2) { animation-delay: 0.6s; }
.right-column { animation: slideUpFade 0.9s cubic-bezier(0.2, 0.8, 0.2, 1) backwards; animation-delay: 0.7s; }

/* ========== 核心指标矩阵 ========== */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; flex-shrink: 0; }
.card-header-mini { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.header-title { font-size: 13px; font-weight: 600; color: #86868B; }
.icon-box {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; justify-content: center; align-items: center; font-size: 15px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.premium-card:hover .icon-box { transform: scale(1.15) rotate(-3deg); }

.bg-slate { background: #F1F5F9; color: #475569; }
.bg-amber { background: #FEF3C7; color: #D97706; }
.bg-rose { background: #FFE4E6; color: #E11D48; }
.bg-emerald { background: #D1FAE5; color: #059669; }

.stat-value { font-size: 32px; font-weight: 700; color: #1D1D1F; line-height: 1; letter-spacing: -0.5px; margin-bottom: 10px; }
.stat-meta { font-size: 12px; font-weight: 500; display: flex; align-items: center; }
.divider { width: 3px; height: 3px; background: currentColor; border-radius: 50%; margin: 0 8px; opacity: 0.5; }
.text-slate { color: #64748B; }
.text-amber { color: #D97706; }
.text-rose { color: #E11D48; }
.text-emerald { color: #059669; }

/* ========== 复合内容网格 ========== */
.content-grid {
  flex: 1; min-height: 0; 
  display: grid; grid-template-columns: 320px 1fr; gap: 16px;
}
.left-column, .right-column { display: flex; flex-direction: column; min-height: 0; gap: 16px; }
.flex-card { flex: 1; min-height: 0; } 
.fixed-card { flex-shrink: 0; } 

.section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-shrink: 0; }
.header-icon { font-size: 16px; color: #1D1D1F; display: flex; align-items: center; }
.section-title { margin: 0; font-size: 15px; font-weight: 600; color: #1D1D1F; }
.header-subtitle { font-size: 12px; color: #86868B; margin-left: auto; }

/* 状态条 */
.status-bars { flex: 1; display: flex; flex-direction: column; justify-content: space-evenly; gap: 10px; }
.status-bar-item { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 48px; font-size: 12px; font-weight: 500; color: #86868B; flex-shrink: 0; }
.bar-track { flex: 1; height: 6px; background: #F1F5F9; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.8s cubic-bezier(0.2, 0.8, 0.2, 1); }
.bar-value { width: 36px; text-align: right; font-size: 14px; font-weight: 600; flex-shrink: 0; font-feature-settings: "tnum"; }

/* 快捷按键 */
.action-buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.fluent-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px; border-radius: 10px; border: none;
  font-size: 13px; font-weight: 600; cursor: pointer; 
  transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.2, 1), background 0.2s ease;
}
.fluent-btn:hover { transform: translateY(-2px) scale(1.02); }
.fluent-btn:active { transform: translateY(1px) scale(0.98); }
.btn-slate { background: #F1F5F9; color: #1D1D1F; }
.btn-slate:hover { background: #E2E8F0; }
.btn-amber { background: #FFFBEB; color: #D97706; }
.btn-amber:hover { background: #FEF3C7; }
.btn-emerald { background: #ECFDF5; color: #059669; }
.btn-emerald:hover { background: #D1FAE5; }
.btn-blue { background: #F0F9FF; color: #0284C7; }
.btn-blue:hover { background: #E0F2FE; }

/* ========== 地图绝对定位约束舱 ========== */
.map-bounding-box {
  flex: 1; position: relative; width: 100%; height: 100%;
  border-radius: 10px; background: #F8FAFC; overflow: hidden;
}
.absolute-map { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }

/* 响应式降级 */
@media (max-width: 1200px) {
  .admin-dashboard-premium { height: auto; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
  .map-bounding-box { min-height: 400px; position: static; }
  .absolute-map { position: static; height: 100%; }
}
</style>