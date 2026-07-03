<template>
  <div class="alpine-records-canvas">
    
    <header class="records-header">
      <div class="header-left">
        <div class="title-with-icon">
          <div class="glass-icon-box">
            <el-icon><DataLine /></el-icon>
          </div>
          <h1 class="page-title">{{ dynamicText.pageTitle }}</h1>
        </div>
        <span class="record-counter">{{ records.length }} {{ dynamicText.recordUnit }}</span>
      </div>
      
      <div class="header-right">
        <div class="alpine-search">
          <el-icon class="search-icon"><Search /></el-icon>
          <input 
            type="text" 
            v-model="searchQuery" 
            :placeholder="dynamicText.searchPlaceholder"
            class="search-input"
          >
        </div>
        <button class="alpine-btn icon-only">
          <el-icon><Filter /></el-icon>
        </button>
      </div>
    </header>

    <div class="overview-row">
      <div class="overview-card">
        <span class="overview-label">{{ dynamicText.statTotal }}</span>
        <span class="overview-value">{{ records.length }}</span>
      </div>
      <div class="overview-card">
        <span class="overview-label">{{ dynamicText.statAvgAqi }}</span>
        <span class="overview-value text-azure">{{ avgAqi }}</span>
      </div>
      <div class="overview-card">
        <span class="overview-label">{{ dynamicText.statHighRisk }}</span>
        <span class="overview-value text-rose">{{ highRiskCount }}</span>
      </div>
    </div>

    <main class="ledger-workspace">
      
      <div class="ledger-row is-header">
        <div class="col-id">{{ dynamicText.colId }}</div>
        <div class="col-task">{{ dynamicText.colTask }}</div>
        <div class="col-metrics">{{ dynamicText.colMetrics }}</div>
        <div class="col-remark">{{ dynamicText.colRemark }}</div>
        <div class="col-time">{{ dynamicText.colTime }}</div>
        <div class="col-aqi">{{ dynamicText.colAqi }}</div>
      </div>

      <div class="ledger-scroll-area" v-loading="isLoading">
        
        <div v-if="filteredRecords.length === dynamicText.zeroCount && !isLoading" class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <p>{{ dynamicText.emptyDesc }}</p>
        </div>

        <div 
          v-else
          v-for="record in filteredRecords" 
          :key="record.id" 
          class="ledger-row is-data"
        >
          <div class="col-id">
            <span class="mono-text">{{ dynamicText.idPrefix }}{{ record.id }}</span>
          </div>
          
          <div class="col-task">
            <span class="task-badge">
              <el-icon><Monitor /></el-icon> {{ record.feedbackId || dynamicText.unboundTask }}
            </span>
          </div>
          
          <div class="col-metrics">
            <div class="metric-pill">
              <span class="m-label">SO₂</span><span class="m-val">{{ record.so2Aqi || 0 }}</span>
            </div>
            <div class="metric-pill">
              <span class="m-label">CO</span><span class="m-val">{{ record.coAqi || 0 }}</span>
            </div>
            <div class="metric-pill">
              <span class="m-label">PM2.5</span><span class="m-val">{{ record.pm25Aqi || 0 }}</span>
            </div>
          </div>
          
          <div class="col-remark">
            <span class="remark-text">{{ record.remark || dynamicText.noRemark }}</span>
          </div>
          
          <div class="col-time">
            <span class="time-text">{{ formatTime(record.createTime) }}</span>
          </div>
          
          <div class="col-aqi">
            <div class="aqi-orb" :class="getAqiLevel(record.finalAqi)">
              {{ record.finalAqi }}
            </div>
          </div>
        </div>
        
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyAqiRecords } from '@/api/aqi'
import { DataLine, Search, Filter, Document, Monitor } from '@element-plus/icons-vue'

// ==========================================
// 1. 全局字典与文案（彻底消除模板硬编码）
// ==========================================
const dynamicText = ref({
  pageTitle: '历史检测记录',
  recordUnit: '条归档记录',
  searchPlaceholder: '搜索编号、案件或备注...',
  statTotal: '累计检测总数',
  statAvgAqi: '历史平均 AQI',
  statHighRisk: '污染超标次数',
  colId: '记录编号',
  colTask: '关联案件',
  colMetrics: '环境指标监测 (AQI)',
  colRemark: '现场备注',
  colTime: '归档时间',
  colAqi: '最终判定',
  idPrefix: 'REC-',
  unboundTask: '无关联工单',
  noRemark: '暂无现场备注说明',
  emptyDesc: '暂无任何历史检测数据',
  zeroCount: 0
})

// ==========================================
// 2. 状态与数据模型
// ==========================================
const records = ref([])
const isLoading = ref(false)
const searchQuery = ref('')

// ==========================================
// 3. 计算属性 (统计与过滤)
// ==========================================
const filteredRecords = computed(() => {
  if (!searchQuery.value) return records.value
  const q = searchQuery.value.toLowerCase()
  return records.value.filter(r => 
    String(r.id).includes(q) || 
    String(r.feedbackId || '').includes(q) || 
    String(r.remark || '').toLowerCase().includes(q)
  )
})

const avgAqi = computed(() => {
  if (records.value.length === 0) return 0
  const total = records.value.reduce((sum, r) => sum + (r.finalAqi || 0), 0)
  return Math.round(total / records.value.length)
})

const highRiskCount = computed(() => {
  return records.value.filter(r => (r.finalAqi || 0) > 100).length
})

// ==========================================
// 4. 辅助函数
// ==========================================
const formatTime = (t) => {
  if (!t) return dynamicText.value.noRemark
  return t.replace('T', ' ').substring(0, 16)
}

const getAqiLevel = (val) => {
  const v = Number(val) || 0
  if (v <= 50) return 'excellent'
  if (v <= 150) return 'warning'
  return 'danger'
}

// ==========================================
// 5. 生命周期与数据获取
// ==========================================
const fetchRecords = async () => {
  const uid = localStorage.getItem('userId')
  if (!uid) return
  
  isLoading.value = true
  try {
    const res = await getMyAqiRecords()
    if (res && res.data) {
      records.value = res.data
    }
  } catch (e) {
    // 生产环境中可通过 ElMessage 提示错误
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
/* =======================================================
   1. 顶级画布约束 (Strict Canvas) - 杜绝外层滚动
======================================================= */
.alpine-records-canvas {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

/* =======================================================
   2. 顶部控制台 (Header Console)
======================================================= */
.records-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
  margin-bottom: 24px;
}
.header-left { display: flex; align-items: baseline; gap: 16px; }
.title-with-icon { display: flex; align-items: center; gap: 12px; }
.glass-icon-box {
  width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  color: #0284C7; display: flex; align-items: center; justify-content: center;
  font-size: 20px; box-shadow: 0 4px 12px rgba(2, 132, 199, 0.1);
}
.page-title { font-size: 24px; font-weight: 700; color: #0F172A; margin: 0; letter-spacing: 0.5px; }
.record-counter { font-size: 14px; color: #64748B; font-weight: 500; }

.header-right { display: flex; align-items: center; gap: 16px; }

/* 搜索框与按钮 */
.alpine-search {
  display: flex; align-items: center; gap: 8px;
  background: white; border: 1px solid rgba(15, 23, 42, 0.08);
  padding: 10px 16px; border-radius: 14px; width: 280px;
  transition: all 0.3s;
}
.alpine-search:focus-within { border-color: #0284C7; box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1); }
.search-icon { color: #94A3B8; font-size: 16px; }
.search-input { border: none; outline: none; font-size: 14px; width: 100%; color: #0F172A; background: transparent; }
.search-input::placeholder { color: #94A3B8; }

.alpine-btn.icon-only {
  width: 42px; height: 42px; border-radius: 14px;
  background: white; border: 1px solid rgba(15, 23, 42, 0.08);
  color: #64748B; display: flex; align-items: center; justify-content: center;
  font-size: 18px; cursor: pointer; transition: all 0.3s;
}
.alpine-btn.icon-only:hover { border-color: #0284C7; color: #0284C7; }

/* =======================================================
   3. 顶部数据概览 (Overview Mini-Bento)
======================================================= */
.overview-row {
  flex-shrink: 0;
  display: flex; gap: 24px; margin-bottom: 24px;
}
.overview-card {
  flex: 1; background: white; border-radius: 16px; padding: 20px 24px;
  border: 1px solid rgba(15, 23, 42, 0.03);
  box-shadow: 0 4px 16px -8px rgba(15, 23, 42, 0.04);
  display: flex; flex-direction: column; gap: 8px;
}
.overview-label { font-size: 13px; color: #64748B; font-weight: 500; }
.overview-value { font-size: 28px; font-weight: 800; color: #0F172A; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace; }
.text-azure { color: #0284C7; }
.text-rose { color: #F43F5E; }

/* =======================================================
   4. 主数据账本 (Ledger Workspace) - 自适应填满
======================================================= */
.ledger-workspace {
  flex: 1;
  background: white;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.04);
  box-shadow: 0 4px 24px -8px rgba(15, 23, 42, 0.03);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 统一列宽配比 (CSS Grid) */
.ledger-row {
  display: grid;
  grid-template-columns: 1fr 1.5fr 2.5fr 2fr 1.5fr 1fr;
  align-items: center;
  gap: 16px;
  padding: 0 32px;
}

/* 表头行 */
.is-header {
  flex-shrink: 0; height: 56px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(248, 250, 252, 0.5);
  font-size: 12px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.5px;
}

/* 数据滚动区 */
.ledger-scroll-area {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
/* macOS 风格内滚动条 */
.ledger-scroll-area::-webkit-scrollbar { width: 6px; }
.ledger-scroll-area::-webkit-scrollbar-thumb { background: rgba(15, 23, 42, 0.08); border-radius: 6px; }
.ledger-scroll-area::-webkit-scrollbar-thumb:hover { background: rgba(15, 23, 42, 0.15); }

/* 数据行 */
.is-data {
  height: 72px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.03);
  transition: background 0.2s;
}
.is-data:hover { background: rgba(241, 245, 249, 0.4); }
.is-data:last-child { border-bottom: none; }

/* =======================================================
   5. 列内容定制化样式 (Cell Styles)
======================================================= */
.mono-text { font-family: monospace; font-size: 13px; color: #475569; font-weight: 600; }

.task-badge {
  display: inline-flex; align-items: center; gap: 6px;
  background: #F1F5F9; color: #475569; font-size: 12px; font-weight: 600;
  padding: 4px 10px; border-radius: 8px;
}

.col-metrics { display: flex; gap: 12px; }
.metric-pill {
  display: flex; align-items: center;
  background: white; border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 6px; overflow: hidden;
}
.m-label { background: #F8FAFC; color: #64748B; font-size: 10px; font-weight: 600; padding: 4px 6px; border-right: 1px solid rgba(15, 23, 42, 0.06); }
.m-val { font-size: 12px; font-weight: 700; color: #0F172A; padding: 4px 8px; font-family: monospace; }

.remark-text { font-size: 13px; color: #64748B; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; max-width: 90%; }

.time-text { font-size: 13px; color: #94A3B8; }

/* 最终 AQI 纯净圆环 */
.col-aqi { display: flex; justify-content: flex-end; }
.aqi-orb {
  width: 36px; height: 36px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 800; font-family: monospace;
}
.aqi-orb.excellent { background: #F0FDF4; color: #10B981; border: 1px solid rgba(16, 185, 129, 0.2); }
.aqi-orb.warning { background: #FFFBEB; color: #F59E0B; border: 1px solid rgba(245, 158, 11, 0.2); }
.aqi-orb.danger { background: #FEF2F2; color: #EF4444; border: 1px solid rgba(239, 68, 68, 0.2); }

/* 空状态 */
.empty-state {
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #94A3B8; font-size: 14px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; color: #E2E8F0; }
</style>