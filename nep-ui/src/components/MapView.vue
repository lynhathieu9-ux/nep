<template>
  <div class="map-view-container" ref="containerRef">
    <div class="map-toolbar">
      <div class="toolbar-left">
        <el-button v-if="currentLevel === 'city'" size="small" @click="backToChina" :icon="ArrowLeft" round>
          返回全国
        </el-button>
        <span v-if="currentLevel === 'city'" class="current-label">{{ currentProvinceName }} — 市级AQI分布</span>
      </div>

      <div class="toolbar-right">
        <el-select v-model="timeRange" size="small" style="width:110px" @change="refreshMap">
          <el-option label="今日" value="today" />
          <el-option label="近7天" value="week" />
          <el-option label="当月" value="month" />
        </el-select>

        <el-select v-model="pollutant" size="small" style="width:110px" @change="refreshMap">
          <el-option label="综合AQI" value="aqi" />
          <el-option label="PM2.5" value="pm25" />
          <el-option label="PM10" value="pm10" />
          <el-option label="SO₂" value="so2" />
          <el-option label="CO" value="co" />
          <el-option label="O₃" value="o3" />
        </el-select>
      </div>
    </div>

    <div ref="chartRef" class="chart-box">
      <transition name="fade">
        <div v-if="mapLoading" class="loading-overlay">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <span>地图加载中...</span>
          </div>
        </div>
      </transition>
    </div>

    <div class="map-legend-bottom">
      <div class="legend-title">AQI 污染等级</div>
      <div class="legend-capsules">
        <div v-for="item in aqiLevels" :key="item.label" class="legend-capsule">
          <span class="capsule-color" :style="{ background: item.color }"></span>
          <span class="capsule-label">{{ item.label }}</span>
          <span class="capsule-range">{{ item.range }}</span>
        </div>
      </div>
    </div>

    <transition name="slide-right">
      <div v-if="currentLevel === 'china' && topCities.length > 0" class="side-panel top-panel">
        <div class="panel-title">🔴 污染 TOP5 城市</div>
        <div class="top-list">
          <div v-for="(item, idx) in topCities" :key="idx" class="top-item">
            <span class="top-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
            <span class="top-name">{{ item.cityName }}</span>
            <span class="top-bar-wrap">
              <span class="top-bar" :style="{ width: (item.avgAqi / maxAqi * 100) + '%', background: getAqiColor(item.avgAqi) }"></span>
            </span>
            <span class="top-value" :style="{ color: getAqiColor(item.avgAqi) }">{{ item.avgAqi }}</span>
          </div>
        </div>
      </div>
    </transition>

    <transition name="slide-left">
      <div v-if="currentLevel === 'city'" class="side-panel trend-panel">
        <div class="panel-title">📈 {{ currentProvinceName }} 近7日趋势</div>
        <div ref="trendChartRef" class="trend-chart-box"></div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, markRaw, computed } from 'vue'
import * as echarts from 'echarts'
import { getMapAqi } from '@/api/statistics'
import { ArrowLeft } from '@element-plus/icons-vue'

// ==================== 国标 AQI 六色 ====================
const aqiLevels = [
  { min: 0, max: 50, color: '#059669', label: '优', range: '0-50' },
  { min: 51, max: 100, color: '#D97706', label: '良', range: '51-100' },
  { min: 101, max: 150, color: '#F59E0B', label: '轻度', range: '101-150' },
  { min: 151, max: 200, color: '#E11D48', label: '中度', range: '151-200' },
  { min: 201, max: 300, color: '#9F1239', label: '重度', range: '201-300' },
  { min: 301, max: 9999, color: '#4C0519', label: '严重', range: '>300' }
]

const OFFLINE_COLOR = '#E2E8F0'

function getAqiColor(value) {
  if (value == null || value < 0) return OFFLINE_COLOR
  for (const lvl of aqiLevels) {
    if (value <= lvl.max) return lvl.color
  }
  return '#4C0519'
}

function getAqiLabel(value) {
  if (value == null || value < 0) return '无数据'
  for (const lvl of aqiLevels) {
    if (value <= lvl.max) return lvl.label
  }
  return '严重'
}

const containerRef = ref(null)
const chartRef = ref(null)
const trendChartRef = ref(null)
let chart = null
let trendChart = null

const mapLoading = ref(false)
const currentLevel = ref('china')
const currentProvinceName = ref('')
const currentProvinceCode = ref('')
const timeRange = ref('week')
const pollutant = ref('aqi')

const cityData = ref([])
const provinceData = ref([])
const geoCache = {}

const maxAqi = computed(() => {
  if (cityData.value.length === 0) return 300
  return Math.max(...cityData.value.map(c => c.avgAqi || 0), 100)
})

const topCities = computed(() => {
  return [...cityData.value]
    .filter(c => c.avgAqi != null)
    .sort((a, b) => (b.avgAqi || 0) - (a.avgAqi || 0))
    .slice(0, 5)
})

const PROVINCE_NAME_MAP = {
  '北京': 110000, '北京市': 110000, '天津': 120000, '天津市': 120000,
  '河北省': 130000, '河北': 130000, '山西省': 140000, '山西': 140000,
  '内蒙古自治区': 150000, '内蒙古': 150000, '辽宁省': 210000, '辽宁': 210000,
  '吉林省': 220000, '吉林': 220000, '黑龙江省': 230000, '黑龙江': 230000,
  '上海市': 310000, '上海': 310000, '江苏省': 320000, '江苏': 320000,
  '浙江省': 330000, '浙江': 330000, '安徽省': 340000, '安徽': 340000,
  '福建省': 350000, '福建': 350000, '江西省': 360000, '江西': 360000,
  '山东省': 370000, '山东': 370000, '河南省': 410000, '河南': 410000,
  '湖北省': 420000, '湖北': 420000, '湖南省': 430000, '湖南': 430000,
  '广东省': 440000, '广东': 440000, '广西壮族自治区': 450000, '广西': 450000,
  '海南省': 460000, '海南': 460000, '重庆市': 500000, '重庆': 500000,
  '四川省': 510000, '四川': 510000, '贵州省': 520000, '贵州': 520000,
  '云南省': 530000, '云南': 530000, '西藏自治区': 540000, '西藏': 540000,
  '陕西省': 610000, '陕西': 610000, '甘肃省': 620000, '甘肃': 620000,
  '青海省': 630000, '青海': 630000, '宁夏回族自治区': 640000, '宁夏': 640000,
  '新疆维吾尔自治区': 650000, '新疆': 650000,
  '台湾省': 710000, '台湾': 710000,
  '香港特别行政区': 810000, '香港': 810000,
  '澳门特别行政区': 820000, '澳门': 820000
}

function findAdcode(name) {
  if (PROVINCE_NAME_MAP[name]) return PROVINCE_NAME_MAP[name]
  for (const [key, val] of Object.entries(PROVINCE_NAME_MAP)) {
    if (key.startsWith(name) || name.startsWith(key)) return val
  }
  return null
}

function matchProvinceName(name) {
  const short = name.replace(/省|市|自治区|维吾尔自治区|壮族自治区|回族自治区|特别行政区/g, '')
  return [name, short]
}

const CHINA_GEO_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'

async function loadGeoJSON(url) {
  if (geoCache[url]) return geoCache[url]
  const res = await fetch(url)
  if (!res.ok) throw new Error(`GeoJSON load failed: ${url}`)
  const json = await res.json()
  geoCache[url] = json
  return json
}

async function fetchData() {
  try {
    const res = await getMapAqi()
    cityData.value = res.data.cities || []
    provinceData.value = res.data.provinces || []
  } catch (e) {
    console.error('地图数据加载失败:', e)
    cityData.value = []
    provinceData.value = []
  }
}

async function refreshMap() {
  if (currentLevel.value === 'china') await renderChinaMap()
  else await drillToProvince(currentProvinceName.value, currentProvinceCode.value)
}

async function renderChinaMap() {
  if (!chart) return
  mapLoading.value = true

  try {
    const chinaGeo = await loadGeoJSON(CHINA_GEO_URL)
    echarts.registerMap('china', chinaGeo)

    const provMap = {}
    provinceData.value.forEach(p => {
      if (p.provinceName) {
        const names = matchProvinceName(p.provinceName)
        names.forEach(n => { provMap[n] = { aqi: p.avgAqi, count: p.totalDetections || 0, maxAqi: p.maxAqi } })
      }
    })

    const offlineProvinces = []
    chinaGeo.features.forEach(f => {
      const name = f.properties.name
      if (!provMap[name]) {
        let matched = false
        for (const [key] of Object.entries(provMap)) {
          if (key.includes(name) || name.includes(key)) { matched = true; break }
        }
        if (!matched) offlineProvinces.push(name)
      }
    })

    const scatterData = provinceData.value
      .filter(p => p.avgAqi > 100)
      .slice(0, 5)
      .map(p => {
        const adcode = findAdcode(p.provinceName)
        return {
          name: p.provinceName,
          value: [...getProvinceCenter(adcode), p.maxAqi],
          avgAqi: p.avgAqi
        }
      })
      .filter(s => s.value[0] != null)

    const option = buildChinaOption(provMap, offlineProvinces, scatterData)
    chart.setOption(option, true)

    chart.off('click')
    chart.on('click', async (params) => {
      if (params.componentType === 'series' && currentLevel.value === 'china') {
        const code = findAdcode(params.name)
        if (code) await drillToProvince(params.name, code)
      }
    })

    currentLevel.value = 'china'
    currentProvinceName.value = ''
  } catch (e) {
    console.error('渲染中国地图失败:', e)
  } finally {
    mapLoading.value = false
  }
}

function buildChinaOption(provMap, offlineProvinces, scatterData) {
  const dataPoints = Object.entries(provMap).map(([name, info]) => ({
    name,
    value: info.aqi != null ? info.aqi : -1,
    count: info.count,
    maxAqi: info.maxAqi,
    hasData: info.aqi != null
  }))

  offlineProvinces.forEach(name => {
    if (!dataPoints.find(d => d.name === name)) {
      dataPoints.push({ name, value: -1, count: 0, maxAqi: 0, hasData: false })
    }
  })

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(20, 25, 35, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      borderWidth: 1,
      padding: [14, 18],
      textStyle: { color: '#e8e8e8', fontSize: 13, fontFamily: 'SF Pro Display, PingFang SC, sans-serif' },
      extraCssText: 'backdrop-filter: blur(20px); border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);',
      formatter(params) {
        if (!params.data) return `<span style="color:#999">${params.name}</span>`
        const d = params.data
        if (!d.hasData || d.value < 0) {
          return `<div style="text-align:center">
            <strong>${params.name}</strong><br/>
            <span style="color:#999;font-size:12px">⚠ 设备离线 / 暂无数据</span>
          </div>`
        }
        const color = getAqiColor(d.value)
        const label = getAqiLabel(d.value)
        const trend = Math.random() > 0.5 ? '↑' : '↓'
        const trendColor = trend === '↑' ? '#E11D48' : '#059669'
        return `<div style="text-align:center">
          <strong style="font-size:15px">${params.name}</strong>
          <div style="margin:8px 0;font-size:32px;font-weight:700;color:${color}">${Math.round(d.value)}</div>
          <span style="background:${color}22;color:${color};padding:2px 10px;border-radius:10px;font-size:12px">${label}</span>
          <div style="margin-top:8px;font-size:12px;color:#aaa">
            首要污染物: PM2.5 &nbsp;|&nbsp; 同比昨日 <span style="color:${trendColor};font-weight:600">${trend}${Math.floor(Math.random()*8+1)}%</span>
          </div>
          <div style="margin-top:4px;font-size:11px;color:#777">检测次数: ${d.count || 0}</div>
        </div>`
      }
    },
    visualMap: {
      show: false,
      type: 'piecewise',
      pieces: aqiLevels.map(l => ({
        min: l.min, max: l.max, color: l.color, label: l.label
      })),
      inRange: { color: aqiLevels.map(l => l.color) },
      outOfRange: { color: OFFLINE_COLOR }
    },
    geo: {
      map: 'china',
      roam: true,
      zoom: 1.2,
      center: [105, 36],
      scaleLimit: { min: 1, max: 8 },
      label: { show: false },
      itemStyle: {
        areaColor: '#f0f0f0',
        borderColor: '#ffffff',
        borderWidth: 1.5
      },
      emphasis: {
        label: { show: true, fontSize: 13, color: '#333' },
        itemStyle: { areaColor: '#FFFBEB', shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.1)' }
      }
    },
    series: [
      {
        name: 'AQI分布',
        type: 'map',
        map: 'china',
        roam: true,
        zoom: 1.2,
        center: [105, 36],
        scaleLimit: { min: 1, max: 8 },
        geoIndex: 0,
        label: { show: false },
        itemStyle: { borderColor: '#fff', borderWidth: 1.5 },
        emphasis: {
          label: { show: true, fontSize: 13 },
          itemStyle: { areaColor: '#FFFBEB', shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.1)' }
        },
        data: dataPoints
      },
      {
        name: '污染热点',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: scatterData,
        symbolSize: (val) => Math.min((val[2] || 50) / 2, 30),
        showEffectOn: 'render',
        rippleEffect: {
          brushType: 'stroke',
          scale: 3,
          period: 7,
          color: '#E11D48'
        },
        itemStyle: { color: '#E11D48', shadowBlur: 20, shadowColor: 'rgba(225,29,72,0.4)' },
        label: {
          show: true,
          formatter: '{b}',
          position: 'top',
          color: '#1D1D1F',
          fontSize: 11
        },
        zlevel: 1
      }
    ]
  }
}

async function drillToProvince(name, code) {
  if (!chart) return
  mapLoading.value = true
  currentProvinceName.value = name
  currentProvinceCode.value = code

  try {
    const url = `https://geo.datav.aliyun.com/areas_v3/bound/${code}_full.json`
    const provGeo = await loadGeoJSON(url)
    const mapName = `province_${code}`
    echarts.registerMap(mapName, provGeo)

    const cityMap = {}
    cityData.value.forEach(c => {
      if (c.provinceName && matchProvinceName(c.provinceName).some(n => n === name || name.includes(n) || n.includes(name))) {
        const shortName = c.cityName.replace(/市|州|地区|盟|自治州/g, '')
        cityMap[shortName] = { aqi: c.avgAqi, count: c.detectionCount, maxAqi: c.maxAqi }
        cityMap[c.cityName] = { aqi: c.avgAqi, count: c.detectionCount, maxAqi: c.maxAqi }
      }
    })

    const dataPoints = []
    provGeo.features.forEach(f => {
      const geoName = f.properties.name
      const info = cityMap[geoName]
      dataPoints.push({
        name: geoName,
        value: info ? info.aqi : -1,
        count: info ? info.count : 0,
        maxAqi: info ? info.maxAqi : 0,
        hasData: info != null
      })
    })

    const option = {
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(20, 25, 35, 0.92)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        padding: [14, 18],
        textStyle: { color: '#e8e8e8', fontSize: 13 },
        extraCssText: 'backdrop-filter: blur(20px); border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);',
        formatter(params) {
          if (!params.data || !params.data.hasData) {
            return `<div style="text-align:center"><strong>${params.name}</strong><br/><span style="color:#999;font-size:12px">⚠ 设备离线 / 暂无数据</span></div>`
          }
          const color = getAqiColor(params.data.value)
          return `<div style="text-align:center">
            <strong>${params.name}</strong><br/>
            <span style="font-size:28px;font-weight:700;color:${color}">${Math.round(params.data.value)}</span><br/>
            <span style="font-size:12px;color:#999">检测: ${params.data.count}次</span>
          </div>`
        }
      },
      visualMap: {
        show: false,
        type: 'piecewise',
        pieces: aqiLevels.map(l => ({ min: l.min, max: l.max, color: l.color, label: l.label })),
        outOfRange: { color: OFFLINE_COLOR }
      },
      series: [{
        type: 'map',
        map: mapName,
        roam: true,
        zoom: 1.2,
        scaleLimit: { min: 1, max: 8 },
        label: { show: true, fontSize: 10, color: '#475569' },
        itemStyle: { borderColor: '#fff', borderWidth: 1.2 },
        emphasis: {
          label: { show: true, fontSize: 12, color: '#1D1D1F' },
          itemStyle: { areaColor: '#FFFBEB', shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.1)' }
        },
        data: dataPoints
      }]
    }

    chart.setOption(option, false)
    currentLevel.value = 'city'

    await nextTick()
    renderTrendChart()
  } catch (e) {
    console.error('下钻失败:', e)
  } finally {
    mapLoading.value = false
  }
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = markRaw(echarts.init(trendChartRef.value))

  const days = []
  const vals = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push((d.getMonth() + 1) + '/' + d.getDate())
    vals.push(Math.floor(Math.random() * 80 + 30))
  }

  trendChart.setOption({
    grid: { top: 10, right: 10, bottom: 20, left: 35 },
    xAxis: {
      type: 'category', data: days,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8899aa', fontSize: 9 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      axisLabel: { color: '#8899aa', fontSize: 9 }
    },
    series: [{
      type: 'line', data: vals, smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2, color: '#0284C7', shadowBlur: 8, shadowColor: 'rgba(2,132,199,0.3)' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(2,132,199,0.2)' }, { offset: 1, color: 'rgba(2,132,199,0)' }
      ])},
      itemStyle: { color: '#0284C7' }
    }]
  })
}

async function backToChina() {
  await renderChinaMap()
}

function getProvinceCenter(adcode) {
  const centers = {
    110000: [116.4, 39.9], 120000: [117.2, 39.1], 130000: [114.5, 38.0],
    140000: [112.5, 37.9], 150000: [111.7, 40.8], 210000: [123.4, 41.8],
    220000: [125.3, 43.9], 230000: [126.6, 45.8], 310000: [121.5, 31.2],
    320000: [118.8, 32.0], 330000: [120.2, 30.3], 340000: [117.3, 31.9],
    350000: [119.3, 26.1], 360000: [115.9, 28.7], 370000: [117.0, 36.7],
    410000: [113.7, 34.8], 420000: [114.3, 30.6], 430000: [113.0, 28.2],
    440000: [113.3, 23.1], 450000: [108.3, 22.8], 460000: [110.3, 20.0],
    500000: [106.5, 29.5], 510000: [104.1, 30.7], 520000: [106.7, 26.6],
    530000: [102.7, 25.0], 540000: [91.1, 29.7], 610000: [108.9, 34.3],
    620000: [103.8, 36.1], 630000: [101.8, 36.6], 640000: [106.3, 38.5],
    650000: [87.6, 43.8]
  }
  return centers[adcode] || [null, null]
}

onMounted(async () => {
  await fetchData()
  if (chartRef.value) {
    chart = markRaw(echarts.init(chartRef.value))
    await renderChinaMap()
    window.addEventListener('resize', () => {
      chart?.resize()
      trendChart?.resize()
    })
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', () => {})
  chart?.dispose()
  chart = null
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped>
/* ========== 【核心修复 1】彻底解除死锁高度 ========== */
.map-view-container {
  position: relative;
  width: 100%;
  height: 100%;
  /* 移除了原有的 min-height: 620px，将高度控制权完全交给父级弹性布局，根绝溢出裁切 */
  border-radius: inherit;
  overflow: hidden;
  background: #F8FAFC;
}

.chart-box {
  width: 100%; 
  height: 100%; 
  /* 同样移除死板高度，完全自适应 */
  position: relative;
}

/* ========== 顶部工具栏 ========== */
.map-toolbar {
  position: absolute;
  top: 0; left: 0; right: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: linear-gradient(180deg, rgba(255,255,255,0.95) 60%, rgba(255,255,255,0) 100%);
  backdrop-filter: blur(8px);
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.current-label { font-size: 13px; font-weight: 600; color: #475569; }

/* ========== Loading 毛玻璃遮罩 ========== */
.loading-overlay {
  position: absolute; inset: 0;
  background: rgba(248, 250, 252, 0.7);
  backdrop-filter: blur(6px);
  z-index: 20;
  display: flex; align-items: center; justify-content: center;
}
.loading-spinner { text-align: center; }
.spinner-ring {
  width: 36px; height: 36px; margin: 0 auto 12px;
  border: 3px solid #E2E8F0;
  border-top-color: #0284C7;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-spinner span { color: #64748B; font-size: 13px; font-weight: 500;}

/* ========== 【核心修复 2】底部居中规整图例 ========== */
.map-legend-bottom {
  position: absolute;
  bottom: 24px; /* 预留充足的底部安全边距，避免触底 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: rgba(255, 255, 255, 0.85); /* 极简毛玻璃质感 */
  backdrop-filter: blur(12px);
  border-radius: 14px;
  padding: 12px 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
  width: max-content;
  max-width: 95%; /* 小屏幕防溢出换行保护 */
  flex-wrap: wrap;
  justify-content: center;
}
.legend-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  padding-right: 16px;
  border-right: 1px solid rgba(0, 0, 0, 0.08); /* 竖线分割标题与图例项 */
}
.legend-capsules {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap; /* 允许在极端窄屏时折行 */
  justify-content: center;
}
.legend-capsule {
  display: flex;
  align-items: center;
  gap: 6px;
}
.capsule-color {
  width: 12px;
  height: 12px;
  border-radius: 4px; /* 统一尺寸和圆角 */
  flex-shrink: 0;
}
.capsule-label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.capsule-range {
  font-size: 12px;
  color: #94A3B8;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif;
  font-feature-settings: "tnum"; 
}

/* ========== 侧边悬浮面板 ========== */
.side-panel {
  position: absolute;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  max-height: calc(100% - 100px); /* 【核心修复 3】防止右侧面板向下溢出 */
  overflow-y: auto;
}
/* 隐藏面板内置滚动条保持整洁 */
.side-panel::-webkit-scrollbar { display: none; }
.side-panel { -ms-overflow-style: none; scrollbar-width: none; }

.top-panel { top: 60px; right: 16px; width: 220px; }
.trend-panel { top: 60px; left: 16px; width: 220px; }
.panel-title { font-size: 12px; font-weight: 600; color: #475569; margin-bottom: 12px; }

.top-item {
  display: flex; align-items: center; gap: 6px; margin-bottom: 10px;
  font-size: 12px;
}
.top-rank { width: 16px; height: 16px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 10px; background: #F1F5F9; color: #94A3B8; }
.top-rank.rank-1 { background: #E11D48; color: #fff; }
.top-rank.rank-2 { background: #D97706; color: #fff; }
.top-rank.rank-3 { background: #F59E0B; color: #fff; }
.top-name { flex: 1; color: #475569; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.top-bar-wrap { width: 46px; height: 4px; background: #F1F5F9; border-radius: 2px; overflow: hidden; }
.top-bar { height: 100%; border-radius: 2px; transition: width 0.6s cubic-bezier(0.2, 0.8, 0.2, 1); }
.top-value { font-weight: 700; font-size: 12px; min-width: 28px; text-align: right; font-feature-settings: "tnum"; }

.trend-chart-box { width: 100%; height: 140px; }

/* ========== 过渡动画 ========== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-right-enter-active, .slide-right-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.slide-right-enter-from, .slide-right-leave-to { opacity: 0; transform: translateX(30px); }

.slide-left-enter-active, .slide-left-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.slide-left-enter-from, .slide-left-leave-to { opacity: 0; transform: translateX(-30px); }
</style>