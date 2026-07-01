<template>
  <div class="swiss-dashboard">

    <section class="action-console glass-panel fixed-section">
      <div class="console-left">
        <button class="icon-nav-btn" @click="$router.push('/ne/feedbacks')" title="返回列表">
          <el-icon>
            <Back />
          </el-icon>
        </button>
        <div class="title-group">
          <h2 class="page-title">提报新案件</h2>
          <span class="page-subtitle">录入环境问题信标与现场勘测档案</span>
        </div>
      </div>

      <div class="console-right">
        <button class="swiss-btn ghost-btn" @click="$router.push('/ne/feedbacks')">取消</button>
        <button class="swiss-btn primary-btn" @click="handleSubmit" :class="{ 'is-loading': submitting }">
          <el-icon v-if="!submitting"><Select /></el-icon>
          <el-icon v-else class="is-spinning">
            <Loading />
          </el-icon>
          加密提报档案
        </button>
      </div>
    </section>

    <section class="content-split-grid stretch-section form-split">

      <div class="form-panel glass-panel scrollable-card">
        <div class="panel-header">
          <h3 class="panel-title"><el-icon>
              <Location />
            </el-icon> 空间信标与勘测</h3>
        </div>

        <div class="panel-body scroll-area">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="mac-inset-form">
            <div class="input-row">
              <el-form-item label="所在地区" prop="provinceId" class="flex-1">
                <el-select v-model="form.provinceId" placeholder="省 / 自治区 / 直辖市" @change="onProvinceChange"
                  popper-class="swiss-select-dropdown" style="width: 100%">
                  <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </el-form-item>

              <el-form-item :label="cityLabel" prop="cityId" class="flex-1">
                <el-select v-model="form.cityId" :placeholder="cityPlaceholder" popper-class="swiss-select-dropdown"
                  style="width: 100%">
                  <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="精确物理坐标" prop="specificAddress">
              <el-input v-model="form.specificAddress" placeholder="例如：玄武区中山路 188 号交叉口向东 50 米" type="textarea" :rows="3"
                class="inset-input" />
            </el-form-item>

            <el-form-item label="现场勘测详述" prop="description" class="margin-bottom-0">
              <el-input v-model="form.description" placeholder="请详细描述您观察到的空气异常情况、气味、颜色或任何其他相关环境细节..." type="textarea"
                :rows="12" class="inset-input" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="ref-panel glass-panel scrollable-card">
        <div class="panel-header">
          <h3 class="panel-title"><el-icon>
              <DataLine />
            </el-icon> 污染等级评估中枢</h3>
          <span class="required-asterisk">* 必填项</span>
        </div>

        <div class="panel-body scroll-area">
          <div class="aqi-sensory-grid">
            <div v-for="item in aqiReference" :key="item.levelVal" class="aqi-tile"
              :class="{ 'is-selected': form.estimatedAqiLevel === item.levelVal }"
              @click="selectAqiLevel(item.levelVal)">
              <div class="tile-glow" :style="{ backgroundColor: item.color }"></div>
              <div class="tile-content">
                <span class="level-badge" :style="{ backgroundColor: item.color }">级 {{ item.levelVal }}</span>
                <span class="level-desc">{{ item.category }}</span>
              </div>
            </div>
          </div>

          <el-form-item prop="estimatedAqiLevel" style="margin:0; height:0; overflow:hidden;" :model="form"
            :rules="rules" ref="aqiFormRef">
            <el-input v-model="form.estimatedAqiLevel" />
          </el-form-item>

          <div class="reference-section">
            <h4 class="section-sub-title"><el-icon>
                <Reading />
              </el-icon> AQI 指数规范参考表</h4>
            <div class="reference-table-wrapper">
              <table class="premium-ref-table">
                <thead>
                  <tr>
                    <th>等级</th>
                    <th>指数范围</th>
                    <th>污染类别</th>
                    <th>健康指导建议</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in aqiReference" :key="item.levelVal"
                    :class="{ 'is-active-row': form.estimatedAqiLevel === item.levelVal }">
                    <td class="font-mono">级 {{ item.levelVal }}</td>
                    <td class="font-mono text-secondary">{{ item.range }}</td>
                    <td>
                      <span class="ref-badge"
                        :style="{ backgroundColor: item.color, color: item.levelVal > 3 ? '#FFF' : '#1C2421' }">
                        {{ item.category }}
                      </span>
                    </td>
                    <td class="text-hint">{{ item.suggestion }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

        </div>
      </div>

    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { submitFeedback } from '@/api/feedback'
import { getProvinces, getCitiesByProvince } from '@/api/region'
import { ElMessage } from 'element-plus'
import { Back, Select, Loading, Location, DataLine, Reading } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const aqiFormRef = ref(null)
const submitting = ref(false)
const provinces = ref([])
const cities = ref([])

// 直辖市ID：北京1 天津2 上海8 重庆21 → 标签改为"区县"
const isMunicipality = computed(() => [1, 2, 8, 21].includes(form.value.provinceId))
const cityLabel = computed(() => isMunicipality.value ? '所属区县' : '所属城市')
const cityPlaceholder = computed(() => isMunicipality.value ? '选择区县' : '选择城市')

const form = ref({
  provinceId: null,
  cityId: null,
  specificAddress: '',
  longitude: null,
  latitude: null,
  estimatedAqiLevel: null,
  description: '',
  supervisorId: Number(localStorage.getItem('userId') || 0)
})

const rules = {
  provinceId: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择城市或区县', trigger: 'change' }],
  specificAddress: [{ required: true, message: '物理坐标描述不可为空', trigger: 'blur' }],
  estimatedAqiLevel: [{ required: true, message: '请在上方拨盘选择 AQI 等级', trigger: 'change' }],
  description: [{ required: true, message: '现场勘测描述不可为空', trigger: 'blur' }]
}

const aqiReference = [
  { levelVal: 1, range: '0~50', category: '优', color: '#2AA876', suggestion: '各类人群可正常活动' },
  { levelVal: 2, range: '51~100', category: '良', color: '#85C77A', suggestion: '异常敏感人群应减少户外活动' },
  { levelVal: 3, range: '101~150', category: '轻度污染', color: '#F5A623', suggestion: '儿童、老人应减少户外锻炼' },
  { levelVal: 4, range: '151~200', category: '中度污染', color: '#E87A31', suggestion: '一般人群减少户外活动' },
  { levelVal: 5, range: '201~300', category: '重度污染', color: '#D9534F', suggestion: '一般人群应避免户外活动' },
  { levelVal: 6, range: '>300', category: '严重污染', color: '#A03232', suggestion: '所有人应避免户外活动' }
]

function selectAqiLevel(val) {
  form.value.estimatedAqiLevel = val
  nextTick(() => {
    if (aqiFormRef.value) aqiFormRef.value.clearValidate()
  })
}

async function onProvinceChange(provinceId) {
  form.value.cityId = null
  cities.value = []
  if (provinceId) {
    const res = await getCitiesByProvince(provinceId)
    cities.value = res.data
  }
}

async function handleSubmit() {
  let isAqiValid = false
  let isMainFormValid = false

  await aqiFormRef.value?.validate((valid) => { isAqiValid = valid }).catch(() => { })
  await formRef.value?.validate((valid) => { isMainFormValid = valid }).catch(() => { })

  if (!form.value.estimatedAqiLevel) {
    ElMessage.warning({ message: '请在右侧面板点选 AQI 污染等级', customClass: 'swiss-message' })
    return
  }
  if (!isMainFormValid) return

  submitting.value = true
  try {
    await submitFeedback(form.value)
    ElMessage.success({ message: '监督反馈档案提报成功！', customClass: 'swiss-message' })
    router.push('/ne/feedbacks')
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await getProvinces()
  provinces.value = res.data
  const uid = localStorage.getItem('userId')
  if (uid) form.value.supervisorId = parseInt(uid)
})
</script>

<style scoped>
/* =========================================
   完全复刻 Home.vue 的核心布局架构
   ========================================= */

.swiss-dashboard {
  max-width: 1440px;
  width: 100%;
  height: 100%;
  /* 填满父容器 */
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  color: #1C2421;
}

/* 锁定部分：禁止压缩 */
.fixed-section {
  flex-shrink: 0;
}

/* 伸缩部分：填满剩余空间，死锁全局滑动 */
.stretch-section {
  flex: 1;
  min-height: 0;
}

/* 通用玻璃面板 */
.glass-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 8px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6);
}

/* 卡片内部结构 */
.scrollable-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 防止内部结构撑爆玻璃面板 */
}

.panel-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(28, 36, 33, 0.06);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1C2421;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title .el-icon {
  color: #2A483A;
  font-size: 18px;
}

/* 局部滑动核心区 */
.scroll-area {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.scroll-area::-webkit-scrollbar {
  display: none;
}

.panel-body {
  padding: 24px 28px 32px;
}

/* =========================================
   1. 顶部悬浮控制台 (对齐 Home.vue 英雄区)
   ========================================= */
.action-console {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 48px;
  height: 120px;
  /* 强制高度匹配 */
}

.console-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-nav-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.5);
  color: #1C2421;
  font-size: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.icon-nav-btn:hover {
  background: #FFF;
  transform: scale(1.05);
}

.title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin: 0;
  letter-spacing: 0.5px;
  color: #1C2421;
}

.page-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #74807B;
}

.console-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* macOS 风格按钮 */
.swiss-btn {
  border: none;
  outline: none;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.swiss-btn.ghost-btn {
  padding: 0 20px;
  height: 40px;
  background: rgba(255, 255, 255, 0.6);
  color: #1C2421;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.swiss-btn.ghost-btn:hover {
  background: #FFF;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.swiss-btn.primary-btn {
  padding: 0 24px;
  height: 40px;
  background: #2A483A;
  color: #FFF;
  box-shadow: 0 2px 10px rgba(42, 72, 58, 0.3);
}

.swiss-btn.primary-btn:hover:not(.is-loading) {
  background: #1C2421;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(42, 72, 58, 0.4);
}

.is-spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

/* =========================================
   2. 并排主面板区 
   ========================================= */
.form-split {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  /* 左 1 : 右 1.2 */
  gap: 24px;
}

/* 左侧：表单设计 (macOS Inset 质感) */
.input-row {
  display: flex;
  gap: 16px;
}

.flex-1 {
  flex: 1;
  min-width: 0;
}

.margin-bottom-0 {
  margin-bottom: 0 !important;
}

.mac-inset-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: #74807B;
  padding-bottom: 8px;
  line-height: 1;
}

.mac-inset-form :deep(.el-input__wrapper),
.mac-inset-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.4) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.03), 0 0 0 1px rgba(255, 255, 255, 0.9) !important;
  border-radius: 10px !important;
  border: none;
  transition: all 0.3s;
  padding: 10px 14px;
  font-size: 14px;
  color: #1C2421;
}

.mac-inset-form :deep(.el-input__wrapper.is-focus),
.mac-inset-form :deep(.el-textarea__inner:focus) {
  background: #FFF !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.01), 0 0 0 2px #2A483A !important;
}

/* 右侧：AQI 拨盘 */
.required-asterisk {
  font-size: 12px;
  color: #D9534F;
  font-weight: 600;
}

.aqi-sensory-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 32px;
}

.aqi-tile {
  height: 64px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.9);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.tile-glow {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.tile-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.level-badge {
  font-size: 11px;
  font-weight: 700;
  color: #FFF;
  padding: 2px 8px;
  border-radius: 6px;
}

.level-desc {
  font-size: 13px;
  font-weight: 600;
  color: #1C2421;
}

.aqi-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.aqi-tile.is-selected {
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.aqi-tile.is-selected .tile-glow {
  opacity: 0.15;
}

/* 右侧：参考表 */
.section-sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #74807B;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.reference-table-wrapper {
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  overflow: hidden;
}

.premium-ref-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 12px;
}

.premium-ref-table th {
  background: rgba(28, 36, 33, 0.03);
  color: #74807B;
  font-weight: 600;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(28, 36, 33, 0.05);
}

.premium-ref-table td {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(28, 36, 33, 0.03);
  color: #1C2421;
  transition: background 0.3s;
}

.premium-ref-table tbody tr:last-child td {
  border-bottom: none;
}

.premium-ref-table tbody tr:hover td {
  background: rgba(255, 255, 255, 0.4);
}

.premium-ref-table tbody tr.is-active-row td {
  background: rgba(42, 168, 118, 0.08);
}

.font-mono {
  font-family: "SF Mono", Consolas, monospace;
  font-weight: 600;
}

.text-secondary {
  color: #74807B;
  font-weight: 500;
}

.text-hint {
  color: #515154;
  line-height: 1.4;
}

.ref-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

@media (max-width: 1024px) {
  .form-split {
    grid-template-columns: 1fr;
  }

  .action-console {
    padding: 0 24px;
  }
}
</style>

<style>
/* 注入：下拉框与弹窗 Mac 质感 */
.swiss-select-dropdown {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(24px) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 12px !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08) !important;
}

.swiss-select-dropdown .el-select-dropdown__item.selected {
  color: #2A483A !important;
  font-weight: 600 !important;
  background: rgba(42, 168, 118, 0.08) !important;
}

.swiss-message {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(24px) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 12px !important;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08) !important;
  color: #1C2421 !important;
  font-weight: 600 !important;
}
</style>