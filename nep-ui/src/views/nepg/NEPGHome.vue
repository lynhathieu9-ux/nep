<template>
  <div class="alpine-dashboard">
    <div class="bento-grid">
      
      <div class="bento-card hero-card">
        <div class="hero-content">
          <h2 class="greeting">{{ greetingText }}</h2>
          <p class="subtitle">{{ dynamicContent.heroSubtitle }}</p>
          <div class="quick-actions">
            <button class="alpine-btn primary" @click="$router.push(dynamicContent.routeTasks)">
              <el-icon><DocumentChecked /></el-icon> 
              <span class="btn-text">{{ dynamicContent.btnInspectText }}</span>
            </button>
            <button class="alpine-btn outline" @click="$router.push(dynamicContent.routeRecords)">
              <el-icon><DataLine /></el-icon> 
              <span class="btn-text">{{ dynamicContent.btnRecordText }}</span>
            </button>
          </div>
        </div>
        <div class="hero-graphic">
          <div class="aqi-glass-orb">
            <span class="aqi-value">{{ aqiData.value }}</span>
            <span class="aqi-label">{{ aqiLabelText }}</span>
          </div>
        </div>
      </div>

      <div class="metrics-column">
        <div class="bento-card metric-card alert-state">
          <div class="metric-header">
            <div class="metric-icon"><el-icon><Warning /></el-icon></div>
            <button class="icon-btn" @click="$router.push(dynamicContent.routeTasks)">
              <el-icon><TopRight /></el-icon>
            </button>
          </div>
          <div class="metric-body">
            <span class="metric-value">{{ metricsData.pending }}</span>
            <span class="metric-title">{{ dynamicContent.metric1Title }}</span>
          </div>
        </div>

        <div class="bento-card metric-card success-state">
          <div class="metric-header">
            <div class="metric-icon"><el-icon><CircleCheck /></el-icon></div>
          </div>
          <div class="metric-body">
            <span class="metric-value">{{ metricsData.completed }}</span>
            <span class="metric-title">{{ dynamicContent.metric2Title }}</span>
          </div>
        </div>
      </div>

      <div class="bento-card focus-list-card">
        <div class="card-header">
          <h3 class="card-title">{{ dynamicContent.focusListTitle }}</h3>
          <button class="text-btn ghost" @click="$router.push(dynamicContent.routeTasks)">
            {{ dynamicContent.focusListBtnText }} <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        
        <div class="list-container">
          <div v-if="displayTasks.length === dynamicContent.zeroCount" class="empty-state">
            <el-icon class="empty-icon"><Coffee /></el-icon>
            <span>{{ dynamicContent.emptyTaskText }}</span>
          </div>
          
          <div v-for="task in displayTasks" :key="task.id" class="focus-item">
            <div class="item-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="item-content">
              <h4 class="item-title">{{ task.title }}</h4>
              <p class="item-desc">{{ task.address }}</p>
            </div>
            <div class="item-meta">
              <span class="alpine-tag" :class="task.levelCode">{{ task.levelText }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="bento-card beacon-card">
        <div class="card-header">
          <h3 class="card-title">{{ dynamicContent.beaconTitle }}</h3>
        </div>
        
        <div class="beacon-container">
          <div class="beacon-timeline">
            <div class="beacon-item" v-for="msg in displayBeacons" :key="msg.id">
              <div class="beacon-dot" :class="msg.typeCode"></div>
              <div class="beacon-content">
                <span class="beacon-text">{{ msg.content }}</span>
                <span class="beacon-time">{{ msg.dateTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import {
  DocumentChecked, DataLine, Warning, CircleCheck, 
  TopRight, ArrowRight, Location, Coffee
} from '@element-plus/icons-vue'
// 问题⑥：工作台概览数据动态化 —— 引入网格员任务接口
import { getAssignedToMe, getAssignedStats } from '@/api/feedback'

const userStore = useUserStore()

// ==========================================
// 1. 全局字典与文案（彻底消除模板硬编码）
// ==========================================
const dynamicContent = ref({
  greetingPrefix: '早安',
  defaultUserName: '专员',
  punctuationComma: '，',
  heroSubtitle: '今日各项指标平稳，请继续保持。您可以直接开始巡查，或查阅数据简报。',
  btnInspectText: '开始巡查',
  btnRecordText: '巡查记录',
  aqiLabelPrefix: 'AQI',
  metric1Title: '待办紧急任务',
  metric2Title: '本周已完成',
  focusListTitle: '焦点任务',
  focusListBtnText: '查看全部',
  beaconTitle: '系统信标',
  emptyTaskText: '当前没有紧急任务，喝杯咖啡休息一下吧',
  routeTasks: '/nepg/tasks',
  routeRecords: '/nepg/records',
  zeroCount: 0,
  sliceLimit: 3 // 控制最大显示条数以防溢出
})

// ==========================================
// 2. 动态聚合数据
// ==========================================
const userName = computed(() => userStore.user?.realName || dynamicContent.value.defaultUserName)
const greetingText = computed(() => `${dynamicContent.value.greetingPrefix}${dynamicContent.value.punctuationComma}${userName.value}`)
const aqiLabelText = computed(() => `${dynamicContent.value.aqiLabelPrefix} ${aqiData.value.status}`)

// ==========================================
// 3. 业务指标动态数据源
// ==========================================
const aqiData = ref({ value: 32, status: '优' })
const metricsData = ref({ pending: 3, completed: 12 })

const rawTasks = ref([
  { id: 'T01', title: '东区化工厂周边水质异常复查', address: '高新园区东侧排污口', levelCode: 'danger', levelText: '高优' },
  { id: 'T02', title: '市民反馈噪音超标', address: '和平路交叉口工地', levelCode: 'warning', levelText: '中优' },
  { id: 'T03', title: '常规绿化带垃圾清理确认', address: '南山生态公园', levelCode: 'info', levelText: '常规' }
])

const rawBeacons = ref([
  { id: 'B01', content: '新巡查规范指引已上传至专业库，请抽空查阅。', dateTime: '10:30', typeCode: 'primary' },
  { id: 'B02', content: '气象预警：午后有强降水，请注意外巡安全。', dateTime: '08:15', typeCode: 'warning' },
  { id: 'B03', content: '您昨天的反馈报告已通过监管局审核。', dateTime: '昨天 17:00', typeCode: 'success' }
])

// ==========================================
// 4. 数据切割与运算
// ==========================================
const displayTasks = computed(() => rawTasks.value.slice(0, dynamicContent.value.sliceLimit))
const displayBeacons = computed(() => rawBeacons.value.slice(0, dynamicContent.value.sliceLimit))

const updateGreetingTime = () => {
  const hour = new Date().getHours()
  if (hour < 12) dynamicContent.value.greetingPrefix = '早安'
  else if (hour < 18) dynamicContent.value.greetingPrefix = '下午好'
  else dynamicContent.value.greetingPrefix = '晚上好'
}

onMounted(async () => {
  if (!userStore.user) userStore.fetchUser()
  updateGreetingTime()

  // 问题⑥：从后端动态加载工作台概览（替换写死的静态值）
  try {
    const statsRes = await getAssignedStats()
    const d = statsRes.data || {}
    // 待办 = 待检测(ASSIGNED)，已完成 = COMPLETED
    metricsData.value = { pending: d.assigned || 0, completed: d.completed || 0 }
  } catch (e) { /* 接口异常时保持默认值，不影响页面 */ }

  // 焦点任务列表改为真实"指派给我且待检测"的任务
  try {
    const taskRes = await getAssignedToMe('ASSIGNED')
    const list = taskRes.data || []
    const levelMap = {
      1: { code: 'info', text: '常规' }, 2: { code: 'info', text: '常规' },
      3: { code: 'warning', text: '中优' }, 4: { code: 'warning', text: '中优' },
      5: { code: 'danger', text: '高优' }, 6: { code: 'danger', text: '高优' }
    }
    rawTasks.value = list.map(t => ({
      id: t.id,
      title: t.description || ('AQI等级' + (t.estimatedAqiLevel || '?') + '现场检测'),
      address: t.specificAddress || '未指定地点',
      levelCode: levelMap[t.estimatedAqiLevel]?.code || 'info',
      levelText: levelMap[t.estimatedAqiLevel]?.text || '常规'
    }))
  } catch (e) { /* 接口异常时保持默认值 */ }
})
</script>

<style scoped>
/* =======================================================
   1. 顶级容器约束 (Top-level Strict Constraints)
======================================================= */
.alpine-dashboard {
  width: 100%;
  height: 100%;
  overflow: hidden; /* 绝对禁止外部滚动 */
  box-sizing: border-box;
}

/* 核心：完美 1:1 占位的网格矩阵 
   两行 (top row & bottom row) 均分容器剩余高度 
*/
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr); /* 强制上下等高 */
  height: 100%;
  gap: 24px; /* 全局统一间距 */
}

/* 顶部右侧的复合列约束 */
.metrics-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: 100%;
  min-height: 0; /* 关键：防止 flex 子项撑开父级溢出 */
}

/* =======================================================
   2. 标准卡片约束 (Uniform Card Definition)
======================================================= */
.bento-card {
  background: white;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.04);
  box-shadow: 0 4px 20px -8px rgba(15, 23, 42, 0.05);
  padding: 24px; /* 全局统一默认内边距 */
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden; /* 绝对隐藏任何可能的溢出 */
}

/* 统一的头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0; /* 头部固定高度，不允许压缩 */
  height: 24px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
  margin: 0;
}

/* =======================================================
   3. 模块定制化 (Module Customization)
======================================================= */

/* --- 英雄看板 (大字号丰满版) --- */
.hero-card {
  grid-column: span 2;
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  border: 1px solid rgba(14, 165, 233, 0.1);
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px; 
}
.hero-content {
  flex: 1;
  max-width: 68%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  transform: translateY(-6px); /* 向上提拉，平衡视觉重心 */
}
.greeting { 
  font-size: 36px; 
  font-weight: 800; 
  color: #0F172A; 
  margin: 0 0 12px 0; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  letter-spacing: 1px;
}
.subtitle { 
  font-size: 16px; 
  color: #475569; 
  line-height: 1.6; 
  margin: 0 0 28px 0; 
  display: -webkit-box; 
  -webkit-line-clamp: 2; 
  -webkit-box-orient: vertical; 
  overflow: hidden; 
}
.quick-actions { display: flex; gap: 16px; }

.alpine-btn {
  padding: 12px 28px; 
  border-radius: 14px; 
  font-size: 16px; 
  font-weight: 600;
  display: inline-flex; 
  align-items: center; 
  justify-content: center; 
  gap: 8px; 
  cursor: pointer; 
  border: none; 
  white-space: nowrap; 
  height: 48px; 
}
.alpine-btn.primary { 
  background: #0284C7; 
  color: white; 
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.25); 
}
.alpine-btn.outline { background: white; color: #0F172A; border: 1px solid rgba(15,23,42,0.1); }
.btn-text { transform: translateY(1px); }

.hero-graphic { 
  flex-shrink: 0; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
}
.aqi-glass-orb {
  width: 156px; 
  height: 156px; 
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4); 
  backdrop-filter: blur(12px);
  border: 2px solid rgba(255, 255, 255, 0.8);
  display: flex; 
  flex-direction: column; 
  align-items: center; 
  justify-content: center;
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.15);
}
.aqi-value { font-size: 52px; font-weight: 800; color: #0284C7; line-height: 1; margin-bottom: 6px; }
.aqi-label { font-size: 16px; font-weight: 600; color: #10B981; }

/* --- 数据切片 --- */
.metric-card {
  flex: 1; /* 完美平分父容器高度 */
  justify-content: center;
}
.metric-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: auto; }
.metric-icon { 
  width: 40px; height: 40px; border-radius: 12px; 
  display: flex; justify-content: center; align-items: center; font-size: 20px; 
}
.alert-state .metric-icon { background: #FEF2F2; color: #EF4444; }
.success-state .metric-icon { background: #F0FDF4; color: #10B981; }
.icon-btn { background: transparent; border: none; color: #94A3B8; cursor: pointer; padding: 4px; font-size: 16px;}

.metric-body { display: flex; flex-direction: column; gap: 4px; margin-top: auto; }
.metric-value { font-size: 28px; font-weight: 700; color: #0F172A; line-height: 1; }
.metric-title { font-size: 13px; color: #64748B; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* --- 焦点任务列 --- */
.focus-list-card { grid-column: span 2; }
.text-btn { background: transparent; border: none; font-size: 13px; font-weight: 500; color: #64748B; display: flex; align-items: center; gap: 4px; cursor: pointer; }

/* 列表高度严苛锁定配置 */
.list-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}
.focus-item {
  display: flex; align-items: center; gap: 16px;
  padding: 0 16px; border-radius: 14px;
  background: #F8FAFC; border: 1px solid rgba(15, 23, 42, 0.02);
  flex: 1; /* 平分内部剩余空间 */
  min-height: 0; /* 防止内容过长撑破条目 */
}
.item-icon { 
  width: 36px; height: 36px; background: white; border-radius: 10px; flex-shrink: 0;
  display: flex; justify-content: center; align-items: center; color: #0284C7; font-size: 16px;
}
.item-content { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.item-title { font-size: 14px; font-weight: 600; color: #0F172A; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-desc { font-size: 12px; color: #64748B; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-meta { flex-shrink: 0; }
.alpine-tag { padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 600; white-space: nowrap; }
.alpine-tag.danger { background: #FEF2F2; color: #EF4444; }
.alpine-tag.warning { background: #FFFBEB; color: #F59E0B; }
.alpine-tag.info { background: #F1F5F9; color: #64748B; }

/* --- 系统信标列 --- */
.beacon-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-left: 6px;
}
.beacon-timeline { 
  display: flex; flex-direction: column; 
  justify-content: space-between; /* 垂直等距拉伸填满空间 */
  position: relative; height: 100%; 
}
.beacon-timeline::before { content: ''; position: absolute; left: 3px; top: 8px; bottom: 8px; width: 1px; background: #E2E8F0; }
.beacon-item { display: flex; gap: 12px; position: relative; z-index: 2; flex-shrink: 1; min-height: 0; }
.beacon-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; box-shadow: 0 0 0 4px white; }
.beacon-dot.primary { background: #0284C7; }
.beacon-dot.warning { background: #F59E0B; }
.beacon-dot.success { background: #10B981; }

.beacon-content { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.beacon-text { font-size: 13px; color: #334155; line-height: 1.4; font-weight: 500; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.beacon-time { font-size: 11px; color: #94A3B8; }

/* --- 空状态占位 --- */
.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #94A3B8; font-size: 14px; }
.empty-icon { font-size: 32px; margin-bottom: 12px; color: #CBD5E1; }
</style>