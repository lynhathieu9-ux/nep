<template>
  <div class="alpine-command-layout">
    <div class="ambient-background">
      <div class="ambient-glow glow-azure"></div>
      <div class="ambient-glow glow-slate"></div>
      <div class="ambient-glow glow-ice"></div>
      <div class="fine-grain-overlay"></div>
    </div>

    <aside class="glass-island command-sidebar">
      <div class="brand-zone">
        <div class="brand-icon-box">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-title">NEP Grid</span>
          <span class="brand-subtitle">网格员中枢</span>
        </div>
      </div>

      <div class="user-profile-card">
        <el-avatar :size="48" class="profile-avatar" :src="userStore.user?.avatar">
          <el-icon v-if="!userStore.user?.avatar"><UserFilled /></el-icon>
        </el-avatar>
        <div class="profile-info">
          <span class="profile-name">{{ userName }}</span>
          <span class="profile-role">
            <el-icon><LocationInformation /></el-icon>
            {{ workArea }}
          </span>
        </div>
        <el-dropdown trigger="click" placement="bottom-end" popper-class="alpine-dropdown">
          <button class="action-btn outline"><el-icon><Setting /></el-icon></button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item class="info-row" disabled>编号: {{ employeeCode }}</el-dropdown-item>
              <el-dropdown-item divided @click="$router.push('/nepg/profile')">个人设定</el-dropdown-item>
              <el-dropdown-item @click="handleLogout" class="danger-action">退出系统</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <nav class="navigation-menu">
        <div class="nav-group-title">核心工作</div>
        <router-link to="/nepg/home" class="nav-item" active-class="is-active">
          <el-icon><Odometer /></el-icon>
          <span>工作台概览</span>
        </router-link>
        <router-link to="/nepg/tasks" class="nav-item" active-class="is-active">
          <el-icon><DocumentChecked /></el-icon>
          <span>检测任务</span>
          <div v-if="taskCount > 0" class="minimal-badge">{{ taskCount }}</div>
        </router-link>
        <router-link to="/nepg/records" class="nav-item" active-class="is-active">
          <el-icon><DataLine /></el-icon>
          <span>历史记录</span>
        </router-link>

        <div class="nav-divider"></div>

        <div class="nav-group-title">知识与洞察</div>
        <router-link to="/nepg/news" class="nav-item" active-class="is-active">
          <el-icon><Message /></el-icon>
          <span>环保资讯</span>
        </router-link>
        <router-link to="/nepg/knowledge" class="nav-item" active-class="is-active">
          <el-icon><Notebook /></el-icon>
          <span>专业库</span>
        </router-link>
        <router-link to="/nepg/ai" class="nav-item" active-class="is-active">
          <el-icon><Cpu /></el-icon>
          <span>AI 分析助手</span>
        </router-link>
      </nav>
    </aside>

    <div class="glass-island workspace-main">
      <header class="utility-header">
        <div class="header-left">
          <div class="status-indicator">
            <div class="pulse-dot alpine-pulse"></div>
            <span class="status-message">数据同步实时连接中</span>
          </div>
        </div>

        <div class="header-right">
          <div class="date-display">{{ currentDate }}</div>
          <div class="vertical-separator"></div>
          <button class="action-btn ghost" @click="$router.push('/nepg/notifications')">
            <el-icon><Bell /></el-icon>
            <div v-if="unreadCount > 0" class="dot-badge"></div>
          </button>
        </div>
      </header>

      <main class="viewport-canvas">
        <router-view v-slot="{ Component, route }">
          <transition name="alpine-fade" mode="out-in">
            <div :key="route.path" class="viewport-content">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notification'
import { getAssignedStats } from '@/api/feedback'
import { ElMessageBox } from 'element-plus'
import {
  Monitor, UserFilled, LocationInformation, Setting,
  Odometer, DocumentChecked, DataLine, Message,
  Notebook, Cpu, Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const userName = computed(() => userStore.user?.realName || '网格专员')
const employeeCode = ref(localStorage.getItem('employeeCode') || 'GRID-0000')
const unreadCount = ref(0)
const taskCount = ref(0)
const workArea = ref('中心管辖区')
const currentDate = ref(new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }))

let timer = null

async function fetchData() {
  const uid = localStorage.getItem('userId')
  if (!uid) return
  try {
    const [notifRes, taskRes] = await Promise.all([
      getUnreadCount(Number(uid)),
      getAssignedStats()
    ])
    unreadCount.value = notifRes.data?.unreadCount || 0
    // 待检测任务数（网格员专属统计，仅本人）
    taskCount.value = taskRes.data?.assigned || 0
  } catch (e) {}
}

const handleLogout = () => {
  ElMessageBox.confirm('安全退出 NEP 指挥系统？', '操作确认', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'alpine-message-box'
  }).then(() => {
    localStorage.clear()
    router.push('/login')
  }).catch(() => {})
}

onMounted(() => {
  userStore.fetchUser()
  fetchData()
  timer = setInterval(fetchData, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ========== Design Variables (Alpine Edition) ========== */
:root {
  --alpine-bg: #F8FAFC;
  --alpine-text-main: #0F172A;
  --alpine-text-muted: #64748B;
  --alpine-primary: #0284C7; /* Azure */
  --alpine-primary-light: #E0F2FE;
  
  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(255, 255, 255, 1);
  --glass-shadow: 0 10px 40px -10px rgba(15, 23, 42, 0.06);
}

/* ========== Global Layout ========== */
.alpine-command-layout {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #F8FAFC;
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, Arial, sans-serif;
  color: #0F172A;
  display: flex;
  padding: 24px; /* The perfect outer breathing room */
  gap: 24px;     /* Space between the two islands */
}

/* ========== 1. Ambient Background ========== */
.ambient-background { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.ambient-glow {
  position: absolute; filter: blur(140px); opacity: 0.6;
  animation: alpine-drift 25s infinite alternate cubic-bezier(0.4, 0, 0.6, 1);
}
.glow-azure { width: 50vw; height: 50vw; background: rgba(14, 165, 233, 0.12); top: -10%; left: -5%; }
.glow-slate { width: 45vw; height: 45vw; background: rgba(100, 116, 139, 0.1); bottom: -10%; right: -5%; animation-delay: -8s; }
.glow-ice { width: 50vw; height: 50vw; background: rgba(241, 245, 249, 0.8); top: 30%; left: 30%; animation-delay: -15s; }

@keyframes alpine-drift {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(2%, 3%) scale(1.05); }
}

.fine-grain-overlay {
  position: absolute; inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.02'/%3E%3C/svg%3E");
}

/* ========== Shared Glass Island ========== */
.glass-island {
  position: relative;
  z-index: 10;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow: 0 12px 32px -8px rgba(15, 23, 42, 0.05), inset 0 2px 4px rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
}

/* ========== 2. Command Sidebar ========== */
.command-sidebar {
  width: 280px;
  flex-shrink: 0;
  padding: 32px 24px;
}

.brand-zone { display: flex; align-items: center; gap: 16px; margin-bottom: 40px; }
.brand-icon-box { 
  width: 40px; height: 40px; background: #0284C7; color: white; 
  border-radius: 12px; display: flex; justify-content: center; align-items: center;
  font-size: 20px; box-shadow: 0 4px 12px rgba(2, 132, 199, 0.2);
}
.brand-text { display: flex; flex-direction: column; }
.brand-title { font-size: 18px; font-weight: 700; letter-spacing: 0.5px; }
.brand-subtitle { font-size: 12px; color: #64748B; font-weight: 500; letter-spacing: 1px; }

.user-profile-card {
  display: flex; align-items: center; gap: 12px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 16px; border-radius: 16px; margin-bottom: 40px;
  box-shadow: 0 4px 16px -4px rgba(15, 23, 42, 0.03);
}
.profile-avatar { background: #E2E8F0; color: #64748B; }
.profile-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.profile-name { font-size: 15px; font-weight: 600; }
.profile-role { font-size: 12px; color: #64748B; display: flex; align-items: center; gap: 4px; }

.navigation-menu { display: flex; flex-direction: column; gap: 6px; }
.nav-group-title { font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 1px; margin: 16px 0 8px 12px; }
.nav-divider { height: 1px; background: rgba(15, 23, 42, 0.06); margin: 16px 12px; }

.nav-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px; border-radius: 12px;
  text-decoration: none; color: #64748B; font-size: 14px; font-weight: 500;
  transition: all 0.3s ease; position: relative;
}
.nav-item:hover { color: #0F172A; background: rgba(241, 245, 249, 0.6); }
.nav-item.is-active {
  color: #0284C7; background: #E0F2FE; font-weight: 600;
  box-shadow: inset 0 2px 4px rgba(255,255,255,0.5);
}
.minimal-badge {
  margin-left: auto; background: #0284C7; color: white;
  font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 10px;
}

/* ========== 3. Workspace Main ========== */
.workspace-main {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.utility-header {
  height: 64px; border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 32px; flex-shrink: 0;
}
.header-left, .header-right { display: flex; align-items: center; gap: 20px; }

.status-indicator { display: flex; align-items: center; gap: 8px; }
.alpine-pulse {
  width: 6px; height: 6px; border-radius: 50%; background-color: #10B981; /* Emerald */
  animation: alpine-pulse-anim 3s infinite;
}
@keyframes alpine-pulse-anim {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}
.status-message { font-size: 13px; color: #64748B; font-weight: 500; }
.date-display { font-size: 13px; color: #64748B; font-weight: 500; }
.vertical-separator { width: 1px; height: 16px; background: rgba(15, 23, 42, 0.1); }

.action-btn {
  width: 36px; height: 36px; border-radius: 10px; border: none; cursor: pointer;
  display: flex; justify-content: center; align-items: center; font-size: 18px;
  transition: all 0.3s; position: relative;
}
.action-btn.outline { background: transparent; border: 1px solid rgba(15, 23, 42, 0.1); color: #64748B; }
.action-btn.outline:hover { background: white; color: #0F172A; border-color: rgba(15, 23, 42, 0.2); }
.action-btn.ghost { background: transparent; color: #64748B; }
.action-btn.ghost:hover { background: rgba(15, 23, 42, 0.04); color: #0F172A; }
.dot-badge { position: absolute; top: 8px; right: 8px; width: 6px; height: 6px; background: #EF4444; border-radius: 50%; }

/* 画板区域 */
.viewport-canvas {
  flex: 1; padding: 32px; overflow-y: auto; overflow-x: hidden;
}
.viewport-canvas::-webkit-scrollbar { display: none; }
.viewport-content { height: 100%; }

/* Transitions */
.alpine-fade-enter-active, .alpine-fade-leave-active { transition: opacity 0.4s ease, transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); }
.alpine-fade-enter-from { opacity: 0; transform: translateY(8px); }
.alpine-fade-leave-to { opacity: 0; transform: translateY(-8px); }
</style>

<style>
/* 全局覆盖样式注入 */
.alpine-dropdown.el-dropdown-menu {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(40px) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 16px !important;
  padding: 8px !important;
  box-shadow: 0 16px 32px -8px rgba(15, 23, 42, 0.1) !important;
}
.alpine-dropdown .info-row { padding: 4px 16px 8px; font-size: 12px; color: #94A3B8; font-weight: 600; }
.alpine-dropdown .el-dropdown-menu__item { border-radius: 8px; font-size: 14px; padding: 10px 16px; font-weight: 500; color: #0F172A; }
.alpine-dropdown .el-dropdown-menu__item:hover { background: #F1F5F9 !important; color: #0F172A !important; }
.alpine-dropdown .danger-action:hover { background: #FEF2F2 !important; color: #EF4444 !important; }

.alpine-message-box {
  border-radius: 20px !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(40px) !important;
  box-shadow: 0 24px 48px -12px rgba(15, 23, 42, 0.15) !important;
}
</style>