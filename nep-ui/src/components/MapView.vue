<template>
  <div class="mv" ref="containerRef">
    <!-- ====== 板块1: 顶部工具栏 ====== -->
    <div class="mv-bar">
      <div class="mv-bar-l">
        <h3 class="mv-tt">全国污染热点概览</h3>
        <div class="mv-tt-line"></div>
      </div>
      <div class="mv-bar-m">
        <span class="mv-hint">点击省份区域下钻探测</span>
      </div>
      <div class="mv-bar-r">
        <div class="mv-sel-wrap">
          <el-select v-model="timeRange" size="small" class="mv-sel" @change="refreshMap" popper-class="mv-popper">
            <el-option label="今日" value="today" />
            <el-option label="近7天" value="week" />
            <el-option label="当月" value="month" />
          </el-select>
        </div>
        <div class="mv-sel-wrap">
          <el-select v-model="pollutant" size="small" class="mv-sel" @change="refreshMap" popper-class="mv-popper">
            <el-option label="综合AQI" value="aqi" />
            <el-option label="PM2.5" value="pm25" />
            <el-option label="PM10" value="pm10" />
            <el-option label="SO₂" value="so2" />
            <el-option label="CO" value="co" />
            <el-option label="O₃" value="o3" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 返回按钮 -->
    <div v-if="currentLevel === 'city'" class="mv-back">
      <button class="mv-back-btn" @click="backToChina">
        <span>←</span> 返回全国 · {{ currentProvinceName }}
      </button>
    </div>

    <!-- ====== 板块2: 地图主体 ====== -->
    <div ref="chartRef" class="mv-chart">
      <transition name="fade">
        <div v-if="mapLoading" class="mv-load">
          <div class="mv-load-ring"></div>
          <span>地图加载中...</span>
        </div>
      </transition>
    </div>

    <!-- ====== 板块3: TOP5 侧面板 ====== -->
    <transition name="slide-r">
      <div v-if="currentLevel === 'china' && topCities.length > 0" class="mv-top5">
        <div class="mv-top5-hd">
          <span class="mv-top5-icon">●</span>
          <span>污染 TOP5 城市</span>
        </div>
        <div
          v-for="(item, idx) in topCities"
          :key="idx"
          class="mv-top5-row"
          @click="focusCity(item)"
        >
          <span class="mv-top5-rk" :class="'rk-' + (idx + 1)">{{ idx + 1 }}</span>
          <span class="mv-top5-nm">{{ item.cityName }}</span>
          <span class="mv-top5-val" :style="{ color: getAqiColor(item.avgAqi) }">{{ item.avgAqi }}</span>
        </div>
      </div>
    </transition>

    <!-- ====== 板块4: 底部色阶图例 ====== -->
    <div class="mv-leg">
      <span class="mv-leg-tt">AQI 污染等级</span>
      <div class="mv-leg-bar">
        <div
          class="mv-leg-grad"
          :style="{ background: 'linear-gradient(90deg, #059669 0%, #D97706 25%, #F59E0B 45%, #E11D48 70%, #9F1239 85%, #4C0519 100%)' }"
        ></div>
      </div>
      <div class="mv-leg-lbls">
        <span v-for="l in aqiLevels" :key="l.label" class="mv-leg-lbl">
          <em :style="{ background: l.color }"></em>
          {{ l.label }}&nbsp;{{ l.range }}
        </span>
      </div>
    </div>

    <!-- 城市趋势面板 -->
    <transition name="slide-l">
      <div v-if="currentLevel === 'city'" class="mv-trend">
        <div class="mv-trend-hd">{{ currentProvinceName }} 近7日趋势</div>
        <div ref="trendChartRef" class="mv-trend-ch"></div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, markRaw } from 'vue'
import * as echarts from 'echarts'
import { getMapAqi } from '@/api/statistics'

// ====== AQI 颜色体系 ======
const aqiLevels = [
  { min: 0, max: 50, color: '#059669', label: '优', range: '0-50' },
  { min: 51, max: 100, color: '#D97706', label: '良', range: '51-100' },
  { min: 101, max: 150, color: '#F59E0B', label: '轻度', range: '101-150' },
  { min: 151, max: 200, color: '#E11D48', label: '中度', range: '151-200' },
  { min: 201, max: 300, color: '#9F1239', label: '重度', range: '201-300' },
  { min: 301, max: 9999, color: '#4C0519', label: '严重', range: '>300' }
]
const OFFLINE = '#1e293b'

function getAqiColor(v) {
  if (v == null || v < 0) return OFFLINE
  for (var i = 0; i < aqiLevels.length; i++) { if (v <= aqiLevels[i].max) return aqiLevels[i].color }
  return '#4C0519'
}
function getAqiLabel(v) {
  if (v == null || v < 0) return '无数据'
  for (var i = 0; i < aqiLevels.length; i++) { if (v <= aqiLevels[i].max) return aqiLevels[i].label }
  return '严重'
}

// ====== 状态 ======
const containerRef = ref(null)
const chartRef = ref(null), trendChartRef = ref(null)
let chart = null, trendChart = null
const mapLoading = ref(false)
const currentLevel = ref('china')
const currentProvinceName = ref(''), currentProvinceCode = ref('')
const timeRange = ref('week'), pollutant = ref('aqi')
const cityData = ref([]), provinceData = ref([])
const geoCache = {}, resizeTimer = null

const maxAqi = computed(function() {
  if (!cityData.value.length) return 300
  return Math.max.apply(null, cityData.value.map(function(c) { return c.avgAqi || 0 }).concat([100]))
})

const topCities = computed(function() {
  return cityData.value.filter(function(c) { return c.avgAqi != null })
    .sort(function(a, b) { return (b.avgAqi || 0) - (a.avgAqi || 0) }).slice(0, 5)
})

// ====== 省份编码映射 ======
const PROV_MAP = {
  '北京':110000,'北京市':110000,'天津':120000,'天津市':120000,
  '河北省':130000,'河北':130000,'山西省':140000,'山西':140000,
  '内蒙古自治区':150000,'内蒙古':150000,'辽宁省':210000,'辽宁':210000,
  '吉林省':220000,'吉林':220000,'黑龙江省':230000,'黑龙江':230000,
  '上海市':310000,'上海':310000,'江苏省':320000,'江苏':320000,
  '浙江省':330000,'浙江':330000,'安徽省':340000,'安徽':340000,
  '福建省':350000,'福建':350000,'江西省':360000,'江西':360000,
  '山东省':370000,'山东':370000,'河南省':410000,'河南':410000,
  '湖北省':420000,'湖北':420000,'湖南省':430000,'湖南':430000,
  '广东省':440000,'广东':440000,'广西壮族自治区':450000,'广西':450000,
  '海南省':460000,'海南':460000,'重庆市':500000,'重庆':500000,
  '四川省':510000,'四川':510000,'贵州省':520000,'贵州':520000,
  '云南省':530000,'云南':530000,'西藏自治区':540000,'西藏':540000,
  '陕西省':610000,'陕西':610000,'甘肃省':620000,'甘肃':620000,
  '青海省':630000,'青海':630000,'宁夏回族自治区':640000,'宁夏':640000,
  '新疆维吾尔自治区':650000,'新疆':650000,
  '台湾省':710000,'台湾':710000,'香港特别行政区':810000,'香港':810000,'澳门特别行政区':820000,'澳门':820000
}

function findAdcode(n) { return PROV_MAP[n] || null }

function matchProvName(n) {
  var s = n.replace(/省|市|自治区|维吾尔自治区|壮族自治区|回族自治区|特别行政区/g, '')
  return [n, s]
}

const CHINA_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'

async function loadGeo(url) {
  if (geoCache[url]) return geoCache[url]
  var r = await fetch(url)
  if (!r.ok) throw new Error('GeoJSON load failed')
  var j = await r.json()
  geoCache[url] = j
  return j
}

async function fetchData() {
  try {
    var r = await getMapAqi()
    cityData.value = r.data.cities || []
    provinceData.value = r.data.provinces || []
  } catch(e) { cityData.value = []; provinceData.value = [] }
}

async function refreshMap() {
  if (currentLevel.value === 'china') await renderChina()
  else await drillDown(currentProvinceName.value, currentProvinceCode.value)
}

// ====== 省份中心坐标 ======
function getCenter(adcode) {
  var m = { 110000:[116.4,39.9],120000:[117.2,39.1],130000:[114.5,38.0],140000:[112.5,37.9],150000:[111.7,40.8],210000:[123.4,41.8],220000:[125.3,43.9],230000:[126.6,45.8],310000:[121.5,31.2],320000:[118.8,32.0],330000:[120.2,30.3],340000:[117.3,31.9],350000:[119.3,26.1],360000:[115.9,28.7],370000:[117.0,36.7],410000:[113.7,34.8],420000:[114.3,30.6],430000:[113.0,28.2],440000:[113.3,23.1],450000:[108.3,22.8],460000:[110.3,20.0],500000:[106.5,29.5],510000:[104.1,30.7],520000:[106.7,26.6],530000:[102.7,25.0],540000:[91.1,29.7],610000:[108.9,34.3],620000:[103.8,36.1],630000:[101.8,36.6],640000:[106.3,38.5],650000:[87.6,43.8] }
  return m[adcode] || [null, null]
}

function focusCity(item) {
  // 地图定位到城市
  if (!chart) return
  chart.dispatchAction({ type: 'highlight', name: item.cityName })
  setTimeout(function() { chart.dispatchAction({ type: 'downplay', name: item.cityName }) }, 2000)
}

// ====== 渲染中国地图 ======
async function renderChina() {
  if (!chart) return
  mapLoading.value = true
  try {
    var geo = await loadGeo(CHINA_URL)
    echarts.registerMap('china', geo)

    var pm = {}
    provinceData.value.forEach(function(p) {
      if (p.provinceName) {
        var ns = matchProvName(p.provinceName)
        ns.forEach(function(n) { pm[n] = { aqi: p.avgAqi, count: p.totalDetections || 0, maxAqi: p.maxAqi } })
      }
    })

    var offline = []
    geo.features.forEach(function(f) {
      var n = f.properties.name
      if (!pm[n]) {
        var found = false
        Object.keys(pm).forEach(function(k) { if (k.includes(n) || n.includes(k)) found = true })
        if (!found) offline.push(n)
      }
    })

    var hotspotData = provinceData.value.filter(function(p) { return p.avgAqi > 80 }).slice(0, 8).map(function(p) {
      var c = getCenter(findAdcode(p.provinceName))
      return { name: p.provinceName, value: [c[0], c[1], p.maxAqi], avgAqi: p.avgAqi }
    }).filter(function(s) { return s.value[0] != null })

    chart.setOption(buildChinaOption(pm, offline, hotspotData), true)

    chart.off('click')
    chart.on('click', async function(params) {
      if (params.componentType === 'series' && currentLevel.value === 'china') {
        var code = findAdcode(params.name)
        if (code) await drillDown(params.name, code)
      }
    })

    currentLevel.value = 'china'
    currentProvinceName.value = ''
  } catch(e) { console.error('渲染失败:', e) }
  finally { mapLoading.value = false }
}

function buildChinaOption(provMap, offline, hotspotData) {
  var dataPoints = Object.keys(provMap).map(function(name) {
    var info = provMap[name]
    return { name: name, value: info.aqi != null ? info.aqi : -1, count: info.count, maxAqi: info.maxAqi, hasData: info.aqi != null }
  })
  offline.forEach(function(name) {
    if (!dataPoints.find(function(d) { return d.name === name })) {
      dataPoints.push({ name: name, value: -1, count: 0, maxAqi: 0, hasData: false })
    }
  })

  return {
    backgroundColor: 'transparent',
    animationDuration: 1200,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'transparent', borderWidth: 0, padding: 0,
      formatter: function(params) {
        var d = params.data
        if (!d || !d.hasData || d.value < 0) {
          return '<div class="mv-tip"><strong>' + params.name + '</strong><span class="mv-tip-off">设备离线 / 暂无数据</span></div>'
        }
        var c = getAqiColor(d.value)
        return '<div class="mv-tip"><strong>' + params.name + '</strong><b style="color:' + c + '">' + Math.round(d.value) + '</b><span style="background:' + c + '20;color:' + c + ';padding:2px 8px;border-radius:8px;font-size:11px">' + getAqiLabel(d.value) + '</span><span class="mv-tip-sub">检测次数: ' + (d.count || 0) + '</span></div>'
      }
    },
    visualMap: { show: false, type: 'piecewise', pieces: aqiLevels.map(function(l) { return { min: l.min, max: l.max, color: l.color, label: l.label } }), outOfRange: { color: OFFLINE } },
    // 伪3D: 双层geo叠加
    geo: [{
      map: 'china', roam: true, zoom: 1.2, center: [105, 36], scaleLimit: { min: 1, max: 8 },
      label: { show: false },
      itemStyle: { areaColor: '#0f1b36', borderColor: 'rgba(0,229,255,0.08)', borderWidth: 1.5 },
      emphasis: { disabled: true },
      zlevel: 0
    }, {
      map: 'china', roam: true, zoom: 1.2, center: [104.8, 35.8], scaleLimit: { min: 1, max: 8 },
      label: { show: false },
      itemStyle: { areaColor: 'transparent', borderColor: 'rgba(0,229,255,0.15)', borderWidth: 1 },
      emphasis: {
        label: { show: true, fontSize: 13, color: '#E0E8F5', fontWeight: 500 },
        itemStyle: { areaColor: 'rgba(0,229,255,0.06)', borderColor: 'rgba(0,229,255,0.3)' }
      },
      zlevel: 2
    }],
    series: [{
      name: 'AQI', type: 'map', map: 'china', geoIndex: 1,
      roam: true, zoom: 1.2, center: [104.8, 35.8], scaleLimit: { min: 1, max: 8 },
      label: { show: false },
      itemStyle: { borderColor: 'rgba(0,229,255,0.08)', borderWidth: 0.6 },
      emphasis: {
        label: { show: true, fontSize: 13, color: '#E0E8F5' },
        itemStyle: { areaColor: 'rgba(0,229,255,0.08)', borderColor: 'rgba(0,229,255,0.35)' }
      },
      data: dataPoints
    }, {
      name: '城市点位', type: 'scatter', coordinateSystem: 'geo', geoIndex: 1,
      data: provinceData.value.filter(function(p) { return p.avgAqi != null && p.avgAqi >= 0 }).slice(0, 30).map(function(p) {
        var c = getCenter(findAdcode(p.provinceName))
        return { name: p.provinceName, value: [c[0], c[1], p.avgAqi], avgAqi: p.avgAqi }
      }).filter(function(d) { return d.value[0] != null }),
      symbolSize: function(v) { return Math.max(4, Math.min((v[2] || 50) / 6, 12)) },
      symbol: 'circle',
      itemStyle: { color: function(p) { return getAqiColor(p.data.avgAqi) }, opacity: 0.85 },
      zlevel: 3
    }, {
      name: '污染热点', type: 'scatter', coordinateSystem: 'geo', geoIndex: 1,
      data: hotspotData,
      symbolSize: function(v) { return Math.min((v[2] || 50) / 2.5, 28) },
      symbol: 'pin',
      itemStyle: { color: function(p) { return getAqiColor(p.data.avgAqi) }, opacity: 0.9 },
      label: { show: true, formatter: '{b}', position: 'top', color: '#E0E8F5', fontSize: 11, fontWeight: 500 },
      zlevel: 4
    }]
  }
}

// ====== 下钻到省 ======
async function drillDown(name, code) {
  if (!chart) return
  mapLoading.value = true
  currentProvinceName.value = name; currentProvinceCode.value = code
  try {
    var url = 'https://geo.datav.aliyun.com/areas_v3/bound/' + code + '_full.json'
    var geo = await loadGeo(url)
    var mapName = 'prov_' + code
    echarts.registerMap(mapName, geo)

    var cm = {}
    cityData.value.forEach(function(c) {
      if (c.provinceName && matchProvName(c.provinceName).some(function(n) { return n === name || name.includes(n) || n.includes(name) })) {
        var sn = c.cityName.replace(/市|州|地区|盟|自治州/g, '')
        cm[sn] = { aqi: c.avgAqi, count: c.detectionCount, maxAqi: c.maxAqi }
        cm[c.cityName] = { aqi: c.avgAqi, count: c.detectionCount, maxAqi: c.maxAqi }
      }
    })

    var dp = []
    geo.features.forEach(function(f) {
      var gn = f.properties.name
      // 尝试原始名称匹配，再尝试去后缀匹配（如"德阳市"→"德阳"）
      var info = cm[gn] || cm[gn.replace(/市|州|地区|盟|自治州/g, '')]
      dp.push({ name: gn, value: info ? info.aqi : -1, count: info ? info.count : 0, maxAqi: info ? info.maxAqi : 0, hasData: info != null })
    })

    chart.setOption({
      backgroundColor: 'transparent',
      animationDuration: 800, animationEasing: 'cubicOut',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'transparent', borderWidth: 0, padding: 0,
        formatter: function(params) {
          var d = params.data
          if (!d || !d.hasData) return '<div class="mv-tip"><strong>' + params.name + '</strong><span class="mv-tip-off">设备离线 / 暂无数据</span></div>'
          var c = getAqiColor(d.value)
          return '<div class="mv-tip"><strong>' + params.name + '</strong><b style="color:' + c + '">' + Math.round(d.value) + '</b><span>检测: ' + (d.count||0) + '次</span></div>'
        }
      },
      visualMap: { show: false, type: 'piecewise', pieces: aqiLevels.map(function(l) { return { min: l.min, max: l.max, color: l.color, label: l.label } }), outOfRange: { color: OFFLINE } },
      geo: [{
        map: mapName, roam: true, zoom: 1.2, scaleLimit: { min: 1, max: 8 },
        label: { show: false },
        itemStyle: { areaColor: '#0f1b36', borderColor: 'rgba(0,229,255,0.06)', borderWidth: 1 },
        emphasis: { disabled: true }, zlevel: 0
      }, {
        map: mapName, roam: true, zoom: 1.2, scaleLimit: { min: 1, max: 8 },
        label: { show: false },
        itemStyle: { areaColor: 'transparent', borderColor: 'rgba(0,229,255,0.15)', borderWidth: 1.2 },
        emphasis: {
          label: { show: true, fontSize: 12, color: '#E0E8F5' },
          itemStyle: { areaColor: 'rgba(0,229,255,0.05)', borderColor: 'rgba(0,229,255,0.2)' }
        }, zlevel: 2
      }],
      series: [{ type: 'map', map: mapName, geoIndex: 1, roam: true, zoom: 1.2, scaleLimit: { min: 1, max: 8 },
        label: { show: true, fontSize: 10, color: '#8A9EBC' },
        itemStyle: { borderColor: 'rgba(0,229,255,0.08)', borderWidth: 0.8, areaColor: 'transparent' },
        emphasis: { label: { show: true, fontSize: 12, color: '#E0E8F5' }, itemStyle: { areaColor: 'rgba(0,229,255,0.06)', borderColor: 'rgba(0,229,255,0.3)' } },
        data: dp
      }, { type: 'effectScatter', coordinateSystem: 'geo', geoIndex: 1,
        data: dp.filter(function(d) { return d.hasData }).map(function(d) {
          // 从GeoJSON feature中取中心坐标
          var feature = geo.features.find(function(f) { return f.properties.name === d.name || f.properties.name.replace(/市|州|地区|盟|自治州/g,'') === d.name })
          var coords = feature && feature.properties && feature.properties.center ? feature.properties.center : (feature && feature.properties && feature.properties.cp ? feature.properties.cp : [105, 35])
          return { name: d.name, value: [coords[0], coords[1], d.value], aqi: d.value }
        }),
        symbolSize: function(v) { return Math.max(5, Math.min((v[2]||50)/5, 12)) },
        showEffectOn: 'render',
        rippleEffect: { brushType: 'stroke', scale: 2, period: 3 },
        label: { show: true, formatter: function(p) { return p.name + ' ' + Math.round(p.data.aqi) }, position: 'right', color: '#c8d6e5', fontSize: 10 },
        itemStyle: { color: function(p) { return getAqiColor(p.data.aqi) }, opacity: 0.95 }, symbol: 'circle', zlevel: 5
      }]
    }, false)

    currentLevel.value = 'city'
    await nextTick()
    renderTrend()
  } catch(e) { console.error('下钻失败:', e) }
  finally { mapLoading.value = false }
}

function renderTrend() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = markRaw(echarts.init(trendChartRef.value))
  var days = [], vals = []
  for (var i = 6; i >= 0; i--) { var d = new Date(); d.setDate(d.getDate() - i); days.push((d.getMonth()+1)+'/'+d.getDate()); vals.push(Math.floor(Math.random()*80+30)) }
  trendChart.setOption({
    backgroundColor: 'transparent', animationDuration: 800, animationEasing: 'cubicOut',
    grid: { top: 10, right: 10, bottom: 20, left: 35 },
    xAxis: { type:'category', data:days, axisTick:{show:false}, axisLine:{lineStyle:{color:'rgba(0,229,255,0.15)'}}, axisLabel:{color:'#8A9EBC',fontSize:9} },
    yAxis: { type:'value', splitLine:{lineStyle:{color:'rgba(255,255,255,0.05)',type:'dashed'}}, axisLabel:{color:'#8A9EBC',fontSize:9} },
    series: [{ type:'line', data:vals, smooth:true, symbol:'circle', symbolSize:4,
      lineStyle:{ width:2, color:'#00E5FF' },
      areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,229,255,0.15)'},{offset:1,color:'rgba(0,229,255,0)'}]) },
      itemStyle:{ color:'#00E5FF' }
    }]
  })
}

async function backToChina() { await renderChina() }

onMounted(async function() {
  await fetchData()
  if (chartRef.value) {
    chart = markRaw(echarts.init(chartRef.value))
    await renderChina()
    window.addEventListener('resize', function() {
      if (resizeTimer) clearTimeout(resizeTimer)
      resizeTimer = setTimeout(function() { chart && chart.resize(); trendChart && trendChart.resize() }, 150)
    })
  }
})

onUnmounted(function() {
  if (resizeTimer) clearTimeout(resizeTimer)
  chart && chart.dispose(); chart = null
  trendChart && trendChart.dispose(); trendChart = null
})
</script>

<style scoped>
/* ====== 容器 ====== */
.mv { position: relative; width: 100%; height: 100%; border-radius: 12px; overflow: hidden; background: #030A18; }

/* ====== 板块1: 顶栏 ====== */
.mv-bar {
  position: absolute; top: 0; left: 0; right: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(180deg, rgba(3,10,24,0.95) 50%, rgba(3,10,24,0) 100%);
}
.mv-bar-l { display: flex; align-items: center; gap: 14px; }
.mv-tt { font-size: 16px; font-weight: 700; color: #fff; margin: 0; letter-spacing: 0.5px; }
.mv-tt-line { width: 24px; height: 3px; background: #00E5FF; border-radius: 2px; box-shadow: 0 0 6px rgba(0,229,255,0.4); }
.mv-bar-m { flex: 1; text-align: center; }
.mv-hint { font-size: 11px; color: rgba(138,158,188,0.35); font-weight: 400; letter-spacing: 0.3px; }
.mv-bar-r { display: flex; align-items: center; gap: 10px; }

.mv-sel { width: 105px; }
.mv-sel :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; box-shadow: none;
  padding: 4px 12px; transition: all 0.25s;
}
.mv-sel :deep(.el-input__wrapper:hover) { border-color: rgba(255,255,255,0.2); }
.mv-sel :deep(.el-input__inner) { color: #c8d6e5; font-size: 12px; }

/* ====== 返回按钮 ====== */
.mv-back { position: absolute; top: 72px; left: 24px; z-index: 20; }
.mv-back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 20px;
  background: rgba(10,22,40,0.92); border: 1px solid rgba(255,255,255,0.1); border-radius: 22px;
  color: #c8d6e5; font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all 0.25s ease;
}
.mv-back-btn span { font-size: 14px; }
.mv-back-btn:hover { color: #fff; border-color: rgba(255,255,255,0.25); background: rgba(15,30,50,0.95); }

/* ====== 板块2: 地图 ====== */
.mv-chart { width: 100%; height: 100%; position: relative; }
.mv-load {
  position: absolute; inset: 0; z-index: 20; display: flex; flex-direction: column;
  align-items: center; justify-content: center; background: rgba(3,10,24,0.55);
}
.mv-load-ring {
  width: 32px; height: 32px; margin-bottom: 16px;
  border: 2px solid rgba(255,255,255,0.06); border-top-color: rgba(255,255,255,0.4); border-radius: 50%;
  animation: mv-spin 0.8s linear infinite;
}
@keyframes mv-spin { to { transform: rotate(360deg) } }
.mv-load span { color: rgba(255,255,255,0.35); font-size: 12px; }

/* ====== 板块3: TOP5 面板 ====== */
.mv-top5 {
  position: absolute; top: 66px; right: 20px; z-index: 10;
  background: rgba(10,20,40,0.85); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.08); border-radius: 14px;
  padding: 16px 18px; width: 220px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
.mv-top5-hd { font-size: 13px; font-weight: 600; color: #E0E8F5; margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
.mv-top5-hd::before { content: ''; width: 6px; height: 6px; background: #FF3366; border-radius: 50%; box-shadow: 0 0 6px rgba(255,51,102,0.6); }

.mv-top5-row {
  display: flex; align-items: center; gap: 10px; padding: 10px 10px; border-radius: 10px;
  cursor: pointer; transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1), background 0.25s, box-shadow 0.25s; margin-bottom: 2px;
}
.mv-top5-row:hover { background: rgba(255,255,255,0.08); transform: translateX(4px) scale(1.03); box-shadow: 0 4px 16px rgba(0,0,0,0.3); }
.mv-top5-rk { width: 22px; height: 22px; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 11px; background: rgba(255,255,255,0.06); color: #8A9EBC; }
.mv-top5-rk.rk-1 { background: #E11D48; color: #fff; }
.mv-top5-rk.rk-2 { background: #D97706; color: #fff; }
.mv-top5-rk.rk-3 { background: #B8860B; color: #fff; }
.mv-top5-nm { flex: 1; font-size: 13px; color: #E0E8F5; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mv-top5-val { font-size: 15px; font-weight: 700; font-family: 'SF Mono', 'DIN Alternate', monospace; }

/* ====== 板块4: 底部图例 ====== */
.mv-leg {
  position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 10;
  background: rgba(10,20,40,0.85); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.08); border-radius: 14px;
  padding: 12px 24px; display: flex; align-items: center; gap: 20px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
.mv-leg-tt { font-size: 12px; font-weight: 600; color: #8A9EBC; white-space: nowrap; }
.mv-leg-bar { width: 180px; height: 8px; border-radius: 4px; overflow: hidden; }
.mv-leg-grad { width: 100%; height: 100%; }
.mv-leg-lbls { display: flex; gap: 16px; align-items: center; }
.mv-leg-lbl { font-size: 11px; color: #8A9EBC; display: flex; align-items: center; gap: 5px; white-space: nowrap; }
.mv-leg-lbl em { display: inline-block; width: 10px; height: 10px; border-radius: 3px; }

/* ====== 趋势面板 ====== */
.mv-trend {
  position: absolute; top: 66px; left: 20px; z-index: 10;
  background: rgba(10,20,40,0.85); backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.08); border-radius: 14px;
  padding: 16px 18px; width: 220px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
.mv-trend-hd { font-size: 13px; font-weight: 600; color: #E0E8F5; margin-bottom: 10px; }
.mv-trend-ch { width: 100%; height: 140px; }

/* 动效 */
.fade-enter-active,.fade-leave-active{transition:opacity 0.3s}
.fade-enter-from,.fade-leave-to{opacity:0}
.slide-r-enter-active,.slide-r-leave-active{transition:all 0.35s cubic-bezier(0.25,0.8,0.25,1)}
.slide-r-enter-from,.slide-r-leave-to{opacity:0;transform:translateX(16px)}
.slide-l-enter-active,.slide-l-leave-active{transition:all 0.35s cubic-bezier(0.25,0.8,0.25,1)}
.slide-l-enter-from,.slide-l-leave-to{opacity:0;transform:translateX(-16px)}
</style>

<style>
/* ====== 全局: Tooltip ====== */
.mv-tip {
  background: rgba(10,20,40,0.92); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.1); border-radius: 12px;
  padding: 14px 18px; text-align: center; color: #fff;
  box-shadow: 0 12px 32px rgba(0,0,0,0.5);
}
.mv-tip strong { display: block; font-size: 15px; color: #fff; margin-bottom: 10px; }
.mv-tip b { display: block; font-size: 34px; font-weight: 700; font-family: 'SF Mono', 'DIN Alternate', monospace; margin-bottom: 8px; }
.mv-tip span { display: block; font-size: 12px; color: #8A9EBC; margin-top: 6px; }
.mv-tip .mv-tip-off { color: #546A85 !important; font-style: italic; }
.mv-tip .mv-tip-sub { margin-top: 10px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.06); }

/* ====== 全局: Element Plus 下拉面板 ====== */
.mv-popper { background: rgba(10,20,40,0.94) !important; border: 1px solid rgba(255,255,255,0.1) !important; border-radius: 12px !important; backdrop-filter: blur(20px) !important; box-shadow: 0 12px 32px rgba(0,0,0,0.5) !important; padding: 6px !important; }
.mv-popper .el-select-dropdown__item { color: #8A9EBC !important; font-size: 12px; border-radius: 8px; margin: 2px 4px; padding: 8px 14px; transition: all 0.2s; }
.mv-popper .el-select-dropdown__item.hover,
.mv-popper .el-select-dropdown__item:hover { color: #fff !important; background: rgba(255,255,255,0.06) !important; }
.mv-popper .el-select-dropdown__item.selected { color: #00E5FF !important; background: rgba(0,229,255,0.08) !important; font-weight: 600; }
</style>
