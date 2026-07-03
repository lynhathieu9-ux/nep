<template>
  <div class="manage-page">
    <div class="page-header">
      <h3>📰 新闻公告管理</h3>
      <el-button type="primary" @click="showDialog(null)">+ 发布新闻</el-button>
    </div>

    <el-table :data="newsList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="newsType" label="类型" width="100">
        <template #default="{row}">
          <el-tag size="small" :type="row.newsType === 'NEWS' ? 'primary' : row.newsType === 'NOTICE' ? 'warning' : 'success'">
            {{ row.newsType === 'NEWS' ? '新闻' : row.newsType === 'NOTICE' ? '公告' : '政策' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}">{{ row.status === 1 ? '已发布' : row.status === 0 ? '草稿' : '已下架' }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button size="small" @click="showDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box" v-if="total > size">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" background />
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑新闻' : '发布新闻'" width="700px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.newsType">
            <el-option label="环保新闻" value="NEWS" />
            <el-option label="系统公告" value="NOTICE" />
            <el-option label="政策法规" value="POLICY" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入摘要" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入内容（支持HTML）" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex; flex-direction:column; gap:8px; width:100%">
            <el-input v-model="form.coverImage" placeholder="图片URL（可直接填，或点下方上传）" />
            <!-- 问题④：图片上传，上传成功后自动回填 coverImage -->
            <el-upload
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept="image/*"
            >
              <el-button size="small" :loading="uploading">📷 上传图片</el-button>
            </el-upload>
            <img v-if="form.coverImage" :src="form.coverImage" alt="封面预览"
                 style="max-width:200px; max-height:120px; border-radius:6px; object-fit:cover" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNewsPage, createNews, updateNews, deleteNews } from '@/api/news'
import { uploadImage } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const newsList = ref([])
const loading = ref(false)
const uploading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ title: '', newsType: 'NEWS', summary: '', content: '', coverImage: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getNewsPage(page.value, size.value)
    newsList.value = res.data || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}

function showDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row }
  } else {
    isEdit.value = false
    editId.value = null
    form.value = { title: '', newsType: 'NEWS', summary: '', content: '', coverImage: '' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (isEdit.value) {
      await updateNews(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createNews(form.value)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该新闻？', '提示', { type: 'warning' })
    await deleteNews(id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {}
}

// 问题④：封面图上传，成功后回填 coverImage
async function handleCoverUpload(file) {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return false
  }
  uploading.value = true
  try {
    const res = await uploadImage(file)
    form.value.coverImage = res.data
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
  }
  return false // 阻止 el-upload 默认上传行为
}

onMounted(fetchData)
</script>

<style scoped>
.manage-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h3 { margin: 0; font-size: 18px; color: #1C2421; }
.pagination-box { display: flex; justify-content: center; margin-top: 20px; }
</style>
