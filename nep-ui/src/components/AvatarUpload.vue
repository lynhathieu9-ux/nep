<template>
  <div class="avatar-upload">
    <el-upload
      class="avatar-uploader"
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      name="file"
    >
      <el-avatar v-if="displayUrl" :size="size" :src="displayUrl" />
      <div v-else class="upload-placeholder" :style="{ width: size+'px', height: size+'px' }">
        <el-icon :size="size * 0.4"><Plus /></el-icon>
      </div>
    </el-upload>
    <div v-if="displayUrl" style="margin-top:8px;text-align:center">
      <el-button text type="primary" size="small" @click="confirm">确认使用此头像</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  size: { type: Number, default: 100 }
})
const emit = defineEmits(['update:modelValue', 'uploaded'])

const uploadUrl = '/api/file/avatar'
const internalUrl = ref('')

// 同步外部传入的值
watch(() => props.modelValue, (val) => {
  if (val && !internalUrl.value) internalUrl.value = val
}, { immediate: true })

const displayUrl = computed(() => internalUrl.value || props.modelValue)

const headers = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function confirm() {
  emit('update:modelValue', displayUrl.value)
  emit('uploaded', displayUrl.value)
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片文件！'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过5MB！'); return false }
  return true
}

function handleSuccess(response) {
  if (response.code === 200) {
    internalUrl.value = response.data
    // 自动触发确认
    emit('update:modelValue', response.data)
    emit('uploaded', response.data)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleError() {
  ElMessage.error('上传失败，请重试')
}
</script>

<style scoped>
.upload-placeholder {
  border: 2px dashed #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
  color: #999;
  background: #fafafa;
}
.upload-placeholder:hover { border-color: #409EFF; color: #409EFF; }

/* 修复头像"扁圆"：强制圆形容器 + 图片等比裁剪填充 */
.avatar-uploader :deep(.el-upload) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.avatar-uploader :deep(.el-avatar) {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-uploader :deep(.el-avatar img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
