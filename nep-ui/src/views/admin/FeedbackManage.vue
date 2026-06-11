<template>
  <el-card shadow="never">
    <template #header><div class="card-header"><span>反馈管理（指派网格员）</span></div></template>
    <el-table :data="feedbacks" v-loading="loading" stripe>
      <el-table-column prop="id" label="编号" width="60"/>
      <el-table-column prop="supervisorId" label="监督员ID" width="90"/>
      <el-table-column prop="cityId" label="城市" width="80"/>
      <el-table-column prop="specificAddress" label="地址" show-overflow-tooltip/>
      <el-table-column prop="estimatedAqiLevel" label="预估AQI" width="90">
        <template #default="{row}"><el-tag :type="row.estimatedAqiLevel<=2?'success':row.estimatedAqiLevel<=4?'warning':'danger'">{{ row.estimatedAqiLevel }}级</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{row}">
          <el-tag v-if="row.status==='PENDING'" type="warning">待指派</el-tag>
          <el-tag v-else-if="row.status==='ASSIGNED'" type="primary">已指派</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <template v-if="row.status==='PENDING'">
            <el-input-number v-model="row._inspectorId" :min="1" size="small" placeholder="网格员ID" style="width:100px"/>
            <el-button size="small" type="primary" @click="handleAssign(row)" style="margin-left:8px">指派</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetch"/>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFeedbackPage, assignInspector } from '@/api/feedback'
import { ElMessage } from 'element-plus'

const feedbacks = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetch() {
  loading.value = true
  try { const r = await getFeedbackPage(page.value, size.value); feedbacks.value = r.data.map(f => ({...f, _inspectorId: null})); total.value = r.total }
  catch (e) {} finally { loading.value = false }
}

async function handleAssign(row) {
  if (!row._inspectorId) { ElMessage.warning('请输入网格员ID'); return }
  try { await assignInspector(row.id, row._inspectorId); ElMessage.success('指派成功'); fetch() } catch (e) {}
}

onMounted(fetch)
</script>
