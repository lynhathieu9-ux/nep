<template>
  <div class="swiss-page-container">
    
    <header class="action-console fixed-section">
      <div class="console-left">
        <h2 class="page-title">账户与安全</h2>
        <span class="page-subtitle">管理您的个人档案与系统访问凭证</span>
      </div>
    </header>

    <main class="stretch-section" v-loading="loading">
      <div v-if="loaded" class="profile-glass-card glass-panel">
        
        <aside class="profile-sidebar">
          <div class="avatar-showcase">
            <div class="avatar-ring">
              <avatar-upload v-model="avatar" :size="120" @uploaded="saveAvatar" class="premium-avatar-uploader" />
            </div>
            <div class="avatar-hint">
              <el-icon><Camera /></el-icon>
              <span>点击更换面容 (Max 5MB)</span>
            </div>
          </div>

          <div class="identity-info">
            <h2 class="user-display-name">{{ form.realName || '未命名用户' }}</h2>
            <div class="role-badge" :class="form.role?.toLowerCase() || 'default'">
              {{ roleLabel }}
            </div>
            <div class="join-date">
              <el-icon><Timer /></el-icon> 注册于 {{ formatTime(form.createTime) }}
            </div>
          </div>
        </aside>

        <div class="vertical-separator"></div>

        <section class="profile-content">
          <div class="section-header">
            <h3 class="section-title"><el-icon><User /></el-icon> 个人档案设定</h3>
          </div>

          <el-form ref="formRef" :model="form" label-position="top" class="swiss-form">
            <div class="form-grid">
              
              <el-form-item label="真实姓名" class="grid-col">
                <el-input v-model="form.realName" placeholder="请输入您的真实姓名" class="swiss-input" />
              </el-form-item>
              
              <el-form-item label="手机号 (绑定账号)" class="grid-col">
                <el-input v-model="form.phone" disabled class="swiss-input is-locked">
                  <template #prefix><el-icon><Iphone /></el-icon></template>
                  <template #suffix><el-icon class="lock-icon"><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="联络邮箱" class="full-width">
                <el-input v-model="form.email" placeholder="example@domain.com" class="swiss-input">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="年龄" class="grid-col">
                <el-input-number v-model="form.age" :min="16" :max="120" class="swiss-input-number" controls-position="right" />
              </el-form-item>

              <el-form-item label="性别" class="grid-col">
                <el-radio-group v-model="form.gender" class="swiss-segmented-control">
                  <el-radio-button :value="1"><el-icon><Male /></el-icon> 男</el-radio-button>
                  <el-radio-button :value="0"><el-icon><Female /></el-icon> 女</el-radio-button>
                </el-radio-group>
              </el-form-item>

            </div>

            <div class="form-actions">
              <button type="button" class="action-btn outline" @click="showPwdDialog = true">
                <el-icon><Key /></el-icon> 修改密码
              </button>
              <button type="button" class="action-btn primary" @click="doSave" :disabled="saving">
                <el-icon v-if="!saving"><Check /></el-icon>
                <el-icon v-else class="is-loading"><Loading /></el-icon>
                {{ saving ? '保存中...' : '保存修改' }}
              </button>
            </div>
          </el-form>
        </section>

      </div>
    </main>

    <el-dialog 
      v-model="showPwdDialog" 
      title="更新安全凭证" 
      width="440px" 
      :close-on-click-modal="false" 
      destroy-on-close
      class="swiss-dialog"
    >
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="swiss-form">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前原密码" class="swiss-input" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入至少6位新密码" class="swiss-input" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次验证新密码" class="swiss-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <button class="action-btn ghost" @click="showPwdDialog = false">取消</button>
          <button class="action-btn primary" :disabled="changingPwd" @click="handleChangePassword">
            确认更新
          </button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUser, getUserById, changePassword } from '@/api/user'
import AvatarUpload from '@/components/AvatarUpload.vue'
import { ElMessage } from 'element-plus'
import { 
  Camera, Timer, User, Iphone, Lock, Message, 
  Male, Female, Key, Check, Loading 
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const saving = ref(false)
const loading = ref(true)
const loaded = ref(false)
const avatar = ref('')
const userId = ref(null)

const form = ref({
  phone: '', email: '', realName: '', age: 25, gender: 1, role: '', createTime: ''
})

const roleLabel = computed(() => {
  const r = form.value.role
  if (r === 'NEPS') return '公众监督员'
  if (r === 'NEPG') return '环境网格员'
  if (r === 'NEPM') return '系统管理员'
  if (r === 'NEPV') return '监管决策者'
  return '系统用户'
})

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function loadProfile() {
  const uid = localStorage.getItem('userId')
  if (!uid) return
  userId.value = uid
  try {
    const res = await getUserById(uid)
    const u = res.data
    form.value = {
      phone: u.phone || '',
      email: u.email || '',
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
      email: form.value.email,
      age: form.value.age,
      gender: form.value.gender
    }
    if (extra && extra.avatar) data.avatar = extra.avatar
    if (avatar.value && !data.avatar) data.avatar = avatar.value

    await updateUser(userId.value, data)
    ElMessage.success('档案信息更新成功')
    await userStore.fetchUser()
  } catch (e) {
    ElMessage.error('更新失败: ' + (e.response?.data?.message || e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// ==================== 修改密码 ====================
const showPwdDialog = ref(false)
const changingPwd = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.value.newPassword) {
    callback(new Error('两次输入的密码验证不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '必须提供当前使用的密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请设置新的安全凭证', trigger: 'blur' },
    { min: 6, message: '出于安全考虑，密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '必须确认一次新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  await pwdFormRef.value.validate()
  changingPwd.value = true
  try {
    await changePassword({
      userId: userId.value,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('凭证变更成功，您的账户已处于最新安全级别。')
    showPwdDialog.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    // 错误在拦截器处理
  } finally {
    changingPwd.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
/* ========== 全局主容器 ========== */
.swiss-page-container { max-width: 1200px; width: 100%; height: 100%; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; padding-bottom: 32px; box-sizing: border-box; color: #1C2421; }
.fixed-section { flex-shrink: 0; }
.stretch-section { flex: 1; min-height: 0; display: flex; align-items: flex-start; justify-content: center; }

/* 悬浮控制台 */
.action-console { height: 100px; padding: 0 40px; display: flex; align-items: center; box-sizing: border-box; }
.console-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 28px; font-weight: 700; margin: 0; color: #1C2421; letter-spacing: 0.5px; }
.page-subtitle { font-size: 14px; font-weight: 500; color: #74807B; }

/* 核心玻璃卡片 */
.glass-panel {
  background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 32px; box-shadow: 0 12px 48px -12px rgba(28, 36, 33, 0.08), inset 0 2px 4px rgba(255, 255, 255, 0.6);
}

.profile-glass-card {
  width: 100%; max-width: 960px; display: flex; flex-direction: row; 
  min-height: 540px; overflow: hidden; margin-top: 16px;
}

/* ========== 左侧：数字身份画廊 ========== */
.profile-sidebar {
  width: 320px; padding: 48px 32px; display: flex; flex-direction: column; 
  align-items: center; background: rgba(255, 255, 255, 0.3);
}

.avatar-showcase { display: flex; flex-direction: column; align-items: center; gap: 16px; margin-bottom: 32px; }
.avatar-ring {
  width: 136px; height: 136px; border-radius: 50%; padding: 6px;
  background: linear-gradient(135deg, rgba(42, 72, 58, 0.1), rgba(42, 72, 58, 0.02));
  border: 1px solid rgba(42, 72, 58, 0.05); box-shadow: 0 12px 24px -8px rgba(28, 36, 33, 0.08);
}
.premium-avatar-uploader :deep(.el-upload) {
  width: 122px; height: 122px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
/* 强制头像为正方形圆形并等比裁剪，修复"扁圆"变形 */
.premium-avatar-uploader :deep(.el-avatar) {
  width: 122px !important; height: 122px !important;
  border-radius: 50%; overflow: hidden;
}
.premium-avatar-uploader :deep(.el-avatar img) {
  width: 100%; height: 100%; object-fit: cover;
}
.premium-avatar-uploader :deep(.upload-placeholder) {
  width: 122px !important; height: 122px !important;
}

.avatar-hint { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #A0AAB2; font-weight: 500; }

.identity-info { display: flex; flex-direction: column; align-items: center; gap: 12px; text-align: center; }
.user-display-name { font-size: 24px; font-weight: 800; color: #1C2421; margin: 0; letter-spacing: 0.5px; }

.role-badge { padding: 6px 16px; border-radius: 12px; font-size: 13px; font-weight: 700; letter-spacing: 0.5px; }
.role-badge.neps { background: #E8F5E9; color: #2AA876; }
.role-badge.nepg { background: #E1F5FE; color: #409EFF; }
.role-badge.nepm { background: #FFF8E1; color: #F5A623; }
.role-badge.nepv { background: #F3E5F5; color: #8E44AD; }
.role-badge.default { background: #F4F6F5; color: #74807B; }

.join-date { font-size: 13px; color: #74807B; font-family: monospace; display: flex; align-items: center; gap: 6px; margin-top: 8px; }

/* 垂直分割线 */
.vertical-separator { width: 1px; background: linear-gradient(to bottom, rgba(28, 36, 33, 0), rgba(28, 36, 33, 0.06), rgba(28, 36, 33, 0)); }

/* ========== 右侧：档案编辑区 ========== */
.profile-content { flex: 1; padding: 48px; display: flex; flex-direction: column; }
.section-header { margin-bottom: 32px; }
.section-title { font-size: 18px; font-weight: 700; color: #1C2421; margin: 0; display: flex; align-items: center; gap: 10px; }
.section-title .el-icon { color: #2A483A; font-size: 20px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 24px; }
.full-width { grid-column: span 2; }

/* 高级表单覆盖 */
.swiss-form :deep(.el-form-item__label) { font-size: 13px; font-weight: 600; color: #74807B; padding-bottom: 6px; line-height: 1; }
.swiss-form :deep(.el-form-item) { margin-bottom: 24px; }

.swiss-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.5) !important; box-shadow: none !important;
  border: 1px solid rgba(28, 36, 33, 0.08); border-radius: 12px; height: 44px; transition: all 0.3s;
}
.swiss-input :deep(.el-input__wrapper.is-focus) { background: #FFF !important; border-color: #2AA876; box-shadow: 0 0 0 3px rgba(42, 168, 118, 0.1) !important; }
.swiss-input :deep(.el-input__inner) { color: #1C2421; font-weight: 500; font-size: 15px; }
.swiss-input :deep(.el-input__prefix) { color: #A0AAB2; font-size: 18px; margin-right: 4px; }

/* 不可变凭证锁 */
.swiss-input.is-locked :deep(.el-input__wrapper) { background: rgba(28, 36, 33, 0.03) !important; border-color: transparent; }
.swiss-input.is-locked :deep(.el-input__inner) { color: #A0AAB2; }
.lock-icon { color: #A0AAB2; font-size: 16px; }

/* 步进器定制 */
.swiss-input-number { width: 100%; }
.swiss-input-number :deep(.el-input__wrapper) { background: rgba(255, 255, 255, 0.5) !important; box-shadow: none !important; border: 1px solid rgba(28, 36, 33, 0.08); border-radius: 12px; height: 44px; transition: all 0.3s; }
.swiss-input-number :deep(.el-input-number__increase), .swiss-input-number :deep(.el-input-number__decrease) { background: transparent; border-color: rgba(28, 36, 33, 0.08); color: #74807B; }

/* 分段控制器 (Radio Group) */
.swiss-segmented-control { display: flex; width: 100%; background: rgba(28, 36, 33, 0.04); padding: 4px; border-radius: 14px; box-sizing: border-box; }
.swiss-segmented-control :deep(.el-radio-button) { flex: 1; }
.swiss-segmented-control :deep(.el-radio-button__inner) { width: 100%; border: none !important; background: transparent !important; box-shadow: none !important; border-radius: 10px !important; color: #74807B; font-weight: 600; padding: 10px 0; transition: all 0.3s; }
.swiss-segmented-control :deep(.el-radio-button.is-active .el-radio-button__inner) { background: white !important; color: #1C2421; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important; }

/* 底部操作按钮 */
.form-actions { margin-top: 32px; padding-top: 32px; border-top: 1px solid rgba(28, 36, 33, 0.06); display: flex; justify-content: flex-end; gap: 16px; }

.action-btn { height: 44px; padding: 0 24px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; border: none; }
.action-btn.primary { background: #2A483A; color: white; box-shadow: 0 4px 12px rgba(42, 72, 58, 0.2); }
.action-btn.primary:hover:not(:disabled) { background: #1C2421; transform: translateY(-1px); box-shadow: 0 6px 16px rgba(42, 72, 58, 0.3); }
.action-btn.primary:disabled { background: #A0AAB2; cursor: not-allowed; box-shadow: none; }
.action-btn.outline { background: white; color: #1C2421; border: 1px solid rgba(28, 36, 33, 0.1); }
.action-btn.outline:hover { background: #F8FAFC; border-color: #1C2421; }
.action-btn.ghost { background: transparent; color: #74807B; }
.action-btn.ghost:hover { background: rgba(28, 36, 33, 0.05); color: #1C2421; }

.is-loading { animation: rotating 2s linear infinite; }
@keyframes rotating { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* ========== 对话框深度美化 ========== */
:deep(.swiss-dialog) { background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(32px) saturate(180%); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 24px; box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.1); }
:deep(.swiss-dialog .el-dialog__header) { padding: 32px 32px 16px; margin: 0; }
:deep(.swiss-dialog .el-dialog__title) { font-size: 20px; font-weight: 700; color: #1C2421; }
:deep(.swiss-dialog .el-dialog__body) { padding: 0 32px; }
:deep(.swiss-dialog .el-dialog__footer) { padding: 24px 32px 32px; border-top: 1px solid rgba(28, 36, 33, 0.04); }
.dialog-actions { display: flex; justify-content: flex-end; gap: 12px; }

/* 响应式断点 */
@media (max-width: 900px) {
  .profile-glass-card { flex-direction: column; }
  .profile-sidebar { width: 100%; padding: 40px 24px 24px; border-bottom: 1px solid rgba(28, 36, 33, 0.06); }
  .vertical-separator { display: none; }
  .profile-content { padding: 32px 24px; }
  .form-grid { grid-template-columns: 1fr; }
  .full-width { grid-column: auto; }
}
</style>