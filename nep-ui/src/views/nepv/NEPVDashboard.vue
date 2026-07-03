<template>
  <div class="dash">
    <!-- ====== 页头 ====== -->
    <div class="hero">
      <h1>全国环境质量监测大屏</h1>
      <p>实时数据驱动 · 科学环保决策</p>
      <el-button type="primary" :loading="exporting" @click="handleExport" :icon="Download" size="large">
        {{ exporting ? '导出中...' : '导出空间分析报告' }}
      </el-button>
    </div>

    <!-- ====== 总览 KPI 卡片 ====== -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-val">{{ overview.totalUsers || 0 }}</div>
        <div class="kpi-lbl">注册监督员</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-val">{{ overview.totalFeedbacks || 0 }}</div>
        <div class="kpi-lbl">监督反馈总数</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-val">{{ overview.totalDetections || 0 }}</div>
        <div class="kpi-lbl">AQI检测总数</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-val">{{ overview.totalCities || 0 }}</div>
        <div class="kpi-lbl">覆盖城市</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-val green">{{ overview.completedFeedbacks || 0 }}</div>
        <div class="kpi-lbl">已完成反馈</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-val warn">{{ overview.pendingFeedbacks || 0 }}</div>
        <div class="kpi-lbl">待处理反馈</div>
      </div>
    </div>

    <!-- ====== 实时数据条 ====== -->
    <div class="live-bar">
      <span class="live-dot"></span> 实时监控数据
      <span class="live-time">刷新：{{ realtime.timestamp }}</span>
    </div>
    <div class="kpi-row">
      <div class="kpi-card live">
        <div class="kpi-val">{{ realtime.totalDetections || 0 }}</div>
        <div class="kpi-lbl">实时检测总数</div>
      </div>
      <div class="kpi-card live">
        <div class="kpi-val green">{{ realtime.goodDetections || 0 }}</div>
        <div class="kpi-lbl">检测良好</div>
      </div>
      <div class="kpi-card live">
        <div class="kpi-val red">{{ realtime.badDetections || 0 }}</div>
        <div class="kpi-lbl">检测超标</div>
      </div>
      <div class="kpi-card live">
        <div class="kpi-val">{{ realtime.onlineInspectors || 0 }}</div>
        <div class="kpi-lbl">在线网格员</div>
      </div>
      <div class="kpi-card live">
        <div class="kpi-val warn">{{ realtime.pendingFeedback || 0 }}</div>
        <div class="kpi-lbl">待处理反馈</div>
      </div>
      <div class="kpi-card live">
        <div class="kpi-val">{{ realtime.gridCoverageByProvince || 0 }}%</div>
        <div class="kpi-lbl">省份覆盖率</div>
      </div>
    </div>

    <!-- ====== 地图区域 ====== -->
    <div class="section-card">
      <h3 class="sec-title">污染热点地图 — 全国AQI实时分布</h3>
      <MapView style="height:620px" />
    </div>

    <!-- ====== 图表区域 ====== -->
    <div class="charts-row">
      <div class="section-card chart-box">
        <h3 class="sec-title">反馈状态分布</h3>
        <div ref="pieChart" class="chart-canvas"></div>
      </div>
      <div class="section-card chart-box">
        <h3 class="sec-title">月度反馈趋势</h3>
        <div ref="lineChart" class="chart-canvas"></div>
      </div>
      <div class="section-card chart-box wide">
        <h3 class="sec-title">各省份反馈统计</h3>
        <div ref="barChart" class="chart-canvas"></div>
      </div>
      <div class="section-card chart-box wide">
        <h3 class="sec-title">各省份污染物超标累计（SO₂ / CO / PM2.5）</h3>
        <div ref="pollutantChart" class="chart-canvas" style="height:340px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getOverview, getFeedbackStatus, getProvinceFeedback, getMonthlyTrend, getProvincePollutantExceed, exportSpatialReport } from '@/api/statistics'
import MapView from '@/components/MapView.vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const exporting = ref(false)
const overview = ref({})
const pieChart = ref(null), lineChart = ref(null), barChart = ref(null), pollutantChart = ref(null)
let charts = [], stompClient = null

const realtime = ref({
  totalDetections: 0, goodDetections: 0, badDetections: 0,
  gridCoverageByProvince: 0, gridCoverageByCity: 0,
  onlineInspectors: 0, pendingFeedback: 0,
  exceededByPollutant: {}, timestamp: ''
})

function connectWebSocket() {
  try {
    const socket = new SockJS('/ws')
    stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient.subscribe('/topic/dashboard', (msg) => {
          try { realtime.value = JSON.parse(msg.body) } catch(e) {}
        })
      }
    })
    stompClient.activate()
  } catch (e) { /* 降级 */ }
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await exportSpatialReport()
    const blob = new Blob([res], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'NEP空间分析报告_' + new Date().toISOString().substring(0,10) + '.pdf'
    a.click(); URL.revokeObjectURL(url)
    ElMessage.success('空间分析报告导出成功')
  } catch (e) { ElMessage.error('导出失败') }
  finally { exporting.value = false }
}

onMounted(async () => {
  try {
    const [ov, fs, pf, mt, pe] = await Promise.all([
      getOverview(), getFeedbackStatus(), getProvinceFeedback(), getMonthlyTrend(), getProvincePollutantExceed()
    ])
    overview.value = ov.data || {}
    initPie(fs.data); initBar(pf.data); initLine(mt.data); initPollutant(pe.data)
  } catch(e) {}
  connectWebSocket()
})

const chartColors = { text: '#74807B', blue: '#409EFF', green: '#2AA876', orange: '#F5A623', red: '#F56C6C', gray: '#909399' }

function initPie(data) {
  if (!pieChart.value) return
  const c = markRaw(echarts.init(pieChart.value))
  c.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['45%', '72%'],
      data: [
        { value: data?.pending || 0, name: '待指派', itemStyle: { color: chartColors.orange } },
        { value: data?.assigned || 0, name: '处理中', itemStyle: { color: chartColors.blue } },
        { value: data?.completed || 0, name: '已完成', itemStyle: { color: chartColors.green } }
      ],
      label: { color: chartColors.text }
    }]
  })
  charts.push(c)
}

function initBar(data) {
  if (!barChart.value) return
  const c = markRaw(echarts.init(barChart.value))
  const list = (data || []).slice(0, 10)
  c.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: list.map(d => d.provinceName), axisLabel: { color: chartColors.text } },
    yAxis: { type: 'value', axisLabel: { color: chartColors.text } },
    series: [{ type: 'bar', data: list.map(d => d.count), itemStyle: { color: chartColors.blue, borderRadius: [6,6,0,0] } }]
  })
  charts.push(c)
}

function initLine(data) {
  if (!lineChart.value) return
  const c = markRaw(echarts.init(lineChart.value))
  c.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (data||[]).map(d => d.month), axisLabel: { color: chartColors.text } },
    yAxis: { type: 'value', axisLabel: { color: chartColors.text } },
    series: [{ type: 'line', data: (data||[]).map(d => d.count), smooth:true, itemStyle:{color: chartColors.green}, areaStyle:{color:'rgba(42,168,118,0.1)'} }]
  })
  charts.push(c)
}

function initPollutant(data) {
  if (!pollutantChart.value) return
  const c = markRaw(echarts.init(pollutantChart.value))
  const list = (data || []).filter(d => d.provinceName).slice(0, 10)
  c.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['SO₂超标', 'CO超标', 'PM2.5超标'], textStyle: { color: chartColors.text } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: list.map(d => d.provinceName), axisLabel: { color: chartColors.text, rotate: 30 } },
    yAxis: { type: 'value', name: '超标次数', axisLabel: { color: chartColors.text } },
    series: [
      { name: 'SO₂超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.so2Exceed) || 0), itemStyle: { color: chartColors.orange } },
      { name: 'CO超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.coExceed) || 0), itemStyle: { color: chartColors.red } },
      { name: 'PM2.5超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.pm25Exceed) || 0), itemStyle: { color: chartColors.gray } }
    ]
  })
  charts.push(c)
}

onUnmounted(() => {
  charts.forEach(c => c.dispose())
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.dash { max-width: 1440px; margin: 0 auto; padding: 28px 32px 48px; }

/* ====== 页头 ====== */
.hero { text-align: center; margin-bottom: 36px; }
.hero h1 { font-size: 26px; font-weight: 700; margin: 0 0 8px; color: #1C2421; }
.hero p { font-size: 15px; color: #74807B; margin: 0 0 16px; }

/* ====== KPI 行 ====== */
.kpi-row {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; margin-bottom: 24px;
}

/* ====== KPI 卡片 ====== */
.kpi-card {
  background: rgba(255,255,255,0.5);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.8);
  border-radius: 18px;
  padding: 24px 16px; text-align: center;
  box-shadow: 0 4px 16px -4px rgba(28,36,33,0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px -6px rgba(28,36,33,0.08); }

.kpi-val { font-size: 30px; font-weight: 700; color: #1C2421; }
.kpi-val.green { color: #2AA876; }
.kpi-val.warn { color: #F5A623; }
.kpi-val.red { color: #E11D48; }
.kpi-lbl { font-size: 12px; color: #74807B; font-weight: 600; margin-top: 6px; }

/* 实时卡片微调 */
.kpi-card.live { border-left: 3px solid #409EFF; }

/* 实时条 */
.live-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 0 4px 16px; font-size: 14px; font-weight: 600; color: #1C2421;
}
.live-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #E11D48;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(225,29,72,0.4); }
  50% { box-shadow: 0 0 0 6px rgba(225,29,72,0); }
}
.live-time { font-size: 12px; color: #A0AAB2; font-weight: 400; margin-left: 8px; }

/* ====== 区块卡片 ====== */
.section-card {
  background: rgba(255,255,255,0.5);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.8);
  border-radius: 22px;
  padding: 24px 28px;
  box-shadow: 0 4px 16px -4px rgba(28,36,33,0.04);
  margin-bottom: 20px;
}
.sec-title { margin: 0 0 18px; font-size: 15px; font-weight: 600; color: #1C2421; }

/* ====== 图表网格 ====== */
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-box.wide { grid-column: 1 / -1; }
.chart-canvas { height: 300px; }

/* 响应式 */
@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
  .chart-box.wide { grid-column: 1; }
}
</style>
