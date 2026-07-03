<template>
  <div class="admin-dashboard" v-loading="loading">
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="注册用户总数" :value="stats.userTotal" />
          <div class="stat-sub">
            监督员 {{ stats.supervisor }} · 网格员 {{ stats.inspector }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="监督反馈总数" :value="stats.feedbackTotal" />
          <div class="stat-sub warn">待指派 {{ stats.pending }} 条</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="AQI 检测总数" :value="stats.aqiTotal" />
          <div class="stat-sub danger">超标 {{ stats.aqiExceed }} 次（{{ stats.aqiExceedRate }}）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="已完成工单" :value="stats.completed" />
          <div class="stat-sub success">升级中 {{ stats.escalated }} 条</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工单状态分布 -->
    <el-card shadow="never" style="margin-bottom:20px">
      <template #header><span>📊 工单状态分布</span></template>
      <div class="status-bars">
        <div class="status-bar-item" v-for="s in statusList" :key="s.key">
          <div class="bar-label">{{ s.label }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: barWidth(s.value), background: s.color }"></div>
          </div>
          <div class="bar-value">{{ s.value }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-bottom:20px">
      <template #header><span>🗺️ 全国污染热点地图 — 点击省份可下钻</span></template>
      <MapView style="height:480px" />
    </el-card>

    <el-card shadow="never">
      <template #header><span>快捷操作</span></template>
      <el-button type="primary" @click="$router.push('/admin/users')">用户管理</el-button>
      <el-button type="warning" @click="$router.push('/admin/feedbacks')">反馈指派</el-button>
      <el-button type="success" @click="$router.push('/admin/statistics')">数据统计</el-button>
      <el-button type="danger" @click="$router.push('/nepv/dashboard')">🗺️ 数据大屏</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserStats } from '@/api/user'
import { getFeedbackOverview } from '@/api/feedback'
import { getAqiOverview } from '@/api/aqi'
import MapView from '@/components/MapView.vue'

const loading = ref(false)
const stats = ref({
  userTotal: 0, supervisor: 0, inspector: 0,
  feedbackTotal: 0, pending: 0, assigned: 0, completed: 0, escalated: 0,
  aqiTotal: 0, aqiExceed: 0, aqiExceedRate: '0.0%'
})

// 工单状态分布条形图数据
const statusList = computed(() => [
  { key: 'pending', label: '待指派', value: stats.value.pending, color: '#E6A23C' },
  { key: 'assigned', label: '检测中', value: stats.value.assigned, color: '#409EFF' },
  { key: 'completed', label: '已完成', value: stats.value.completed, color: '#67C23A' },
  { key: 'escalated', label: '已升级', value: stats.value.escalated, color: '#F56C6C' }
])

const maxStatus = computed(() => Math.max(1, ...statusList.value.map(s => s.value)))
const barWidth = (v) => `${Math.round((v / maxStatus.value) * 100)}%`

onMounted(async () => {
  loading.value = true
  // 各统计接口独立容错，单个失败不影响其它卡片
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
.admin-dashboard {
  padding: 4px;
}
.stat-card {
  border-radius: 12px;
}
.stat-sub {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.stat-sub.warn { color: #E6A23C; }
.stat-sub.danger { color: #F56C6C; }
.stat-sub.success { color: #67C23A; }

.status-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}
.status-bar-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.bar-label {
  width: 64px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.bar-track {
  flex: 1;
  height: 20px;
  background: #F5F7FA;
  border-radius: 10px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.bar-value {
  width: 48px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}
</style>
