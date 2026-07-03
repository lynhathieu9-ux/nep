<template>
  <div class="alpine-admin-layout">
    
    <!-- 1. 灵动底色 -->
    <div class="ambient-bg"></div>

    <!-- 2. 内嵌式呼吸侧栏 (Solid Breathing Sidebar) -->
    <aside class="alpine-sidebar glass-panel" :class="{ 'is-collapsed': isCollapsed }">
      
      <div class="sidebar-header">
        <div class="brand-zone">
          <div class="brand-logo">
            <el-icon><Platform /></el-icon>
          </div>
          <span class="brand-text">NEP Admin</span>
        </div>
        <button class="collapse-btn" @click="toggleSidebar" :title="isCollapsed ? '展开菜单' : '收起菜单'">
          <el-icon><Fold v-if="!isCollapsed"/><Expand v-else/></el-icon>
        </button>
      </div>

      <nav class="sidebar-nav">
        <el-tooltip content="数据总控大屏" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/dashboard" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><Odometer /></el-icon>
            <span class="nav-text">数据总控大屏</span>
          </router-link>
        </el-tooltip>

        <el-tooltip content="深度统计洞察" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/statistics" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><DataAnalysis /></el-icon>
            <span class="nav-text">深度统计洞察</span>
          </router-link>
        </el-tooltip>

        <div class="nav-divider"></div>

        <el-tooltip content="系统账户与权限" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/users" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><User /></el-icon>
            <span class="nav-text">系统账户与权限</span>
          </router-link>
        </el-tooltip>

        <el-tooltip content="反馈工单管理" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/feedbacks" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><Tickets /></el-icon>
            <span class="nav-text">反馈工单管理</span>
          </router-link>
        </el-tooltip>

        <div class="nav-divider"></div>

        <el-tooltip content="环保资讯发布" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/news" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><Reading /></el-icon>
            <span class="nav-text">环保资讯发布</span>
          </router-link>
        </el-tooltip>

        <el-tooltip content="文献发布管理" placement="right" :disabled="!isCollapsed" popper-class="alpine-tooltip">
          <router-link to="/admin/knowledge" class="nav-item" active-class="is-active">
            <el-icon class="nav-icon"><Collection /></el-icon>
            <span class="nav-text">文献发布管理</span>
          </router-link>
        </el-tooltip>
      </nav>

      <div class="sidebar-footer">
        <el-dropdown trigger="click" placement="top-start" popper-class="alpine-admin-dropdown">
          <div class="profile-card">
            <el-avatar :size="40" class="admin-avatar" :src="userStore.user?.avatar">
              <el-icon v-if="!userStore.user?.avatar"><UserFilled /></el-icon>
            </el-avatar>
            <div class="profile-info">
              <span class="admin-name">{{ userName }}</span>
              <span class="admin-role">System Root</span>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navTo('/admin/profile')">
                <el-icon><Setting /></el-icon> 系统偏好设置
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout" class="danger-action">
                <el-icon><SwitchButton /></el-icon> 安全登出
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

    </aside>

    <!-- 3. 右侧核心舞台 (Main Canvas) -->
    <main class="main-canvas">
      
      <!-- 实时业务状态栏 -->
      <header class="utility-strip">
        <div class="strip-left">
          <div class="system-status">
            <div class="pulse-dot"></div>
            <span class="status-text">NEP Core Engine · 运行平稳</span>
          </div>
        </div>
        <div class="strip-right">
          <!-- 待处理任务指示器 -->
          <div class="indicator-group">
            <div class="indicator warn" @click="goToFeedbacks('PENDING')" :class="{ 'is-focus': route.path === '/admin/feedbacks' }" title="查看待指派反馈">
              <span class="indicator-count">{{ pendingCount }}</span>
              <span class="indicator-label">待指派</span>
            </div>
            <div class="indicator danger" @click="goToFeedbacks('ESCALATED')" :class="{ 'is-focus': route.path === '/admin/feedbacks' }" title="超时未处理的紧急工单">
              <span class="indicator-count">{{ escalatedCount }}</span>
              <span class="indicator-label">超时预警</span>
            </div>
            <div class="indicator" @click="navTo('/admin/users')" title="系统用户">
              <span class="indicator-count">{{ userCount }}</span>
              <span class="indicator-label">用户</span>
            </div>
          </div>
          <button class="utility-btn" title="系统通知" @click="navTo('/admin/notifications')">
            <el-icon><Bell /></el-icon>
          </button>
        </div>
      </header>

      <!-- 页面视口 -->
      <div class="viewport-wrapper">
        <router-view v-slot="{ Component, route }">
          <transition name="spatial-fade" mode="out-in">
            <div :key="route.path" class="viewport-content">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </div>

    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { getFeedbackOverview } from '@/api/feedback'
import { getUserStats } from '@/api/user'
import {
  Platform, Odometer, DataAnalysis, User, Tickets,
  Reading, Collection, UserFilled, Setting, SwitchButton,
  Fold, Expand, Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userName = computed(() => userStore.user?.realName || 'Administrator')

// 侧栏折叠控制
const isCollapsed = ref(false)
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const navTo = (path) => router.push(path)

// 跳转到反馈工单页并自动筛选（直接用 URL 参数驱动）
function goToFeedbacks(status) {
  router.push({ path: '/admin/feedbacks', query: { status } })
}

// 登出逻辑
const handleLogout = () => {
  ElMessageBox.confirm('确认要挂起管理员会话并登出系统吗？', '系统安全', {
    confirmButtonText: '安全登出',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'alpine-message-box'
  }).then(() => {
    localStorage.clear()
    router.push('/login')
  }).catch(() => {})
}

// ===== 实时业务指标 =====
const pendingCount = ref(0)     // 待指派反馈数
const escalatedCount = ref(0)  // 已升级工单数
const userCount = ref(0)       // 系统用户数
let refreshTimer = null

async function loadIndicators() {
  try {
    // 反馈总览
    const overview = await getFeedbackOverview()
    if (overview.data) {
      pendingCount.value = overview.data.pending || 0
      escalatedCount.value = overview.data.escalated || 0
    }
  } catch (e) { /* 静默 */ }
  try {
    // 用户统计
    const stats = await getUserStats()
    if (stats.data) {
      userCount.value = stats.data.total || 0
    }
  } catch (e) { /* 静默 */ }
}

onMounted(() => {
  if (!userStore.user) userStore.fetchUser()
  loadIndicators()
  // 每 30 秒刷新一次业务指标
  refreshTimer = setInterval(loadIndicators, 30_000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
/* ========== 1. 基础布局与底色 ========== */
.alpine-admin-layout {
  position: relative; width: 100vw; height: 100vh;
  background-color: #F4F6F5;
  overflow: hidden; display: flex; padding: 24px; gap: 24px;
  box-sizing: border-box; font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, sans-serif;
}
.ambient-bg {
  position: absolute; inset: 0; z-index: 0; pointer-events: none;
  background-image: radial-gradient(circle at 50% 0%, rgba(255,255,255,0.8) 0%, transparent 70%);
}

.glass-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 32px -12px rgba(28, 36, 33, 0.06), inset 0 2px 4px rgba(255, 255, 255, 0.6);
}

/* ========== 2. 内嵌式呼吸侧栏 ========== */
.alpine-sidebar {
  position: relative; z-index: 10; flex-shrink: 0;
  width: 280px; height: 100%; border-radius: 24px;
  display: flex; flex-direction: column;
  transition: width 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  overflow: hidden;
}

.alpine-sidebar.is-collapsed { width: 88px; }

.sidebar-header {
  height: 88px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid rgba(28, 36, 33, 0.04); flex-shrink: 0;
  transition: justify-content 0.4s;
}

.brand-zone { display: flex; align-items: center; gap: 12px; overflow: hidden; }
.brand-logo {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  background: linear-gradient(135deg, #1C2421 0%, #2A483A 100%); color: white;
  display: flex; justify-content: center; align-items: center; font-size: 20px;
  box-shadow: 0 4px 12px rgba(42, 72, 58, 0.2);
}
.brand-text {
  font-size: 18px; font-weight: 800; color: #1C2421; white-space: nowrap;
  max-width: 150px; opacity: 1; transition: max-width 0.3s ease, opacity 0.3s ease;
}
.is-collapsed .brand-zone { display: none; }
.is-collapsed .sidebar-header { justify-content: center; padding: 0; }

.collapse-btn {
  width: 36px; height: 36px; border-radius: 10px; border: none; background: rgba(28, 36, 33, 0.04);
  color: #74807B; display: flex; justify-content: center; align-items: center; font-size: 16px;
  cursor: pointer; transition: all 0.3s; flex-shrink: 0;
}
.collapse-btn:hover { background: rgba(28, 36, 33, 0.08); color: #1C2421; }
.is-collapsed .collapse-btn { width: 44px; height: 44px; font-size: 20px; border-radius: 12px; background: white; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

/* 侧栏导航 */
.sidebar-nav { flex: 1; padding: 24px 16px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; overflow-x: hidden; }
.sidebar-nav::-webkit-scrollbar { display: none; }
.nav-divider { height: 1px; background: rgba(28, 36, 33, 0.06); margin: 8px 12px; }

.nav-item {
  position: relative; height: 48px; border-radius: 14px;
  display: flex; align-items: center; padding: 0 16px; gap: 14px;
  color: #74807B; text-decoration: none; cursor: pointer;
  transition: color 0.3s ease, justify-content 0.3s; z-index: 1; overflow: hidden;
}
.nav-icon { font-size: 20px; flex-shrink: 0; transition: transform 0.3s; }
.nav-text {
  font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden;
  max-width: 200px; opacity: 1; transition: max-width 0.3s ease, opacity 0.3s ease;
}

.is-collapsed .nav-item { padding: 0; justify-content: center; }
.is-collapsed .nav-text { max-width: 0; opacity: 0; display: none; }
.nav-item:hover { color: #1C2421; }
.nav-item:hover .nav-icon { transform: scale(1.1); }

.nav-item::before {
  content: ''; position: absolute; inset: 0; border-radius: 14px; background: #1C2421; z-index: -1;
  transform: scaleX(0); transform-origin: left; transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.nav-item.is-active { color: white; }
.nav-item.is-active::before { transform: scaleX(1); }

/* 侧栏底部 */
.sidebar-footer { padding: 16px; border-top: 1px solid rgba(28, 36, 33, 0.04); transition: padding 0.3s; }
.is-collapsed .sidebar-footer { display: flex; justify-content: center; padding: 16px 8px; }

.profile-card {
  display: flex; align-items: center; gap: 12px; padding: 8px; border-radius: 16px;
  cursor: pointer; transition: background 0.3s; overflow: hidden; width: 100%; box-sizing: border-box;
}
.profile-card:hover { background: rgba(28, 36, 33, 0.04); }

.admin-avatar { flex-shrink: 0; border: 2px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.profile-info { 
  flex: 1; display: flex; flex-direction: column; white-space: nowrap; overflow: hidden;
  max-width: 150px; opacity: 1; transition: max-width 0.3s ease, opacity 0.3s ease;
}
.admin-name { font-size: 14px; font-weight: 700; color: #1C2421; overflow: hidden; text-overflow: ellipsis;}
.admin-role { font-size: 11px; color: #74807B; font-weight: 600; margin-top: 2px; }

.is-collapsed .profile-card { padding: 8px 0; justify-content: center; }
.is-collapsed .profile-info { max-width: 0; opacity: 0; display: none; }

/* ========== 3. 右侧舞台 ========== */
.main-canvas { flex: 1; display: flex; flex-direction: column; min-width: 0; position: relative; z-index: 10; }

/* 系统状态悬浮带 (无边界感设计) */
.utility-strip {
  height: 48px; flex-shrink: 0; display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; padding: 0 8px; /* 极简边距，让位于内部容器 */
}

.system-status { display: flex; align-items: center; gap: 8px; }
.status-text { font-size: 13px; font-weight: 600; color: #74807B; font-family: monospace; letter-spacing: 0.5px; }
.pulse-dot { width: 6px; height: 6px; border-radius: 50%; background: #2AA876; box-shadow: 0 0 0 0 rgba(42, 168, 118, 0.4); animation: pulse-green 2s infinite; }
@keyframes pulse-green { 0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(42, 168, 118, 0.4); } 70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(42, 168, 118, 0); } 100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(42, 168, 118, 0); } }

.strip-right { display: flex; align-items: center; gap: 20px; }

/* 实时业务指标胶囊 */
.indicator-group {
  display: flex; align-items: center; gap: 2px;
  background: rgba(28, 36, 33, 0.03);
  border: 1px solid rgba(28, 36, 33, 0.05);
  border-radius: 12px; padding: 2px;
}
.indicator {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 14px; border-radius: 10px; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  border: 1px solid transparent;
}
.indicator:hover {
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.indicator-count {
  font-size: 18px; font-weight: 700; color: #1C2421;
  font-variant-numeric: tabular-nums; line-height: 1;
}
.indicator-label {
  font-size: 11px; font-weight: 600; color: #74807B;
  letter-spacing: 0.3px;
}
.indicator.warn .indicator-count { color: #F5A623; }
.indicator.danger .indicator-count { color: #E11D48; }
.indicator.is-focus {
  background: rgba(255, 255, 255, 0.5);
  border-color: rgba(28, 36, 33, 0.06);
}

.utility-btn {
  width: 32px; height: 32px; border-radius: 8px; border: none; background: transparent;
  color: #74807B; font-size: 18px; display: flex; justify-content: center; align-items: center;
  cursor: pointer; transition: all 0.3s;
}
.utility-btn:hover { background: rgba(28, 36, 33, 0.05); color: #1C2421; }

/* 画板容器：完全释放边框，由路由子组件全权控制玻璃态和内边距 */
.viewport-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; }
.viewport-content { flex: 1; height: 100%; display: flex; flex-direction: column; }

/* 空间转场动画 */
.spatial-fade-enter-active, .spatial-fade-leave-active { transition: opacity 0.4s ease, transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); }
.spatial-fade-enter-from { opacity: 0; transform: translateY(16px) scale(0.99); }
.spatial-fade-leave-to { opacity: 0; transform: translateY(-16px) scale(0.99); }

</style>

<style>
/* 悬浮菜单覆盖 */
.alpine-admin-dropdown.el-dropdown-menu {
  background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(24px) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important; border-radius: 16px !important;
  padding: 8px !important; box-shadow: 0 16px 40px -8px rgba(28, 36, 33, 0.15) !important;
}
.alpine-admin-dropdown .el-dropdown-menu__item { border-radius: 10px; font-size: 14px; padding: 10px 16px; font-weight: 500; color: #1C2421; transition: all 0.2s; }
.alpine-admin-dropdown .el-dropdown-menu__item:hover { background: #F4F6F5 !important; font-weight: 600; }
.alpine-admin-dropdown .danger-action { color: #E11D48 !important; }
.alpine-admin-dropdown .danger-action:hover { background: #FFF1F2 !important; color: #E11D48 !important; }

/* 模态框 */
.alpine-message-box { border-radius: 24px !important; border: 1px solid rgba(255, 255, 255, 0.9) !important; background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(40px) !important; box-shadow: 0 32px 64px -16px rgba(28, 36, 33, 0.15) !important; }
.alpine-message-box .el-message-box__title { font-weight: 700; color: #1C2421; }
</style>