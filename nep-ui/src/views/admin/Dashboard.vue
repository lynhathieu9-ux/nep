<template>
  <div>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="注册用户数" :value="stats.userCount"/></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="反馈总数" :value="stats.feedbackCount"/></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="待处理反馈" :value="stats.pendingCount"/></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="AQI检测数" :value="stats.aqiCount"/></el-card></el-col>
    </el-row>
    <el-card shadow="never">
      <template #header><span>快捷操作</span></template>
      <el-button type="primary" @click="$router.push('/admin/users')">用户管理</el-button>
      <el-button type="warning" @click="$router.push('/admin/feedbacks')">反馈管理</el-button>
      <el-button type="success" @click="$router.push('/admin/statistics')">数据统计</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList } from '@/api/user'
import { getFeedbackPage } from '@/api/feedback'

const stats = ref({ userCount: 0, feedbackCount: 0, pendingCount: 0, aqiCount: 0 })

onMounted(async () => {
  try { const r = await getUserList(); stats.value.userCount = r.data.length } catch (e) {}
  try { const r = await getFeedbackPage(1, 1); stats.value.feedbackCount = r.total } catch (e) {}
  try { const r = await getFeedbackPage(1, 1, 'PENDING'); stats.value.pendingCount = r.total } catch (e) {}
})
</script>
