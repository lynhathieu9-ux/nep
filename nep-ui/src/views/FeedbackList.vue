<template>
  <div class="feedback-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="待指派" value="PENDING" />
            <el-option label="已指派" value="ASSIGNED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>监督反馈列表</span>
          <el-button type="primary" :icon="Plus" @click="$router.push('/feedback/submit')">
            提交新反馈
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="supervisorId" label="监督员ID" width="100" />
        <el-table-column prop="provinceId" label="省份" width="100" />
        <el-table-column prop="cityId" label="城市" width="100" />
        <el-table-column prop="specificAddress" label="具体地址" show-overflow-tooltip />
        <el-table-column prop="estimatedAqiLevel" label="预估AQI" width="100">
          <template #default="{ row }">
            <el-tag :type="aqiTagType(row.estimatedAqiLevel)">
              {{ row.estimatedAqiLevel }} 级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning">待指派</el-tag>
            <el-tag v-else-if="row.status === 'ASSIGNED'" type="primary">已指派</el-tag>
            <el-tag v-else type="success">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="600px">
      <el-descriptions v-if="currentFeedback" :column="2" border>
        <el-descriptions-item label="编号">{{ currentFeedback.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentFeedback.status === 'PENDING'" type="warning">待指派</el-tag>
          <el-tag v-else-if="currentFeedback.status === 'ASSIGNED'" type="primary">已指派</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="省份">{{ currentFeedback.provinceId }}</el-descriptions-item>
        <el-descriptions-item label="城市">{{ currentFeedback.cityId }}</el-descriptions-item>
        <el-descriptions-item label="具体地址" :span="2">{{ currentFeedback.specificAddress }}</el-descriptions-item>
        <el-descriptions-item label="预估AQI等级">{{ currentFeedback.estimatedAqiLevel }} 级</el-descriptions-item>
        <el-descriptions-item label="指派网格员">{{ currentFeedback.assignedInspectorId || '未指派' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentFeedback.description }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentFeedback.createTime }}</el-descriptions-item>
        <el-descriptions-item label="指派时间">{{ currentFeedback.assignTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFeedbackPage } from '@/api/feedback'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = ref({ status: '' })

const detailVisible = ref(false)
const currentFeedback = ref(null)

function aqiTagType(level) {
  if (level <= 2) return 'success'
  if (level <= 4) return 'warning'
  return 'danger'
}

async function handleSearch() {
  loading.value = true
  try {
    const res = await getFeedbackPage(currentPage.value, pageSize.value, searchForm.value.status)
    tableData.value = res.data
    total.value = res.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

function handleReset() {
  searchForm.value.status = ''
  currentPage.value = 1
  handleSearch()
}

function handleDetail(row) {
  currentFeedback.value = row
  detailVisible.value = true
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.feedback-list { max-width: 1400px; margin: 0 auto; }
.search-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
