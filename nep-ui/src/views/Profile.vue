<template>
  <div style="max-width:700px;margin:0 auto">
    <el-card shadow="never" v-loading="loading">
      <template #header><span>个人中心</span></template>

      <!-- 头像区域 -->
      <div style="text-align:center;margin-bottom:24px">
        <avatar-upload v-model="avatar" @uploaded="saveAvatar" />
        <p style="color:#999;font-size:13px;margin-top:8px">点击更换头像（限5MB以内图片）</p>
      </div>

      <el-divider />

      <!-- 信息表单 -->
      <el-form v-if="loaded" ref="formRef" :model="form" label-width="100px" style="max-width:450px;margin:0 auto">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="16" :max="120" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-tag v-if="form.role==='NEPS'">公众监督员</el-tag>
          <el-tag v-else-if="form.role==='NEPG'" type="success">网格员</el-tag>
          <el-tag v-else-if="form.role==='NEPM'" type="warning">管理员</el-tag>
          <el-tag v-else type="info">决策者</el-tag>
        </el-form-item>
        <el-form-item label="注册时间">
          <span>{{ form.createTime }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="doSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUser, getUserById } from '@/api/user'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)
const loading = ref(true)
const loaded = ref(false)
const avatar = ref('')
const userId = ref(null)

const form = ref({
  phone: '', realName: '', age: 25, gender: 1, role: '', createTime: ''
})

async function loadProfile() {
  const uid = localStorage.getItem('userId')
  if (!uid) return
  userId.value = uid
  try {
    const res = await getUserById(uid)
    const u = res.data
    form.value = {
      phone: u.phone || '',
      realName: u.realName || '',
      age: u.age || 25,
      gender: u.gender ?? 1,
      role: u.role || '',
      createTime: u.createTime || ''
    }
    avatar.value = u.avatar || ''
  } catch (e) {
    console.error('加载用户信息失败:', e)
  } finally {
    loading.value = false
    loaded.value = true
  }
}

function saveAvatar(url) {
  avatar.value = url
  doSave({ avatar: url })
}

async function doSave(extra) {
  saving.value = true
  try {
    const data = {
      realName: form.value.realName,
      age: form.value.age,
      gender: form.value.gender
    }
    if (extra && extra.avatar) data.avatar = extra.avatar
    if (avatar.value && !data.avatar) data.avatar = avatar.value

    console.log('保存数据:', data, 'userId:', userId.value)
    const res = await updateUser(userId.value, data)
    console.log('保存结果:', res)
    ElMessage.success('保存成功')
    await userStore.fetchUser()
  } catch (e) {
    console.error('保存失败，详细信息:', e)
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
