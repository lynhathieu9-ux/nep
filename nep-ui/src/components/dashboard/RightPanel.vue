<template>
  <div class="right-panel">
    <div class="aqi-card">
      <div class="aqi-header">
        <span class="aqi-city">{{ selectedCity || '--' }}</span>
      </div>
      <div class="aqi-value">{{ selectedAqi || 0 }}</div>
      <div class="aqi-status">AQI污染指数</div>
    </div>

    <div class="top-cities-card">
      <div class="card-title">TOP 5 城市</div>
      <div class="city-list">
        <div v-for="(city, idx) in topCities" :key="idx" class="city-item">
          <span class="city-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
          <span class="city-name">{{ city.cityName || city.name || '--' }}</span>
          <span class="city-value">{{ city.avgAqi || city.value || 0 }}</span>
        </div>
      </div>
    </div>

    <div class="pollutant-legend-card">
      <div class="card-title">AQI污染等级</div>
      <div class="pollutant-list">
        <div class="pollutant-item" v-for="(l, i) in levels" :key="i">
          <span class="pollutant-color" :style="{ background: l.color }"></span>
          <span class="pollutant-name">{{ l.label }}</span>
          <span class="pollutant-value">{{ l.range }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMapAqi } from '@/api/statistics'

const cityData = ref([])
const levels = [
  { color:'#059669', label:'优', range:'0-50' },
  { color:'#D97706', label:'良', range:'51-100' },
  { color:'#F59E0B', label:'轻度', range:'101-150' },
  { color:'#E11D48', label:'中度', range:'151-200' },
  { color:'#9F1239', label:'重度', range:'201-300' },
  { color:'#4C0519', label:'严重', range:'>300' }
]

const topCities = computed(function() {
  return cityData.value.filter(function(c) { return c.avgAqi != null }).sort(function(a, b) { return (b.avgAqi||0) - (a.avgAqi||0) }).slice(0, 5)
})

const selectedCity = computed(function() { var t = topCities.value; return t.length > 0 ? (t[0].cityName || t[0].name || '--') : '--' })
const selectedAqi = computed(function() { var t = topCities.value; return t.length > 0 ? (t[0].avgAqi || t[0].value || 0) : 0 })

onMounted(async function() {
  try {
    var res = await getMapAqi()
    cityData.value = (res && res.data && res.data.cities) ? res.data.cities : []
  } catch(e) { console.error('RightPanel加载失败:', e) }
})
</script>

<style scoped>
.right-panel { position:absolute; top:20px; right:20px; width:200px; display:flex; flex-direction:column; gap:14px; z-index:20; }
.aqi-card { background:rgba(15,30,54,0.85); backdrop-filter:blur(12px); border-radius:12px; padding:16px; border:1px solid rgba(245,166,35,0.3); box-shadow:0 4px 20px rgba(0,0,0,0.3); text-align:center; }
.aqi-header { margin-bottom:12px; }
.aqi-city { font-size:12px; font-weight:600; color:#c8d6e5; }
.aqi-value { font-size:48px; font-weight:700; color:#f5a623; text-shadow:0 0 20px rgba(245,166,35,0.5); font-family:'SF Mono',monospace; }
.aqi-status { font-size:11px; color:#8fa3b8; margin-top:4px; }
.top-cities-card { background:rgba(15,30,54,0.85); backdrop-filter:blur(12px); border-radius:12px; padding:14px; border:1px solid rgba(64,144,188,0.2); box-shadow:0 4px 20px rgba(0,0,0,0.3); }
.card-title { font-size:12px; font-weight:600; color:#8fa3b8; margin-bottom:12px; }
.city-list { display:flex; flex-direction:column; gap:8px; }
.city-item { display:flex; align-items:center; gap:8px; padding:6px 8px; background:rgba(0,0,0,0.2); border-radius:6px; transition:transform 0.3s cubic-bezier(0.34,1.56,0.64,1), background 0.3s, box-shadow 0.3s; cursor:pointer; }
.city-item:hover { transform:translateY(-3px) scale(1.02); background:rgba(64,144,188,0.15); box-shadow:0 4px 16px rgba(0,0,0,0.3); }
.city-rank { width:16px; height:16px; border-radius:4px; display:flex; align-items:center; justify-content:center; font-size:10px; font-weight:700; background:#1e293b; color:#6b7280; }
.city-rank.rank-1 { background:#f56c6c; color:#fff; }
.city-rank.rank-2 { background:#f5a623; color:#fff; }
.city-rank.rank-3 { background:#d97706; color:#fff; }
.city-name { flex:1; font-size:11px; color:#c8d6e5; }
.city-value { font-size:11px; font-weight:600; color:#f5a623; font-family:'SF Mono',monospace; }
.pollutant-legend-card { background:rgba(15,30,54,0.85); backdrop-filter:blur(12px); border-radius:12px; padding:14px; border:1px solid rgba(64,144,188,0.2); box-shadow:0 4px 20px rgba(0,0,0,0.3); }
.pollutant-list { display:flex; flex-direction:column; gap:8px; }
.pollutant-item { display:flex; align-items:center; gap:8px; padding:6px 8px; background:rgba(0,0,0,0.2); border-radius:6px; }
.pollutant-color { width:10px; height:10px; border-radius:3px; }
.pollutant-name { flex:1; font-size:11px; color:#c8d6e5; }
.pollutant-value { font-size:11px; font-weight:600; color:#c8d6e5; font-family:'SF Mono',monospace; }
</style>
