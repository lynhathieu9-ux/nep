<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <el-icon :size="28"><Monitor /></el-icon>
        <span>NEP环保监督系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="feedback-group" v-if="role === 'NEPS' || role === 'NEPM'">
          <template #title>
            <el-icon><EditPen /></el-icon>
            <span>监督反馈</span>
          </template>
          <el-menu-item index="/feedback">反馈列表</el-menu-item>
          <el-menu-item index="/feedback/submit" v-if="role === 'NEPS'">提交反馈</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/aqi" v-if="role === 'NEPG' || role === 'NEPM'">
          <el-icon><DataAnalysis /></el-icon>
          <span>AQI检测</span>
        </el-menu-item>

        <el-menu-item index="/statistics" v-if="role === 'NEPM' || role === 'NEPV'">
          <el-icon><PieChart /></el-icon>
          <span>数据统计</span>
        </el-menu-item>

        <el-menu-item index="/ai">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI助手</span>
        </el-menu-item>

        <el-menu-item index="/admin/dashboard" v-if="role === 'NEPM'">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 头部 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" :src="avatarUrl">
                <el-icon v-if="!avatarUrl"><UserFilled /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.user?.realName || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item style="display:flex;align-items:center;gap:8px">
                  <el-tag :type="roleTagType" size="small">{{ roleName }}</el-tag>
                  <span style="font-size:12px;color:#999">{{ userStore.user?.phone || '' }}</span>
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>

      <!-- 底部 -->
      <el-footer class="layout-footer">
        <span>东软环保公众监督系统 NEP v1.0 | © 2026 Neusoft</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const role = computed(() => userStore.getRole())

const avatarUrl = computed(() => {
  const av = userStore.user?.avatar
  return av && av.length > 0 ? av : ''
})

const roleTagType = computed(() => {
  const map = { 'NEPS': 'primary', 'NEPG': 'success', 'NEPM': 'warning', 'NEPV': 'info' }
  return map[role.value] || 'primary'
})

const roleName = computed(() => {
  const map = { 'NEPS': '公众监督员', 'NEPG': '网格员', 'NEPM': '管理员', 'NEPV': '决策者' }
  return map[role.value] || '未知'
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  userStore.fetchUser()
})
</script>

<style scoped>
.layout-container { height: 100vh; }

.layout-aside {
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.el-menu { border-right: none; }

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.layout-main {
  background-color: #f0f2f5;
  min-height: calc(100vh - 120px);
  padding: 20px;
}

.layout-footer {
  text-align: center;
  color: #999;
  font-size: 13px;
  height: 50px;
  line-height: 50px;
  border-top: 1px solid #e6e6e6;
  background: #fff;
}
</style>
