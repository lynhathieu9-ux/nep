<template>
  <div class="register-page">
    <!-- 顶部 */
    <div class="top-bar">
      <router-link to="/login" class="back-link">
        <el-icon><ArrowLeft /></el-icon> 返回登录
      </router-link>
    </div>

    <!-- 主内容 -->
    <div class="register-body">
      <div class="register-card">
        <!-- 左侧引导 -->
        <div class="guide-side">
          <div class="guide-content">
            <div class="step-indicator">
              <div class="step active"><span>1</span></div>
              <div class="step-line"></div>
              <div class="step"><span>2</span></div>
              <div class="step-line"></div>
              <div class="step"><span>3</span></div>
            </div>
            <h2>注册成为公众监督员</h2>
            <p>只需三步，加入环保监督队伍</p>
            <div class="steps">
              <div class="step-text">
                <div class="step-num">01</div>
                <div>
                  <strong>填写基本信息</strong>
                  <p>输入手机号、设置密码</p>
                </div>
              </div>
              <div class="step-text">
                <div class="step-num">02</div>
                <div>
                  <strong>完善个人资料</strong>
                  <p>填写姓名、年龄、性别、邮箱</p>
                </div>
              </div>
              <div class="step-text">
                <div class="step-num">03</div>
                <div>
                  <strong>开始监督之旅</strong>
                  <p>选择网格区域，提交空气质量反馈</p>
                </div>
              </div>
            </div>
            <div class="guide-quote">
              "环境保护，人人有责。每一个公民都是环境的守护者。"
            </div>
          </div>
        </div>

        <!-- 右侧表单 -->
        <div class="form-side">
          <div class="form-container">
            <h2 class="form-title">创建新账号</h2>
            <p class="form-desc">注册后即可参与公众环保监督</p>

            <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleRegister">
              <div class="input-group">
                <label class="input-label">
                  <el-icon><Phone /></el-icon> 手机号
                </label>
                <el-input v-model="form.phone" placeholder="请输入您的手机号" size="large" class="custom-input" />
              </div>

              <div class="input-group">
                <label class="input-label">
                  <el-icon><Lock /></el-icon> 登录密码
                </label>
                <el-input v-model="form.password" type="password" placeholder="请设置密码（至少6位）" size="large" show-password class="custom-input" />
              </div>

              <div class="input-group">
                <label class="input-label">
                  <el-icon><User /></el-icon> 真实姓名
                </label>
                <el-input v-model="form.realName" placeholder="请输入您的真实姓名" size="large" class="custom-input" />
              </div>

              <div class="input-group">
                <label class="input-label">
                  <el-icon><Message /></el-icon> 电子邮箱
                </label>
                <el-input v-model="form.email" placeholder="请输入邮箱（用于接收通知）" size="large" class="custom-input" />
              </div>

              <div class="row-group">
                <div class="input-group" style="flex:1">
                  <label class="input-label">年龄</label>
                  <el-input-number v-model="form.age" :min="16" :max="120" size="large" style="width:100%" />
                </div>
                <div class="input-group" style="flex:1">
                  <label class="input-label">性别</label>
                  <div class="gender-group">
                    <div
                      class="gender-btn"
                      :class="{ active: form.gender === 1 }"
                      @click="form.gender = 1"
                    >👨 男</div>
                    <div
                      class="gender-btn"
                      :class="{ active: form.gender === 0 }"
                      @click="form.gender = 0"
                    >👩 女</div>
                  </div>
                </div>
              </div>

              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="handleRegister"
                class="submit-btn"
              >
                <span v-if="!submitting">立即注册</span>
                <span v-else>注册中...</span>
              </el-button>
            </el-form>

            <div class="form-footer">
              已有账号？<router-link to="/login" class="link">立即登录</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  phone: '', password: '', realName: '', email: '', age: 25, gender: 1
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

async function handleRegister() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await userStore.register({
      phone: form.value.phone, password: form.value.password,
      realName: form.value.realName, email: form.value.email,
      age: form.value.age, gender: form.value.gender
    })
    ElMessage.success('🎉 注册成功！欢迎加入环保监督队伍！')
    router.push('/login')
  } catch (e) {} finally { submitting.value = false }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 50%, #e0f2f1 100%);
}

.top-bar {
  padding: 20px 40px;
}

.back-link {
  color: #555;
  text-decoration: none;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: color 0.3s;
}

.back-link:hover { color: #409EFF; }

.register-body {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 40px 60px;
}

.register-card {
  display: flex;
  width: 1000px;
  min-height: 620px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.1);
  overflow: hidden;
  animation: cardIn 0.6s ease-out;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ========== 左侧引导 ========== */
.guide-side {
  width: 420px;
  background: linear-gradient(160deg, #0c8c3f 0%, #1a6b3a 40%, #0f4c2f 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.guide-side::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='40' height='40' viewBox='0 0 40 40' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='20' cy='20' r='1' fill='white' opacity='0.2'/%3E%3C/svg%3E");
  opacity: 0.5;
}

.guide-content {
  position: relative;
  z-index: 1;
  padding: 50px 40px;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 30px;
}

.step {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.3s;
}

.step.active {
  background: #fff;
  color: #0c8c3f;
  border-color: #fff;
  box-shadow: 0 0 20px rgba(255,255,255,0.3);
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(255,255,255,0.3);
}

.guide-content h2 {
  font-size: 26px;
  margin-bottom: 8px;
  text-align: center;
}

.guide-content > p {
  font-size: 14px;
  opacity: 0.7;
  text-align: center;
  margin-bottom: 40px;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 40px;
}
.step-text {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.step-num {
  font-size: 28px;
  font-weight: 800;
  opacity: 0.3;
  line-height: 1;
}
.step-text strong { font-size: 15px; display: block; margin-bottom: 4px; }
.step-text p { font-size: 13px; opacity: 0.7; margin: 0; }

.guide-quote {
  font-size: 13px;
  opacity: 0.6;
  font-style: italic;
  text-align: center;
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.15);
}

/* ========== 右侧表单 ========== */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-container { width: 380px; }

.form-title { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
.form-desc { font-size: 14px; color: #999; margin-bottom: 30px; }

.input-group { margin-bottom: 18px; }
.input-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  transition: all 0.3s;
}

.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409EFF inset;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64,158,255,0.3) inset;
}

.row-group { display: flex; gap: 16px; }

.gender-group { display: flex; gap: 10px; }
.gender-btn {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}
.gender-btn:hover { border-color: #409EFF; color: #409EFF; }
.gender-btn.active {
  background: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
  font-weight: 600;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 16px;
  background: linear-gradient(135deg, #0c8c3f 0%, #1a6b3a 100%);
  border: none;
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(12,140,63,0.4);
}

.form-footer { text-align: center; margin-top: 24px; font-size: 14px; color: #999; }
.link { color: #409EFF; font-weight: 600; text-decoration: none; }

@media (max-width: 900px) {
  .guide-side { display: none; }
  .register-card { width: 100%; }
}
</style>
