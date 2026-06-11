<template>
  <div class="feedback-submit">
    <el-card shadow="never">
      <template #header>
        <span>提交空气质量监督反馈</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
        <el-form-item label="选择省份" prop="provinceId">
          <el-select v-model="form.provinceId" placeholder="请选择省份" @change="onProvinceChange" style="width: 100%">
            <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="选择城市" prop="cityId">
          <el-select v-model="form.cityId" placeholder="请选择城市" style="width: 100%">
            <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="具体地址" prop="specificAddress">
          <el-input v-model="form.specificAddress" placeholder="请输入观测的具体地址" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="预估AQI等级" prop="estimatedAqiLevel">
          <el-select v-model="form.estimatedAqiLevel" placeholder="请选择预估AQI等级" style="width: 100%">
            <el-option label="一级 - 优" :value="1">
              <span style="color: #00e400">一级 - 优 (0~50)</span>
            </el-option>
            <el-option label="二级 - 良" :value="2">
              <span style="color: #ffff00">二级 - 良 (51~100)</span>
            </el-option>
            <el-option label="三级 - 轻度污染" :value="3">
              <span style="color: #ff7e00">三级 - 轻度污染 (101~150)</span>
            </el-option>
            <el-option label="四级 - 中度污染" :value="4">
              <span style="color: #ff0000">四级 - 中度污染 (151~200)</span>
            </el-option>
            <el-option label="五级 - 重度污染" :value="5">
              <span style="color: #99004c">五级 - 重度污染 (201~300)</span>
            </el-option>
            <el-option label="六级 - 严重污染" :value="6">
              <span style="color: #7e0023">六级 - 严重污染 (>300)</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="空气描述" prop="description">
          <el-input v-model="form.description" placeholder="请描述观测到的空气质量情况" type="textarea" :rows="4" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" :icon="Promotion">
            提交反馈
          </el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- AQI 等级参考表 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span>空气质量指数(AQI)范围及相应类别参考表</span>
      </template>
      <el-table :data="aqiReference" border stripe>
        <el-table-column prop="level" label="AQI等级" width="120" />
        <el-table-column prop="range" label="AQI范围" width="150" />
        <el-table-column prop="category" label="类别" width="150" />
        <el-table-column prop="color" label="表示颜色" width="120">
          <template #default="{ row }">
            <div :style="{ width: '30px', height: '20px', background: row.color, borderRadius: '3px' }" />
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="健康建议" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { submitFeedback } from '@/api/feedback'
import { getProvinces, getCitiesByProvince } from '@/api/region'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const provinces = ref([])
const cities = ref([])

const form = ref({
  provinceId: null,
  cityId: null,
  specificAddress: '',
  estimatedAqiLevel: null,
  description: '',
  supervisorId: 1  // 实际应从登录状态获取
})

const rules = {
  provinceId: [{ required: true, message: '请选择省份', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择城市', trigger: 'change' }],
  specificAddress: [{ required: true, message: '请输入具体地址', trigger: 'blur' }],
  estimatedAqiLevel: [{ required: true, message: '请选择AQI等级', trigger: 'change' }],
  description: [{ required: true, message: '请输入空气质量描述', trigger: 'blur' }]
}

const aqiReference = [
  { level: '一级', range: '0~50', category: '优', color: '#00e400', suggestion: '各类人群可正常活动' },
  { level: '二级', range: '51~100', category: '良', color: '#ffff00', suggestion: '极少数异常敏感人群应减少户外活动' },
  { level: '三级', range: '101~150', category: '轻度污染', color: '#ff7e00', suggestion: '儿童、老年人及心脏病、呼吸系统疾病患者应减少长时间、高强度的户外锻炼' },
  { level: '四级', range: '151~200', category: '中度污染', color: '#ff0000', suggestion: '一般人群减少户外活动' },
  { level: '五级', range: '201~300', category: '重度污染', color: '#99004c', suggestion: '一般人群应避免户外活动' },
  { level: '六级', range: '>300', category: '严重污染', color: '#7e0023', suggestion: '所有人应避免户外活动' }
]

async function onProvinceChange(provinceId) {
  form.value.cityId = null
  cities.value = []
  if (provinceId) {
    const res = await getCitiesByProvince(provinceId)
    cities.value = res.data
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await submitFeedback(form.value)
    ElMessage.success('反馈提交成功')
    router.push('/feedback')
  } catch (e) {
    // handled
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await getProvinces()
  provinces.value = res.data
  // 从localStorage获取当前用户ID
  const uid = localStorage.getItem('userId')
  if (uid) form.value.supervisorId = parseInt(uid)
})
</script>

<style scoped>
.feedback-submit { max-width: 800px; margin: 0 auto; }
</style>
