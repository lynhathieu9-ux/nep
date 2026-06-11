<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-overlay"></div>
      <div class="brand-content">
        <div class="brand-icon">🌿</div>
        <h1 class="brand-title">东软环保公众监督系统</h1>
        <p class="brand-subtitle">NEP · Neusoft Environmental Protection</p>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon"><el-icon><Monitor /></el-icon></div>
            <span>实时空气质量监测</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon><DataAnalysis /></el-icon></div>
            <span>大数据智能分析</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><el-icon><Share /></el-icon></div>
            <span>公众监督共治共享</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-side">
      <div class="form-container">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录您的账号继续使用</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
          <div class="input-group">
            <label class="input-label">手机号</label>
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              :prefix-icon="Phone"
              size="large"
              class="custom-input"
            />
          </div>

          <div class="input-group">
            <label class="input-label">密码</label>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              class="custom-input"
            />
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="submit-btn"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>

        <!-- 快速测试 -->
        <div class="quick-login">
          <p class="quick-title">快速体验</p>
          <div class="quick-btns">
            <el-button size="small" round @click="quickLogin('admin', '123456')">👨‍💼 管理员</el-button>
            <el-button size="small" round @click="quickLogin('13900139000', '123456')">👤 监督员</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 装饰粒子 -->
    <div class="particles">
      <span v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Phone, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

const form = ref({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$|^admin$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

function particleStyle(i) {
  return {
    left: Math.random() * 100 + '%',
    top: Math.random() * 100 + '%',
    width: (Math.random() * 60 + 20) + 'px',
    animationDelay: Math.random() * 8 + 's',
    animationDuration: (Math.random() * 6 + 6) + 's',
    opacity: Math.random() * 0.15 + 0.05
  }
}

async function quickLogin(phone, password) {
  form.value = { phone, password }
  await handleLogin()
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.value.phone, form.value.password)
    ElMessage.success('登录成功，欢迎回来！')
    router.push('/home')
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
  position: relative;
  background: #f0f2f5;
}

/* ========== 左侧品牌区 ========== */
.brand-side {
  flex: 1;
  background: linear-gradient(135deg, #0c8c3f 0%, #1a6b3a 30%, #0f4c2f 70%, #0a2e1c 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.3;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.brand-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 14px;
  opacity: 0.7;
  letter-spacing: 4px;
  margin-bottom: 50px;
  text-transform: uppercase;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
  align-items: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 15px;
  opacity: 0.85;
  transition: all 0.3s;
}

.feature-item:hover {
  opacity: 1;
  transform: translateX(5px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  backdrop-filter: blur(10px);
}

/* ========== 右侧表单区 ========== */
.form-side {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  box-shadow: -4px 0 30px rgba(0,0,0,0.08);
}

.form-container {
  width: 380px;
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.form-header p {
  font-size: 14px;
  color: #999;
}

.input-group {
  margin-bottom: 24px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  transition: all 0.3s;
  padding: 4px 12px;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409EFF inset;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64,158,255,0.3) inset;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 8px;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  border: none;
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(64,158,255,0.4);
}

.form-footer {
  text-align: center;
  margin-top: 30px;
  font-size: 14px;
  color: #999;
}

.link {
  color: #409EFF;
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
  transition: color 0.3s;
}

.link:hover { color: #337ecc; }

/* 快速体验 */
.quick-login {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.quick-title {
  font-size: 12px;
  color: #bbb;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.quick-btns {
  display: flex;
  gap: 10px;
  justify-content: center;
}

/* 粒子 */
.particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64,158,255,0.3) 0%, transparent 70%);
  animation: particleDrift linear infinite;
}

@keyframes particleDrift {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: 0.1; }
  90% { opacity: 0.1; }
  100% { transform: translateY(-100vh) scale(0.5); opacity: 0; }
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  .brand-side { display: none; }
  .form-side { width: 100%; }
}
</style>
