<template>
  <div class="left-panel">
    <div class="panel-card">
      <div class="card-header"><span class="card-title">实时检测数据</span></div>
      <div class="mini-cards">
        <div class="mini-card" v-for="(it, i) in miniItems" :key="i">
          <div class="mini-value">{{ it.value }}</div>
          <div class="mini-label">{{ it.label }}</div>
        </div>
      </div>
    </div>

    <div class="panel-card status-card">
      <div class="card-header"><span class="card-title">实时数据</span></div>
      <div class="status-items">
        <div class="status-item" v-for="(s, i) in statusItems" :key="i">
          <span class="status-indicator" :class="s.cls"></span>
          <span class="status-value">{{ s.value }}</span>
          <span class="status-label">{{ s.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getOverview } from '@/api/statistics'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const miniItems = ref([
  { value:0, label:'注册监督员' },
  { value:0, label:'反馈总数' },
  { value:0, label:'AQI检测数' },
  { value:0, label:'污染超标' }
])

const statusItems = ref([
  { value:0, label:'实时检测超标', cls:'red' },
  { value:0, label:'实时检测良好', cls:'green' },
  { value:0, label:'待处理', cls:'yellow' },
  { value:'0%', label:'在线网格员', cls:'blue' }
])

let stompClient = null

onMounted(async function() {
  try {
    var res = await getOverview()
    var d = (res && res.data) ? res.data : {}
    miniItems.value[0].value = d.totalUsers || 0
    miniItems.value[1].value = d.totalFeedbacks || 0
    miniItems.value[2].value = d.totalDetections || 0
    miniItems.value[3].value = d.pendingFeedbacks || 0
  } catch(e) { console.error('LeftPanel加载失败:', e) }

  // WebSocket 实时数据
  try {
    var s = new SockJS('/ws')
    stompClient = new Client({ webSocketFactory:function(){return s}, reconnectDelay:5000,
      onConnect:function(){
        stompClient.subscribe('/topic/dashboard', function(m) {
          try {
            var r = JSON.parse(m.body)
            statusItems.value[0].value = r.badDetections || 0
            statusItems.value[1].value = r.goodDetections || 0
            statusItems.value[2].value = r.pendingFeedback || 0
            statusItems.value[3].value = (r.gridCoverageByProvince || 0) + '%'
          } catch(e) {}
        })
      }
    })
    stompClient.activate()
  } catch(e) {}
})

onUnmounted(function() { if (stompClient) stompClient.deactivate() })
</script>

<style scoped>
.left-panel { position:absolute; bottom:20px; left:20px; width:260px; display:flex; flex-direction:column; gap:12px; z-index:20; }
.panel-card { background:rgba(15,30,54,0.8); backdrop-filter:blur(12px); border-radius:12px; padding:14px; border:1px solid rgba(64,144,188,0.2); box-shadow:0 4px 20px rgba(0,0,0,0.3); }
.card-header { margin-bottom:12px; }
.card-title { font-size:12px; font-weight:600; color:#8fa3b8; }
.mini-cards { display:grid; grid-template-columns:repeat(2,1fr); gap:8px; }
.mini-card { background:rgba(0,0,0,0.2); border-radius:8px; padding:10px; text-align:center; }
.mini-value { font-size:20px; font-weight:700; color:#fff; font-family:'SF Mono',monospace; }
.mini-label { font-size:10px; color:#8fa3b8; margin-top:4px; }
.status-items { display:grid; grid-template-columns:repeat(2,1fr); gap:10px; }
.status-item { display:flex; align-items:center; gap:8px; padding:8px 10px; background:rgba(0,0,0,0.2); border-radius:8px; }
.status-indicator { width:8px; height:8px; border-radius:50%; animation:pulse 2s infinite; }
.status-indicator.red { background:#f56c6c; box-shadow:0 0 10px rgba(245,108,108,0.5); }
.status-indicator.green { background:#2aa876; box-shadow:0 0 10px rgba(42,168,118,0.5); }
.status-indicator.yellow { background:#f5a623; box-shadow:0 0 10px rgba(245,166,35,0.5); }
.status-indicator.blue { background:#4fa3d1; box-shadow:0 0 10px rgba(79,163,209,0.5); }
@keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.7;transform:scale(1.1)} }
.status-value { font-size:14px; font-weight:700; color:#fff; font-family:'SF Mono',monospace; }
.status-label { font-size:10px; color:#8fa3b8; }
</style>
