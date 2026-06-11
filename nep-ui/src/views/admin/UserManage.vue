<template>
  <el-card shadow="never">
    <template #header><div class="card-header"><span>用户管理</span></div></template>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="phone" label="手机号" width="140"/>
      <el-table-column prop="realName" label="姓名" width="100"/>
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{row}">
          <el-tag v-if="row.role==='NEPS'">公众监督员</el-tag>
          <el-tag v-else-if="row.role==='NEPG'" type="success">网格员</el-tag>
          <el-tag v-else-if="row.role==='NEPM'" type="warning">管理员</el-tag>
          <el-tag v-else type="info">决策者</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}">
          <el-switch :model-value="row.status===1" @change="toggleStatus(row)" :loading="row.switching"/>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180"/>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{row}">
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, updateUser } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const loading = ref(false)

async function fetchUsers() {
  loading.value = true
  try { const r = await getUserList(); users.value = r.data } catch (e) {} finally { loading.value = false }
}

async function toggleStatus(row) {
  row.switching = true
  try { await updateUser(row.id, { status: row.status === 1 ? 0 : 1 }); fetchUsers(); ElMessage.success('状态已更新') }
  catch (e) {}
  finally { row.switching = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户 ${row.realName}？`, '确认', { type: 'warning' })
  try {
    // 这里可以添加删除接口
    ElMessage.success('已删除')
    fetchUsers()
  } catch (e) {}
}

onMounted(fetchUsers)
</script>
