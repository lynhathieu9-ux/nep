<template>
  <div class="top-cards-container">
    <div class="cards-wrapper">
      <div v-for="(card, i) in cards" :key="i" class="stat-card" :class="{ 'main-card': i === 3, 'green-glow': card.glow === 'green', 'purple-glow': card.glow === 'purple' }">
        <div class="card-inner">
          <div class="card-icon"><span class="icon-emoji">{{ card.icon }}</span></div>
          <div class="card-number"><span class="number-value">{{ card.value }}</span></div>
          <div class="card-label">{{ card.label }}</div>
        </div>
        <div class="card-edge"></div>
        <div class="card-shine"></div>
      </div>
    </div>
    <div class="decorative-arc"></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview } from '@/api/statistics'

const cards = ref([
  { value:0, label:'注册监督员', icon:'🔭', glow:'green' },
  { value:0, label:'反馈总数', icon:'📋', glow:null },
  { value:0, label:'AQI检测数', icon:'⚡', glow:null },
  { value:0, label:'覆盖城市', icon:'🌍', glow:'purple' },
  { value:0, label:'已完成', icon:'✅', glow:null },
  { value:0, label:'待处理', icon:'⚠️', glow:null }
])

onMounted(async function() {
  try {
    var res = await getOverview()
    var d = (res && res.data) ? res.data : {}
    cards.value[0].value = d.totalUsers || 0
    cards.value[1].value = d.totalFeedbacks || 0
    cards.value[2].value = d.totalDetections || 0
    cards.value[3].value = d.totalCities || 0
    cards.value[4].value = d.completedFeedbacks || 0
    cards.value[5].value = d.pendingFeedbacks || 0
  } catch(e) { console.error('TopCards加载失败:', e) }
})
</script>

<style scoped>
.top-cards-container {
  position:relative; padding:16px 24px;
  background:linear-gradient(180deg, rgba(8,18,35,0.9) 0%, rgba(5,12,22,0.85) 100%);
  border-bottom:1px solid rgba(64,144,188,0.1); overflow:hidden;
}
.cards-wrapper { display:flex; justify-content:space-between; gap:14px; position:relative; z-index:2; }
.stat-card {
  position:relative; flex:1; min-width:110px; height:88px; border-radius:12px;
  background:linear-gradient(135deg, rgba(15,30,54,0.7) 0%, rgba(8,18,35,0.9) 100%);
  border:1px solid rgba(64,144,188,0.15); overflow:hidden; transition:all 0.4s ease;
}
.stat-card:hover { transform:translateY(-3px); border-color:rgba(64,144,188,0.35); box-shadow:0 8px 25px rgba(0,0,0,0.3); }
.stat-card.main-card { border:1px solid rgba(168,85,247,0.3); box-shadow:0 0 30px rgba(168,85,247,0.15); }
.stat-card.green-glow .number-value { color:#2aa876; text-shadow:0 0 20px rgba(42,168,118,0.5); }
.stat-card.green-glow .card-icon { background:rgba(42,168,118,0.15); border-color:rgba(42,168,118,0.3); }
.stat-card.purple-glow .number-value { color:#a855f7; text-shadow:0 0 20px rgba(168,85,247,0.5); }
.stat-card.purple-glow .card-icon { background:rgba(168,85,247,0.15); border-color:rgba(168,85,247,0.3); }
.card-inner { position:relative; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; padding:8px; border-bottom:1px solid rgba(64,144,188,0.1); }
.card-icon { width:32px; height:32px; border-radius:50%; background:rgba(64,144,188,0.1); border:1px solid rgba(64,144,188,0.2); display:flex; align-items:center; justify-content:center; margin-bottom:6px; }
.icon-emoji { font-size:14px; }
.card-number { position:relative; margin-bottom:2px; }
.number-value { font-size:24px; font-weight:700; color:#fff; font-family:'SF Mono',monospace; }
.stat-card.main-card .number-value { font-size:32px; }
.card-label { font-size:11px; color:#8fa3b8; text-align:center; }
.card-edge { position:absolute; top:0; left:0; right:0; height:1px; background:linear-gradient(90deg, transparent, rgba(64,144,188,0.4), transparent); }
.stat-card.main-card .card-edge { height:2px; background:linear-gradient(90deg, transparent, rgba(168,85,247,0.6), transparent); }
.card-shine { position:absolute; top:0; left:-100%; width:50%; height:100%; background:linear-gradient(90deg, transparent, rgba(255,255,255,0.05), transparent); transform:skewX(-20deg); animation:shine 4s infinite; }
@keyframes shine { 0%{left:-100%} 100%{left:200%} }
.decorative-arc { position:absolute; bottom:0; left:50%; transform:translateX(-50%); width:80%; height:20px; border-radius:50% 50% 0 0; background:linear-gradient(180deg, rgba(64,144,188,0.15) 0%, transparent 100%); z-index:1; }
</style>
