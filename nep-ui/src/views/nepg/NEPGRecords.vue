<template>
  <div class="root">
    <!-- 顶栏 -->
    <div class="top">
      <div class="tl">
        <h1>历史检测记录</h1>
        <span class="cnt">{{ records.length }} 条归档记录</span>
      </div>
      <div class="tr">
        <div class="sbox">
          <span class="si">🔍</span>
          <input v-model="searchQuery" placeholder="搜索编号、案件或备注..." class="sinp" />
        </div>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="ov">
      <div class="oc">
        <span class="ol">累计检测总数</span>
        <span class="ov">{{ records.length }}</span>
      </div>
      <div class="oc">
        <span class="ol">历史平均 AQI</span>
        <span class="ov az">{{ avgAqi }}</span>
        <span class="os">基于 {{ records.length }} 条记录计算</span>
      </div>
      <div class="oc">
        <span class="ol">污染超标次数</span>
        <span class="ov ro">{{ highRiskCount }}</span>
        <span class="os">AQI > 100 的记录</span>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="tbl">
      <div class="th">
        <div class="ci">记录编号</div>
        <div class="ct">关联案件</div>
        <div class="cm">环境指标 (AQI)</div>
        <div class="cr">现场备注</div>
        <div class="ctm">归档时间</div>
        <div class="ca">最终AQI</div>
      </div>

      <div class="tb" v-loading="isLoading">
        <div v-if="!isLoading && filteredRecords.length === 0" class="empty">
          <p>暂无任何历史检测数据</p>
        </div>

        <div v-for="r in filteredRecords" :key="r.id" class="tr">
          <div class="ci"><span class="mono">REC-{{ r.id }}</span></div>
          <div class="ct"><span class="badge">📋 {{ r.feedbackId || '无关联工单' }}</span></div>
          <div class="cm">
            <div class="mp"><span class="ml">SO₂</span><span class="mv">{{ r.so2Aqi || 0 }}</span></div>
            <div class="mp"><span class="ml">CO</span><span class="mv">{{ r.coAqi || 0 }}</span></div>
            <div class="mp"><span class="ml">PM2.5</span><span class="mv">{{ r.pm25Aqi || 0 }}</span></div>
          </div>
          <div class="cr"><span class="rm">{{ r.remark || '暂无现场备注' }}</span></div>
          <div class="ctm"><span class="tm">{{ fmtTime(r.createTime) }}</span></div>
          <div class="ca">
            <span class="orb" :class="aqiLvl(r.finalAqi)">{{ r.finalAqi || 0 }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyAqiRecords } from '@/api/aqi'

const records = ref([])
const isLoading = ref(false)
const searchQuery = ref('')

const filteredRecords = computed(function() {
  if (!searchQuery.value) return records.value
  var q = searchQuery.value.toLowerCase()
  return records.value.filter(function(r) {
    return String(r.id).includes(q) ||
      String(r.feedbackId || '').includes(q) ||
      String(r.remark || '').toLowerCase().includes(q)
  })
})

const avgAqi = computed(function() {
  if (!records.value || records.value.length === 0) return 0
  var total = records.value.reduce(function(sum, r) { return sum + (parseInt(r.finalAqi) || 0) }, 0)
  return Math.round(total / records.value.length)
})

const highRiskCount = computed(function() {
  if (!records.value) return 0
  return records.value.filter(function(r) { return (parseInt(r.finalAqi) || 0) > 100 }).length
})

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 16)
}

function aqiLvl(val) {
  var v = parseInt(val) || 0
  if (v <= 50) return 'good'
  if (v <= 150) return 'warn'
  return 'bad'
}

async function fetchRecords() {
  var uid = localStorage.getItem('userId')
  if (!uid) return
  isLoading.value = true
  try {
    var res = await getMyAqiRecords()
    if (res && res.data) records.value = res.data
  } catch(e) { console.error('AQI记录加载失败:', e) }
  finally { isLoading.value = false }
}

onMounted(function() { fetchRecords() })
</script>

<style scoped>
.root { display: flex; flex-direction: column; height: 100%; overflow: hidden; padding: 24px; color: #0F172A; }

/* 顶栏 */
.top { display: flex; justify-content: space-between; align-items: flex-end; padding-bottom: 20px; border-bottom: 1px solid rgba(15,23,42,0.05); margin-bottom: 20px; flex-shrink: 0; }
.tl { display: flex; align-items: baseline; gap: 14px; }
.tl h1 { font-size: 22px; font-weight: 700; margin: 0; }
.cnt { font-size: 13px; color: #64748B; font-weight: 500; }
.tr { display: flex; align-items: center; gap: 12px; }
.sbox { display: flex; align-items: center; gap: 8px; background: #fff; border: 1px solid rgba(15,23,42,0.08); padding: 9px 16px; border-radius: 12px; width: 260px; }
.si { font-size: 14px; }
.sbox:focus-within { border-color: #0284C7; box-shadow: 0 0 0 3px rgba(2,132,199,0.08); }
.sinp { border: none; outline: none; font-size: 13px; width: 100%; color: #0F172A; background: transparent; }
.sinp::placeholder { color: #94A3B8; }

/* 概览 */
.ov { display: flex; gap: 20px; margin-bottom: 20px; flex-shrink: 0; }
.oc { flex: 1; background: #fff; border-radius: 14px; padding: 18px 22px; border: 1px solid rgba(15,23,42,0.04); display: flex; flex-direction: column; gap: 6px; }
.ol { font-size: 12px; color: #64748B; font-weight: 500; }
.ov { font-size: 26px; font-weight: 800; color: #0F172A; }
.ov.az { color: #0284C7; }
.ov.ro { color: #F43F5E; }
.os { font-size: 11px; color: #94A3B8; }

/* 表格 */
.tbl { flex: 1; background: #fff; border-radius: 16px; border: 1px solid rgba(15,23,42,0.04); display: flex; flex-direction: column; overflow: hidden; }
.th { display: grid; grid-template-columns: 1fr 1.5fr 2.5fr 2fr 1.5fr 1fr; align-items: center; gap: 14px; padding: 0 28px; height: 48px; flex-shrink: 0; border-bottom: 1px solid rgba(15,23,42,0.05); background: rgba(248,250,252,0.5); font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.3px; }
.tb { flex: 1; overflow-y: auto; min-height: 0; }
.tb::-webkit-scrollbar { width: 5px; }
.tb::-webkit-scrollbar-thumb { background: rgba(15,23,42,0.06); border-radius: 4px; }

/* 数据行 */
.tr { display: grid; grid-template-columns: 1fr 1.5fr 2.5fr 2fr 1.5fr 1fr; align-items: center; gap: 14px; padding: 0 28px; height: 66px; border-bottom: 1px solid rgba(15,23,42,0.03); transition: background 0.15s; }
.tr:hover { background: rgba(241,245,249,0.4); }
.tr:last-child { border-bottom: none; }
.mono { font-family: monospace; font-size: 12px; color: #475569; font-weight: 600; }
.badge { display: inline-flex; align-items: center; gap: 4px; background: #F1F5F9; color: #475569; font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: 6px; }

/* 指标 */
.cm { display: flex; gap: 10px; }
.mp { display: flex; align-items: center; background: #fff; border: 1px solid rgba(15,23,42,0.05); border-radius: 5px; overflow: hidden; }
.ml { background: #F8FAFC; color: #64748B; font-size: 10px; font-weight: 600; padding: 3px 5px; border-right: 1px solid rgba(15,23,42,0.05); }
.mv { font-size: 11px; font-weight: 700; color: #0F172A; padding: 3px 7px; font-family: monospace; }
.rm { font-size: 12px; color: #64748B; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; max-width: 90%; }
.tm { font-size: 12px; color: #94A3B8; }

/* AQI 圆标 */
.ca { display: flex; justify-content: flex-end; }
.orb { width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; font-family: monospace; }
.orb.good { background: #F0FDF4; color: #10B981; }
.orb.warn { background: #FFFBEB; color: #F59E0B; }
.orb.bad { background: #FEF2F2; color: #EF4444; }

.empty { height: 100%; display: flex; align-items: center; justify-content: center; color: #94A3B8; font-size: 13px; }
</style>
