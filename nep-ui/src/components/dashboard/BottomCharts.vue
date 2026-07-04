<template>
  <div class="bc">
    <!-- 第一行：仪表盘 + 3D柱群 + 反馈饼图 -->
    <div class="bc-row">
      <div class="bc-card">
        <h5>污染热点分布</h5>
        <div class="bc-pie-w">
          <div ref="ringChartRef" class="bc-ch"></div>
          <div class="bc-cv">2307</div>
        </div>
      </div>

      <div class="bc-card">
        <h5>城市AQI柱群</h5>
        <div class="bc-bar-w">
          <span class="bc-fv">2307</span>
          <div ref="bar3dChartRef" class="bc-ch"></div>
        </div>
      </div>

      <div class="bc-card">
        <h5>反馈状态分布</h5>
        <div class="bc-pie-w">
          <div ref="pieChartRef" class="bc-ch"></div>
          <div class="bc-cv sm">2307</div>
        </div>
      </div>
    </div>

    <!-- 第二行：月度趋势 + 省份统计 + 污染物超标 -->
    <div class="bc-row">
      <div class="bc-card">
        <h5>月度反馈统计</h5>
        <div ref="lineChartRef" class="bc-ch"></div>
      </div>

      <div class="bc-card">
        <h5>各省份反馈统计</h5>
        <div ref="provinceChartRef" class="bc-ch"></div>
      </div>

      <div class="bc-card">
        <h5>各省份污染物超标累计</h5>
        <div ref="pollutantChartRef" class="bc-ch"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import { getFeedbackStatus, getProvinceFeedback, getMonthlyTrend, getProvincePollutantExceed } from '@/api/statistics'

const ringChartRef = ref(null)
const bar3dChartRef = ref(null)
const pieChartRef = ref(null)
const lineChartRef = ref(null)
const provinceChartRef = ref(null)
const pollutantChartRef = ref(null)

let charts = []

const C = {
  blue: '#4fa3d1',
  green: '#2aa876',
  orange: '#f5a623',
  red: '#f56c6c',
  purple: '#a855f7',
  pink: '#ec4899',
  text: '#c8d6e5',
  darkText: '#8fa3b8',
  bg: 'transparent',
  border: 'rgba(64, 144, 188, 0.2)'
}

function initRingChart() {
  if (!ringChartRef.value) return
  const c = markRaw(echarts.init(ringChartRef.value))
  c.setOption({
    backgroundColor: C.bg,
    series: [{
      type: 'gauge',
      startAngle: 90,
      endAngle: -270,
      pointer: { show: false },
      progress: {
        show: true,
        overlap: false,
        roundCap: true,
        clip: false,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#4fa3d1' },
            { offset: 0.5, color: '#a855f7' },
            { offset: 1, color: '#ec4899' }
          ])
        }
      },
      axisLine: {
        lineStyle: { width: 12, color: [[1, 'rgba(0,0,0,0.3)']] }
      },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      data: [{ value: 75, detail: { show: false } }]
    }]
  })
  charts.push(c)
}

function initBar3DChart() {
  if (!bar3dChartRef.value) return
  const c = markRaw(echarts.init(bar3dChartRef.value))
  const data = []
  const categories = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏']
  categories.forEach((cat, i) => {
    data.push([cat, '检测量', Math.random() * 300 + 50])
    data.push([cat, '超标量', Math.random() * 100 + 20])
    data.push([cat, '排放量', Math.random() * 150 + 30])
  })
  c.setOption({
    backgroundColor: C.bg,
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,22,40,0.9)', borderColor: C.border, textStyle: { color: C.text } },
    legend: { data: ['检测量', '超标量', '排放量'], textStyle: { color: C.darkText, fontSize: 10 }, top: 0 },
    grid3D: {
      viewControl: { autoRotate: false, distance: 200, alpha: 25, beta: 30 },
      light: { main: { intensity: 1.2, shadow: true }, ambient: { intensity: 0.3 } },
      boxWidth: 100,
      boxHeight: 60,
      boxDepth: 80,
      environment: 'none'
    },
    xAxis3D: {
      type: 'category',
      data: categories,
      axisLabel: { color: C.darkText, fontSize: 8 }
    },
    yAxis3D: {
      type: 'category',
      data: ['检测量', '超标量', '排放量'],
      axisLabel: { color: C.darkText, fontSize: 8 }
    },
    zAxis3D: { type: 'value', axisLabel: { color: C.darkText, fontSize: 8 } },
    series: [{
      type: 'bar3D',
      data: data.map(d => ({ value: d, itemStyle: {
        color: d[1] === '检测量' ? '#4fa3d1' : d[1] === '超标量' ? '#f56c6c' : '#f5a623',
        opacity: 0.8
      }}))
    }]
  })
  charts.push(c)
}

function initPieChart(ref) {
  if (!ref.value) return
  const c = markRaw(echarts.init(ref.value))
  c.setOption({
    backgroundColor: C.bg,
    tooltip: { trigger: 'item', backgroundColor: 'rgba(10,22,40,0.9)', borderColor: C.border, textStyle: { color: C.text } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      data: [
        { value: 35, name: '待处理', itemStyle: { color: '#f5a623' } },
        { value: 45, name: '处理中', itemStyle: { color: '#4fa3d1' } },
        { value: 20, name: '已完成', itemStyle: { color: '#2aa876' } }
      ],
      label: { color: C.text, fontSize: 10 },
      labelLine: { lineStyle: { color: C.border } }
    }]
  })
  charts.push(c)
}

function initLineChart(ref, color = '#4fa3d1') {
  if (!ref.value) return
  const c = markRaw(echarts.init(ref.value))
  c.setOption({
    backgroundColor: C.bg,
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,22,40,0.9)', borderColor: C.border, textStyle: { color: C.text } },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLabel: { color: C.darkText, fontSize: 9 },
      axisLine: { lineStyle: { color: C.border } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: C.darkText, fontSize: 9 },
      splitLine: { lineStyle: { color: C.border } }
    },
    series: [{
      type: 'bar',
      data: [120, 180, 150, 220, 190, 250],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color },
          { offset: 1, color: color + '88' }
        ]),
        borderRadius: [3, 3, 0, 0]
      }
    }]
  })
  charts.push(c)
}

function initProvinceChart() {
  if (!provinceChartRef.value) return
  const c = markRaw(echarts.init(provinceChartRef.value))
  const provinces = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏']
  c.setOption({
    backgroundColor: C.bg,
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(10,22,40,0.9)', borderColor: C.border, textStyle: { color: C.text } },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: provinces,
      axisLabel: { color: C.darkText, fontSize: 9, rotate: 30 },
      axisLine: { lineStyle: { color: C.border } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: C.darkText, fontSize: 9 },
      splitLine: { lineStyle: { color: C.border } }
    },
    series: [{
      type: 'bar',
      data: [85, 65, 120, 75, 45, 90, 55, 60, 110, 130],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#4fa3d1' },
          { offset: 1, color: '#2d6e96' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
  charts.push(c)
}

function initPollutantChart() {
  if (!pollutantChartRef.value) return
  const c = markRaw(echarts.init(pollutantChartRef.value))
  const provinces = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏']
  c.setOption({
    backgroundColor: C.bg,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(10,22,40,0.9)', borderColor: C.border, textStyle: { color: C.text } },
    legend: { data: ['SO₂超标', 'CO超标', 'PM2.5超标'], textStyle: { color: C.darkText, fontSize: 10 }, top: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: provinces,
      axisLabel: { color: C.darkText, fontSize: 9, rotate: 30 },
      axisLine: { lineStyle: { color: C.border } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: C.darkText, fontSize: 9 },
      splitLine: { lineStyle: { color: C.border } }
    },
    series: [
      { name: 'SO₂超标', type: 'bar', data: [25, 20, 35, 28, 15, 22, 18, 16, 12, 30], itemStyle: { color: '#f5a623' } },
      { name: 'CO超标', type: 'bar', data: [18, 15, 25, 20, 12, 18, 14, 12, 10, 22], itemStyle: { color: '#f56c6c' } },
      { name: 'PM2.5超标', type: 'bar', data: [32, 28, 42, 35, 20, 30, 22, 20, 18, 38], itemStyle: { color: '#2aa876' } }
    ]
  })
  charts.push(c)
}

onMounted(async () => {
  // 初始化所有图表（用默认数据占位）
  initRingChart()
  initBar3DChart()
  initPieChart(pieChartRef)
  initLineChart(lineChartRef, '#4fa3d1')
  initProvinceChart()
  initPollutantChart()

  // 从API加载真实数据并更新图表
  try {
    var [fs, pf, mt, pe] = await Promise.all([getFeedbackStatus(), getProvinceFeedback(), getMonthlyTrend(), getProvincePollutantExceed()])
    // 更新反馈饼图
    if (fs && fs.data && pieChartRef.value) {
      var pieC = echarts.getInstanceByDom(pieChartRef.value)
      if (pieC) pieC.setOption({ series: [{ data: [
        { value: fs.data.pending || 0, name: '待处理', itemStyle: { color: '#f5a623' } },
        { value: fs.data.assigned || 0, name: '处理中', itemStyle: { color: '#4fa3d1' } },
        { value: fs.data.completed || 0, name: '已完成', itemStyle: { color: '#2aa876' } }
      ]}]})
    }
    // 更新月度趋势图
    if (mt && mt.data && lineChartRef.value) {
      var lineC = echarts.getInstanceByDom(lineChartRef.value)
      if (lineC) lineC.setOption({ xAxis: { data: mt.data.map(function(v){return v.month}) }, series: [{ data: mt.data.map(function(v){return v.count}) }] })
    }
    // 更新省份统计图
    if (pf && pf.data && provinceChartRef.value) {
      var barC = echarts.getInstanceByDom(provinceChartRef.value)
      if (barC) { var pl = pf.data.slice(0, 10); barC.setOption({ xAxis: { data: pl.map(function(v){return v.provinceName}) }, series: [{ data: pl.map(function(v){return v.count}) }] }) }
    }
    // 更新污染物图
    if (pe && pe.data && pollutantChartRef.value) {
      var polC = echarts.getInstanceByDom(pollutantChartRef.value)
      if (polC) { var pld = pe.data.filter(function(v){return v.provinceName}).slice(0, 10); polC.setOption({ xAxis: { data: pld.map(function(v){return v.provinceName}) }, series: [
        { data: pld.map(function(v){return Number(v.so2Exceed)||0}) },
        { data: pld.map(function(v){return Number(v.coExceed)||0}) },
        { data: pld.map(function(v){return Number(v.pm25Exceed)||0}) }
      ]}) }
    }
  } catch(e) { console.error('BottomCharts数据加载失败:', e) }

  window.addEventListener('resize', () => { charts.forEach(c => c.resize()) })
})

onUnmounted(() => {
  window.removeEventListener('resize', () => {})
  charts.forEach(c => c.dispose())
  charts = []
})
</script>

<style scoped>
/* ====== 整体容器 ====== */
.bc { display:flex; flex-direction:column; gap:14px; padding:14px 16px 16px; }

/* ====== 每行3列等宽 ====== */
.bc-row { display:grid; grid-template-columns:repeat(3, 1fr); gap:14px; }

/* ====== 卡片 ====== */
.bc-card {
  background:rgba(15,30,54,0.55); backdrop-filter:blur(14px);
  border:1px solid rgba(255,255,255,0.05); border-radius:14px;
  padding:16px 18px; position:relative;
  box-shadow:0 4px 20px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.02);
  transition:transform 0.25s, box-shadow 0.25s, border-color 0.25s;
}
.bc-card:hover {
  transform:translateY(-2px);
  border-color:rgba(255,255,255,0.1);
  box-shadow:0 8px 28px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.03), 0 0 16px rgba(0,229,255,0.04);
}
.bc-card h5 { font-size:13px; font-weight:600; color:#E0E8F5; margin:0 0 14px; }

/* ====== 图表区 ====== */
.bc-ch { width:100%; height:220px; }
.bc-card:not(:has(.bc-pie-w)) .bc-ch { height:200px; }

/* 环图/饼图容器 */
.bc-pie-w { position:relative; }
.bc-cv { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); font-size:26px; font-weight:700; color:#fff; font-family:'SF Mono',monospace; text-shadow:0 0 14px rgba(255,255,255,0.3); pointer-events:none; }
.bc-cv.sm { font-size:20px; }

/* 柱群浮动值 */
.bc-bar-w { position:relative; }
.bc-fv { position:absolute; top:6px; left:10px; font-size:18px; font-weight:700; color:#FECA57; font-family:'SF Mono',monospace; text-shadow:0 0 8px rgba(254,202,87,0.4); z-index:5; }

@media(max-width:1400px){ .bc-row{grid-template-columns:repeat(2,1fr)} }
@media(max-width:900px){ .bc-row{grid-template-columns:1fr} }
</style>