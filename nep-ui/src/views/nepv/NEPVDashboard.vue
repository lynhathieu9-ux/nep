<template>
  <div class="nepv-dashboard" v-loading="loading">
    <div class="hero-banner">
      <h1>🌍 全国环境质量监测大屏</h1>
      <p>实时数据驱动 · 科学环保决策</p>
      <div style="margin-top:16px">
        <el-button type="primary" :loading="exporting" @click="handleExport" :icon="Download">
          {{ exporting ? '导出中...' : '导出空间分析报告' }}
        </el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi"><div class="val">{{ overview.totalUsers || 0 }}</div><div class="lbl">注册监督员</div></div>
      <div class="kpi"><div class="val">{{ overview.totalFeedbacks || 0 }}</div><div class="lbl">监督反馈总数</div></div>
      <div class="kpi"><div class="val">{{ overview.totalDetections || 0 }}</div><div class="lbl">AQI检测总数</div></div>
      <div class="kpi"><div class="val">{{ overview.totalCities || 0 }}</div><div class="lbl">覆盖城市</div></div>
      <div class="kpi"><div class="val">{{ overview.completedFeedbacks || 0 }}</div><div class="lbl">已完成反馈</div></div>
      <div class="kpi warn"><div class="val">{{ overview.pendingFeedbacks || 0 }}</div><div class="lbl">待处理反馈</div></div>
    </div>

    <!-- 🔴 实时推送数据（WebSocket 每10秒刷新） -->
    <div class="realtime-banner">
      <span class="live-dot"></span> 实时数据
      <span style="margin-left:12px;font-size:13px;color:#8899aa">数据刷新：{{ realtime.timestamp }}</span>
    </div>
    <div class="kpi-grid">
      <div class="kpi live"><div class="val">{{ realtime.totalDetections || 0 }}</div><div class="lbl">🔴 实时检测总数</div></div>
      <div class="kpi live good"><div class="val">{{ realtime.goodDetections || 0 }}</div><div class="lbl">🟢 检测良好</div></div>
      <div class="kpi live bad"><div class="val">{{ realtime.badDetections || 0 }}</div><div class="lbl">🟠 检测超标</div></div>
      <div class="kpi live"><div class="val">{{ realtime.onlineInspectors || 0 }}</div><div class="lbl">👤 在线网格员</div></div>
      <div class="kpi live warn"><div class="val">{{ realtime.pendingFeedback || 0 }}</div><div class="lbl">📋 待处理反馈</div></div>
      <div class="kpi live"><div class="val">{{ realtime.gridCoverageByProvince || 0 }}%</div><div class="lbl">📡 省份覆盖率</div></div>
    </div>

    <!-- 污染热点地图 -->
    <div class="map-section">
      <h3>🗺️ 污染热点地图 — 全国AQI实时分布</h3>
      <MapView style="height:620px" />
    </div>

    <div class="charts-grid">
      <div class="chart-card">
        <h3>📊 反馈状态分布</h3>
        <div ref="pieChart" style="height:300px"></div>
      </div>
      <div class="chart-card">
        <h3>📈 月度反馈趋势</h3>
        <div ref="lineChart" style="height:300px"></div>
      </div>
      <div class="chart-card span-2">
        <h3>🏙️ 各省份反馈统计</h3>
        <div ref="barChart" style="height:300px"></div>
      </div>
      <div class="chart-card span-2">
        <h3>🧪 各省份污染物超标累计（SO₂ / CO / PM2.5）</h3>
        <div ref="pollutantChart" style="height:340px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, markRaw, watch } from 'vue'
import * as echarts from 'echarts'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getOverview, getFeedbackStatus, getAqiDistribution, getProvinceFeedback, getMonthlyTrend, getProvincePollutantExceed, exportSpatialReport } from '@/api/statistics'
import MapView from '@/components/MapView.vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const exporting = ref(false)
const overview = ref({})
const pieChart = ref(null), lineChart = ref(null), barChart = ref(null), pollutantChart = ref(null)
let charts = [], stompClient = null

// 实时数据
const realtime = ref({
  totalDetections: 0, goodDetections: 0, badDetections: 0,
  gridCoverageByProvince: 0, gridCoverageByCity: 0,
  onlineInspectors: 0, pendingFeedback: 0,
  exceededByPollutant: {}
})

// 连接 WebSocket 接收实时推送（后端未部署时静默降级）
function connectWebSocket() {
  try {
    const socket = new SockJS('/ws')
    stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('NEPV 大屏 WebSocket 已连接')
        stompClient.subscribe('/topic/dashboard', (msg) => {
          try {
            const data = JSON.parse(msg.body)
            realtime.value = data
          } catch(e) {}
        })
      },
      onStompError: () => { /* 静默降级 */ }
    })
    stompClient.activate()
  } catch (e) {
    // WebSocket 后端未部署，使用静态数据
    console.log('WebSocket 不可用，使用静态数据')
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await exportSpatialReport()
    const blob = new Blob([res], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'NEP空间分析报告_' + new Date().toISOString().substring(0,10) + '.pdf'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('空间分析报告导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [ov, fs, pf, mt, pe] = await Promise.all([
      getOverview(), getFeedbackStatus(), getProvinceFeedback(), getMonthlyTrend(), getProvincePollutantExceed()
    ])
    overview.value = ov.data || {}
    initPie(fs.data)
    initBar(pf.data)
    initLine(mt.data)
    initPollutant(pe.data)
  } catch(e) {} finally { loading.value = false }

  // 启动 WebSocket 实时数据
  connectWebSocket()
})

function initPie(data) {
  if (!pieChart.value) return
  const c = markRaw(echarts.init(pieChart.value))
  c.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: [
        { value: data?.pending || 0, name: '待指派', itemStyle: { color: '#F5A623' } },
        { value: data?.assigned || 0, name: '处理中', itemStyle: { color: '#409EFF' } },
        { value: data?.completed || 0, name: '已完成', itemStyle: { color: '#2AA876' } }
      ],
      label: { color: '#8899aa' }
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
    xAxis: { type: 'category', data: list.map(d => d.provinceName), axisLabel: { color: '#8899aa' } },
    yAxis: { type: 'value', axisLabel: { color: '#8899aa' } },
    series: [{ type: 'bar', data: list.map(d => d.count), itemStyle: { color: '#409EFF', borderRadius: [6,6,0,0] } }]
  })
  charts.push(c)
}

function initLine(data) {
  if (!lineChart.value) return
  const c = markRaw(echarts.init(lineChart.value))
  c.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (data||[]).map(d => d.month), axisLabel: { color: '#8899aa' } },
    yAxis: { type: 'value', axisLabel: { color: '#8899aa' } },
    series: [{ type: 'line', data: (data||[]).map(d => d.count), smooth:true, itemStyle:{color:'#2AA876'}, areaStyle:{color:'rgba(42,168,118,0.1)'} }]
  })
  charts.push(c)
}

/** 省分组三污染物超标累计（堆叠柱状图，取超标最严重前10省） */
function initPollutant(data) {
  if (!pollutantChart.value) return
  const c = markRaw(echarts.init(pollutantChart.value))
  const list = (data || []).filter(d => d.provinceName).slice(0, 10)
  c.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['SO₂超标', 'CO超标', 'PM2.5超标'], textStyle: { color: '#8899aa' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: list.map(d => d.provinceName), axisLabel: { color: '#8899aa', rotate: 30 } },
    yAxis: { type: 'value', name: '超标次数', axisLabel: { color: '#8899aa' } },
    series: [
      { name: 'SO₂超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.so2Exceed) || 0), itemStyle: { color: '#E6A23C' } },
      { name: 'CO超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.coExceed) || 0), itemStyle: { color: '#F56C6C' } },
      { name: 'PM2.5超标', type: 'bar', stack: 'total', data: list.map(d => Number(d.pm25Exceed) || 0), itemStyle: { color: '#909399' } }
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
.nepv-dashboard { max-width:1400px; margin:0 auto; padding:32px; }
.hero-banner { text-align:center; margin-bottom:40px; }
.hero-banner h1 { font-size:28px; color:#fff; margin:0 0 8px; }
.hero-banner p { font-size:15px; color:#8899aa; margin:0; }

.kpi-grid { display:grid; grid-template-columns:repeat(6,1fr); gap:16px; margin-bottom:32px; }
.kpi { background:rgba(255,255,255,0.04); border:1px solid rgba(255,255,255,0.06); border-radius:12px; padding:20px; text-align:center; }
.kpi.warn { border-color:rgba(245,166,35,0.3); }
.kpi .val { font-size:32px; font-weight:700; color:#409EFF; }
.kpi.warn .val { color:#F5A623; }
.kpi .lbl { font-size:13px; color:#8899aa; margin-top:6px; }

.map-section {
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}
.map-section h3 { margin: 0 0 16px; font-size: 16px; color: #ccc; }

.charts-grid { display:grid; grid-template-columns:1fr 1fr; gap:20px; }
.chart-card { background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.06); border-radius:12px; padding:24px; }
.chart-card.span-2 { grid-column:1/-1; }
.chart-card h3 { margin:0 0 16px; font-size:16px; color:#ccc; }
</style>
