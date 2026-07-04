<template>
  <div class="dash">
    <div class="grid">
      <!-- === 英雄看板 === -->
      <div class="card hero">
        <div class="hl">
          <h2 class="h2">{{ greetingText }}</h2>
          <p class="hp">{{ dynamicContent.heroSubtitle }}</p>
          <div class="hbtns">
            <button class="hb primary" @click="router.push(dynamicContent.routeTasks)">
              <span>🔍</span> {{ dynamicContent.btnInspectText }}
            </button>
            <button class="hb outline" @click="router.push(dynamicContent.routeRecords)">
              <span>📋</span> {{ dynamicContent.btnRecordText }}
            </button>
          </div>
        </div>
        <div class="hr">
          <div class="orb">
            <div class="orb-ring r1"></div>
            <div class="orb-ring r2"></div>
            <div class="orb-ring r3"></div>
            <div class="orb-inner">
              <span class="orb-v">{{ aqiData.value }}</span>
              <span class="orb-l">{{ aqiLabelText }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- === 指标列 === -->
      <div class="mcol">
        <!-- 待办紧急任务 -->
        <div class="mcard urgent">
          <div class="urgent-shimmer"></div>
          <div class="urgent-scan"></div>
          <div class="urgent-ring"></div>
          <div class="mhead">
            <span class="micon urg">!</span>
            <button class="mbtn" @click="router.push(dynamicContent.routeTasks)">↗</button>
          </div>
          <div class="mbody">
            <span class="mval">{{ anim.pending }}</span>
            <span class="mtit">{{ dynamicContent.metric1Title }}</span>
          </div>
        </div>

        <!-- 本周已完成 -->
        <div class="mcard done">
          <div class="done-sparkles">
            <span class="ds s1">✦</span>
            <span class="ds s2">✦</span>
            <span class="ds s3">✦</span>
          </div>
          <div class="mhead">
            <span class="micon don">✓</span>
          </div>
          <div class="mbody">
            <span class="mval grn">{{ anim.completed }}</span>
            <span class="mtit">{{ dynamicContent.metric2Title }}</span>
          </div>
          <svg class="done-ring" viewBox="0 0 60 60">
            <circle cx="30" cy="30" r="26" fill="none" stroke="rgba(16,185,129,0.1)" stroke-width="3"/>
            <circle cx="30" cy="30" r="26" fill="none" stroke="#10B981" stroke-width="3"
              stroke-dasharray="163" :stroke-dashoffset="163 - (163 * Math.min(metricsData.completed / Math.max(metricsData.pending + metricsData.completed, 1), 1))"
              stroke-linecap="round" transform="rotate(-90 30 30)" class="done-ring-fill"/>
          </svg>
        </div>
      </div>

      <!-- === 焦点任务 === -->
      <div class="card focus">
        <div class="ch">
          <h3>{{ dynamicContent.focusListTitle }}</h3>
          <button class="tbtn" @click="router.push(dynamicContent.routeTasks)">
            {{ dynamicContent.focusListBtnText }} →
          </button>
        </div>
        <div class="cb" v-if="displayTasks.length === dynamicContent.zeroCount">
          <div class="empty">☕<br/>{{ dynamicContent.emptyTaskText }}</div>
        </div>
        <div class="cb flist" v-else>
          <div v-for="task in displayTasks" :key="task.id" class="fitem">
            <div class="fi-left" :class="task.levelCode">
              <span class="fi-icon">{{ task.levelCode === 'danger' ? '!' : task.levelCode === 'warning' ? '!' : 'i' }}</span>
            </div>
            <div class="finfo">
              <span class="ft">{{ task.title }}</span>
              <span class="fd">📍 {{ task.address }}</span>
            </div>
            <span class="ftag" :class="task.levelCode">{{ task.levelText }}</span>
          </div>
        </div>
      </div>

      <!-- === 系统信标 === -->
      <div class="card beacon">
        <div class="ch">
          <h3>{{ dynamicContent.beaconTitle }}</h3>
        </div>
        <div class="cb blist">
          <div v-for="msg in displayBeacons" :key="msg.id" class="bitem">
            <span class="bdot" :class="msg.typeCode"></span>
            <div class="binfo">
              <span class="bt">{{ msg.content }}</span>
              <span class="bts">{{ msg.dateTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAssignedToMe, getAssignedStats } from '@/api/feedback'
import { getMyAqiRecords } from '@/api/aqi'

const router = useRouter()
const userStore = useUserStore()

const dynamicContent = ref({
  greetingPrefix: '早安', defaultUserName: '专员', punctuationComma: '，',
  heroSubtitle: '今日各项指标平稳，请继续保持。您可以直接开始巡查，或查阅数据简报。',
  btnInspectText: '开始巡查', btnRecordText: '巡查记录',
  aqiLabelPrefix: 'AQI', metric1Title: '待办紧急任务', metric2Title: '本周已完成',
  focusListTitle: '焦点任务', focusListBtnText: '查看全部',
  beaconTitle: '系统信标', emptyTaskText: '当前没有紧急任务，喝杯咖啡休息一下吧',
  routeTasks: '/nepg/tasks', routeRecords: '/nepg/records',
  zeroCount: 0, sliceLimit: 3
})

const userName = computed(function() { return userStore.user?.realName || dynamicContent.value.defaultUserName })
const greetingText = computed(function() { return dynamicContent.value.greetingPrefix + dynamicContent.value.punctuationComma + userName.value })
const aqiLabelText = computed(function() { return dynamicContent.value.aqiLabelPrefix + ' ' + aqiData.value.status })

const aqiData = ref({ value: 0, status: '--' })
const metricsData = ref({ pending: 0, completed: 0 })
const anim = ref({ pending: 0, completed: 0 })

// AQI 数值 → 状态文字
function aqiStatus(val) {
  if (!val && val !== 0) return '--'
  if (val <= 50) return '优'
  if (val <= 100) return '良'
  if (val <= 150) return '轻度污染'
  if (val <= 200) return '中度污染'
  if (val <= 300) return '重度污染'
  return '严重污染'
}

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

const displayTasks = computed(function() { return rawTasks.value.slice(0, dynamicContent.value.sliceLimit) })
const displayBeacons = computed(function() { return rawBeacons.value.slice(0, dynamicContent.value.sliceLimit) })

function updateGreeting() {
  var h = new Date().getHours()
  if (h < 12) dynamicContent.value.greetingPrefix = '早安'
  else if (h < 18) dynamicContent.value.greetingPrefix = '下午好'
  else dynamicContent.value.greetingPrefix = '晚上好'
}

function animateCount(targets) {
  var duration = 1200, startTime = Date.now()
  function step() {
    var elapsed = Date.now() - startTime
    var progress = Math.min(elapsed / duration, 1)
    var ep = 1 - Math.pow(1 - progress, 3)
    anim.value.pending = Math.round(targets.pending * ep)
    anim.value.completed = Math.round(targets.completed * ep)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

onMounted(async function() {
  if (!userStore.user) userStore.fetchUser()
  updateGreeting()

  // 加载 AQI 数据（从数据库实时读取）
  try {
    var aqiRes = await getMyAqiRecords()
    var aqiList = (aqiRes && aqiRes.data) ? aqiRes.data : []
    if (aqiList.length > 0) {
      // 计算平均 AQI
      var sumAqi = aqiList.reduce(function(s, r) { return s + (parseInt(r.finalAqi) || 0) }, 0)
      var avgVal = Math.round(sumAqi / aqiList.length)
      aqiData.value = { value: avgVal, status: aqiStatus(avgVal) }
    }
  } catch(e) { console.error('AQI数据加载失败:', e) }

  // 加载任务统计数据
  var targets = { pending: 0, completed: 0 }
  try {
    var sr = await getAssignedStats()
    var d = sr.data || {}
    targets.pending = d.assigned || 0; targets.completed = d.completed || 0
    metricsData.value = { pending: targets.pending, completed: targets.completed }
  } catch(e) {}
  try {
    var tr = await getAssignedToMe('ASSIGNED')
    var list = tr.data || []
    var lm = { 1:{code:'info',text:'常规'}, 2:{code:'info',text:'常规'}, 3:{code:'warning',text:'中优'}, 4:{code:'warning',text:'中优'}, 5:{code:'danger',text:'高优'}, 6:{code:'danger',text:'高优'} }
    rawTasks.value = list.map(function(t) { return {
      id: t.id,
      title: t.description || ('AQI等级' + (t.estimatedAqiLevel || '?') + '现场检测'),
      address: t.specificAddress || '未指定地点',
      levelCode: lm[t.estimatedAqiLevel]?.code || 'info',
      levelText: lm[t.estimatedAqiLevel]?.text || '常规'
    }})
  } catch(e) {}
  animateCount(targets)
})
</script>

<style scoped>
.dash { width: 100%; height: 100%; overflow: hidden; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(2, 1fr); height: 100%; gap: 20px; }

/* === 通用卡片 === */
.card {
  background: #fff; border-radius: 20px; border: 1px solid rgba(15,23,42,0.04);
  box-shadow: 0 4px 20px -8px rgba(15,23,42,0.05);
  padding: 22px 24px; display: flex; flex-direction: column;
  height: 100%; box-sizing: border-box; overflow: hidden;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.35s ease;
}
.card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 16px 40px -8px rgba(15,23,42,0.12);
  z-index: 5;
}
.ch { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-shrink: 0; }
.ch h3 { font-size: 15px; font-weight: 600; color: #0F172A; margin: 0; }
.tbtn { background: none; border: none; font-size: 12px; font-weight: 500; color: #64748B; cursor: pointer; }
.cb { flex: 1; overflow: hidden; min-height: 0; }

/* === 英雄看板 === */
.hero { grid-column: span 2; background: linear-gradient(135deg, #F0F9FF, #E0F2FE); border: 1px solid rgba(14,165,233,0.1); flex-direction: row; justify-content: space-between; align-items: center; padding: 28px 36px; }
.hero:hover { transform: translateY(-4px) scale(1.01); }
.hl { flex: 1; max-width: 65%; }
.h2 { font-size: 34px; font-weight: 800; color: #0F172A; margin: 0 0 10px; letter-spacing: 0.5px; }
.hp { font-size: 15px; color: #475569; margin: 0 0 24px; line-height: 1.5; }
.hbtns { display: flex; gap: 14px; }
.hb { padding: 12px 26px; border-radius: 14px; font-size: 15px; font-weight: 600; border: none; cursor: pointer; display: flex; align-items: center; gap: 6px; }
.hb.primary { background: #0284C7; color: #fff; box-shadow: 0 4px 12px rgba(2,132,199,0.25); }
.hb.outline { background: #fff; color: #0F172A; border: 1px solid rgba(15,23,42,0.1); }
.hr { flex-shrink: 0; }
.orb {
  position: relative;
  width: 150px; height: 150px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
/* 脉冲光环 */
.orb-ring {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
  border-radius: 50%; border: 2px solid rgba(2,132,199,0.3);
  animation: orb-pulse 3s infinite;
}
.r1 { width: 130px; height: 130px; animation-delay: 0s; }
.r2 { width: 150px; height: 150px; animation-delay: 1s; }
.r3 { width: 170px; height: 170px; animation-delay: 2s; }
@keyframes orb-pulse {
  0% { transform: translate(-50%,-50%) scale(0.85); opacity: 0.6; }
  50% { transform: translate(-50%,-50%) scale(1.1); opacity: 0.2; }
  100% { transform: translate(-50%,-50%) scale(0.85); opacity: 0.6; }
}
/* 内核 */
.orb-inner {
  position: relative; z-index: 1;
  width: 110px; height: 110px; border-radius: 50%;
  background: rgba(255,255,255,0.4); backdrop-filter: blur(12px);
  border: 2px solid rgba(255,255,255,0.8);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  box-shadow: 0 0 30px rgba(2,132,199,0.2), 0 12px 32px rgba(14,165,233,0.15);
  animation: orb-glow 2s infinite alternate;
}
@keyframes orb-glow {
  0% { box-shadow: 0 0 20px rgba(2,132,199,0.15), 0 8px 24px rgba(14,165,233,0.1); }
  100% { box-shadow: 0 0 40px rgba(2,132,199,0.35), 0 16px 40px rgba(14,165,233,0.25); }
}
.orb-v {
  font-size: 48px; font-weight: 800; color: #0284C7;
  line-height: 1;
  animation: val-pop 0.5s ease-out;
}
@keyframes val-pop {
  0% { transform: scale(0.5); opacity: 0; }
  70% { transform: scale(1.15); }
  100% { transform: scale(1); opacity: 1; }
}
.orb-l {
  font-size: 13px; font-weight: 600; color: #10B981;
  margin-top: 2px;
}

/* === 指标列 === */
.mcol { display: flex; flex-direction: column; gap: 20px; height: 100%; }

/* 指标卡片 */
.mcard {
  flex: 1; background: #fff; border-radius: 18px; border: 1px solid rgba(15,23,42,0.04);
  padding: 20px; display: flex; flex-direction: column; justify-content: center;
  box-shadow: 0 4px 20px -8px rgba(15,23,42,0.05);
  position: relative; overflow: hidden;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.35s ease;
  cursor: pointer;
}
.mcard:hover {
  transform: translateY(-6px) scale(1.04);
  box-shadow: 0 18px 40px -10px rgba(15,23,42,0.15);
  z-index: 5;
}

/* 紧急卡片 */
.mcard.urgent { border-left: 3px solid #EF4444; background: linear-gradient(135deg, #FFF5F5, #FFF); }
.urgent-shimmer {
  position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.5) 45%, rgba(255,255,255,0.7) 50%, rgba(255,255,255,0.5) 55%, transparent 60%);
  animation: urg-shimmer 3s infinite; z-index: 1;
}
@keyframes urg-shimmer { 0% { transform: translateX(-100%) rotate(25deg); } 100% { transform: translateX(100%) rotate(25deg); } }
.urgent-scan {
  position: absolute; left: 0; width: 100%; height: 2px;
  background: rgba(239,68,68,0.4); z-index: 1;
  animation: urg-scan 2s infinite;
}
@keyframes urg-scan { 0% { top: -2px; opacity: 0; } 30% { opacity: 1; } 100% { top: 100%; opacity: 0; } }
.urgent-ring {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
  width: 0; height: 0; border-radius: 50%;
  border: 2px solid rgba(239,68,68,0.3);
  animation: urg-ring 2s infinite; z-index: 1;
}
@keyframes urg-ring { 0% { width: 30px; height: 30px; opacity: 0.5; } 100% { width: 180px; height: 180px; opacity: 0; } }

/* 已完成卡片 */
.mcard.done { border-left: 3px solid #10B981; background: linear-gradient(135deg, #F5FFFA, #FFF); }
.done-sparkles { position: absolute; inset: 0; pointer-events: none; z-index: 3; }
.ds { position: absolute; font-size: 13px; color: #10B981; animation: dsp 2s infinite; }
.s1 { top: 6px; right: 16px; animation-delay: 0s; }
.s2 { top: 16px; right: 6px; animation-delay: 0.7s; font-size: 9px; }
.s3 { bottom: 8px; right: 20px; animation-delay: 1.4s; font-size: 10px; }
@keyframes dsp { 0%,100% { opacity: 0; transform: translateY(0) scale(0.5); } 50% { opacity: 1; transform: translateY(-6px) scale(1.2); } }

.done-ring { position: absolute; top: 50%; right: 16px; transform: translateY(-50%); width: 40px; height: 40px; }
.done-ring-fill { transition: stroke-dashoffset 1.2s cubic-bezier(0.4,0,0.2,1); }

.mhead { display: flex; justify-content: space-between; align-items: center; margin-bottom: auto; position: relative; z-index: 2; }
.micon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 800; }
.micon.urg { background: #FEF2F2; color: #EF4444; animation: ib 0.6s ease-out; }
.micon.don { background: #F0FDF4; color: #10B981; animation: ib 0.5s ease-out; }
@keyframes ib { 0% { transform: scale(0) rotate(-20deg); } 60% { transform: scale(1.3) rotate(5deg); } 100% { transform: scale(1) rotate(0); } }
.mbtn { background: none; border: none; color: #94A3B8; cursor: pointer; font-size: 15px; padding: 4px; position: relative; z-index: 2; }

.mbody { position: relative; z-index: 2; margin-top: auto; }
.mval { font-size: 26px; font-weight: 700; color: #0F172A; display: block; }
.mval.grn { color: #10B981; }
.mtit { font-size: 12px; color: #64748B; font-weight: 500; }

/* === 焦点任务 === */
.focus { grid-column: span 2; }
.flist { display: flex; flex-direction: column; gap: 10px; }

.fitem {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border-radius: 14px;
  background: #fff; border: 1px solid rgba(15,23,42,0.04);
  flex: 1; min-height: 0;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
}
.fitem:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 8px 24px -6px rgba(15,23,42,0.08);
  border-color: rgba(2,132,199,0.15);
}

/* 左侧图标 */
.fi-left {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: #F1F5F9; color: #64748B;
  font-size: 16px; font-weight: 800;
  animation: fi-in 0.4s ease-out;
}
.fi-left.danger { background: #FEF2F2; color: #EF4444; box-shadow: 0 0 12px rgba(239,68,68,0.15); }
.fi-left.warning { background: #FFFBEB; color: #F59E0B; box-shadow: 0 0 12px rgba(245,158,11,0.15); }
.fi-left.info { background: #F1F5F9; color: #64748B; }
@keyframes fi-in { 0% { transform: scale(0); } 60% { transform: scale(1.2); } 100% { transform: scale(1); } }

.finfo { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.ft {
  font-size: 13px; font-weight: 600; color: #0F172A;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.fd {
  font-size: 11px; color: #94A3B8;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* 优先级标签 */
.ftag {
  padding: 4px 10px; border-radius: 8px;
  font-size: 10px; font-weight: 700; flex-shrink: 0;
  letter-spacing: 0.5px;
}
.ftag.danger { background: #FEF2F2; color: #EF4444; animation: tag-pulse 2s infinite; }
.ftag.warning { background: #FFFBEB; color: #F59E0B; }
.ftag.info { background: #F1F5F9; color: #64748B; }
@keyframes tag-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.7; } }

/* === 系统信标 === */
.blist { display: flex; flex-direction: column; justify-content: space-between; padding-left: 4px; }
.bitem { display: flex; gap: 10px; flex-shrink: 1; min-height: 0; }
.bdot { width: 7px; height: 7px; border-radius: 50%; margin-top: 4px; flex-shrink: 0; box-shadow: 0 0 0 3px #fff; }
.bdot.primary { background: #0284C7; }
.bdot.warning { background: #F59E0B; }
.bdot.success { background: #10B981; }
.binfo { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.bt { font-size: 12px; color: #334155; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.bts { font-size: 10px; color: #94A3B8; }

.empty { flex: 1; display: flex; align-items: center; justify-content: center; text-align: center; color: #94A3B8; font-size: 13px; line-height: 1.8; }
</style>
