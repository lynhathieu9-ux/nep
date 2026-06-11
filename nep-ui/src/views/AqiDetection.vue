<template>
  <div class="aqi-detection">
    <!-- 提交检测 -->
    <el-card shadow="never" class="submit-card">
      <template #header>
        <span>提交实测AQI数据</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="180px" style="max-width: 600px">
        <el-form-item label="关联反馈ID" prop="feedbackId">
          <el-input-number v-model="form.feedbackId" :min="1" placeholder="请输入反馈ID" style="width: 100%" />
        </el-form-item>

        <el-divider content-position="left">AQI检测数据</el-divider>

        <el-form-item label="SO₂二氧化硫AQI" prop="so2Aqi">
          <el-input-number v-model="form.so2Aqi" :min="0" :max="500" style="width: 100%" />
          <template #extra>
            <span style="font-size: 12px; color: #999;">二氧化硫浓度对应的AQI等级值</span>
          </template>
        </el-form-item>

        <el-form-item label="CO一氧化碳AQI" prop="coAqi">
          <el-input-number v-model="form.coAqi" :min="0" :max="500" style="width: 100%" />
        </el-form-item>

        <el-form-item label="PM2.5悬浮颗粒物AQI" prop="pm25Aqi">
          <el-input-number v-model="form.pm25Aqi" :min="0" :max="500" style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="检测备注(可选)" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" :icon="Upload">
            提交检测数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 检测记录列表 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span>检测记录列表</span>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="feedbackId" label="反馈ID" width="80" />
        <el-table-column prop="inspectorId" label="网格员ID" width="100" />
        <el-table-column prop="so2Aqi" label="SO₂ AQI" width="100" />
        <el-table-column prop="coAqi" label="CO AQI" width="100" />
        <el-table-column prop="pm25Aqi" label="PM2.5 AQI" width="100" />
        <el-table-column prop="finalAqi" label="最终AQI" width="100">
          <template #default="{ row }">
            <el-tag :type="aqiTagType(row.finalAqi)" size="large">
              {{ row.finalAqi }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectionTime" label="检测时间" width="180" />
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { submitAqi, getAqiPage } from '@/api/aqi'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({
  feedbackId: null,
  so2Aqi: null,
  coAqi: null,
  pm25Aqi: null,
  remark: '',
  inspectorId: 2  // 实际应从登录状态获取
})

const rules = {
  feedbackId: [{ required: true, message: '请输入关联的反馈ID', trigger: 'blur' }],
  so2Aqi: [{ required: true, message: '请输入SO2的AQI值', trigger: 'blur' }],
  coAqi: [{ required: true, message: '请输入CO的AQI值', trigger: 'blur' }],
  pm25Aqi: [{ required: true, message: '请输入PM2.5的AQI值', trigger: 'blur' }]
}

function aqiTagType(level) {
  if (level <= 50) return 'success'
  if (level <= 100) return ''
  if (level <= 150) return 'warning'
  if (level <= 200) return 'danger'
  return 'danger'
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await submitAqi(form.value)
    ElMessage.success('AQI检测数据提交成功')
    formRef.value.resetFields()
    fetchList()
  } catch (e) {
    // handled
  } finally {
    submitting.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getAqiPage(currentPage.value, pageSize.value)
    tableData.value = res.data
    total.value = res.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
  const uid = localStorage.getItem('userId')
  if (uid) form.value.inspectorId = parseInt(uid)
})
</script>

<style scoped>
.aqi-detection { max-width: 1200px; margin: 0 auto; }
.submit-card { margin-bottom: 20px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
