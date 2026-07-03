<template>
  <div class="swiss-spa-layout">
    <div class="ambient-background">
      <div class="ambient-glow glow-sage"></div>
      <div class="ambient-glow glow-mist"></div>
      <div class="ambient-glow glow-sand"></div>
      <div class="fine-grain-overlay"></div>
    </div>

    <header class="crystal-topbar">
      <div class="topbar-section left-section">
        <div class="brand-mark">
          <el-icon class="brand-icon"><Platform /></el-icon>
        </div>
        <span class="brand-name">NEP 环保中枢</span>
      </div>

      <div class="topbar-section center-section">
        <div class="status-indicator">
          <div class="pulse-dot"></div>
          <span class="status-message">系统运行稳定</span>
        </div>
      </div>

      <div class="topbar-section right-section">
        <button class="icon-action-btn"><el-icon><Search /></el-icon></button>
        <button class="icon-action-btn notification-btn" @click="$router.push('/ne/notifications')">
          <el-icon><Bell /></el-icon>
          <div class="notification-badge"></div>
        </button>
        
        <div class="separator"></div>

        <!-- 个人中心 · iOS 风格悬浮面板 -->
        <div class="user-menu-anchor" ref="userMenuAnchor">
          <div class="profile-capsule" @click="showUserMenu = !showUserMenu" :class="{ 'is-active': showUserMenu }">
            <span class="profile-name">{{ userStore.user?.realName || '用户' }}</span>
            <el-avatar :size="30" class="profile-avatar" :src="avatarUrl">
              <el-icon v-if="!avatarUrl"><UserFilled /></el-icon>
            </el-avatar>
            <el-icon class="chevron-icon" :class="{ 'is-open': showUserMenu }"><ArrowDown /></el-icon>
          </div>

          <!-- 遮罩层（点击关闭） -->
          <transition name="menu-fade">
            <div v-if="showUserMenu" class="menu-backdrop" @click="showUserMenu = false"></div>
          </transition>

          <!-- iOS 风格悬浮面板 -->
          <transition name="menu-panel" @after-leave="onMenuClosed">
            <div v-if="showUserMenu" class="ios-panel" @click.stop>
              <!-- 顶部三角指示器 -->
              <div class="panel-arrow"></div>

              <!-- 用户信息卡片（紧凑横排） -->
              <div class="panel-user-card" @click="navigateTo('/ne/profile')">
                <div class="user-card-avatar">
                  <el-avatar :size="48" :src="avatarUrl" class="card-avatar-img">
                    <el-icon :size="24"><UserFilled /></el-icon>
                  </el-avatar>
                  <div class="online-dot"></div>
                </div>
                <div class="user-card-info">
                  <div class="user-card-name">{{ userStore.user?.realName || '用户' }}</div>
                  <div class="user-card-meta">
                    <span class="user-card-role">{{ roleName }}</span>
                    <span class="user-card-phone">{{ userStore.user?.phone || '' }}</span>
                  </div>
                </div>
                <el-icon class="card-chevron"><ArrowRight /></el-icon>
              </div>

              <!-- 分隔线 -->
              <div class="panel-separator"></div>

              <!-- 快捷数据概览（点击跳转我的反馈） -->
              <div class="panel-stats-row" @click="navigateTo('/ne/feedbacks')">
                <div class="stat-item">
                  <span class="stat-value">{{ myStats.total || 0 }}</span>
                  <span class="stat-label">全部反馈</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value pending">{{ myStats.pending || 0 }}</span>
                  <span class="stat-label">待处理</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value completed">{{ myStats.completed || 0 }}</span>
                  <span class="stat-label">已完成</span>
                </div>
              </div>

              <!-- 分隔线 -->
              <div class="panel-separator"></div>

              <!-- 底部操作 -->
              <div class="panel-footer">
                <div class="sign-out-btn" @click="handleLogout">
                  <el-icon :size="15"><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <aside class="minimal-dock">
      <nav class="dock-navigation">
        <el-tooltip
          v-for="item in dynamicMenuItems"
          :key="item.path"
          :content="item.title"
          placement="right"
          :show-after="300"
          popper-class="swiss-tooltip"
        >
          <router-link :to="item.path" class="dock-action" active-class="is-active">
            <div class="dock-icon-box">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
          </router-link>
        </el-tooltip>
      </nav>
    </aside>

    <main class="pristine-canvas">
      <div class="canvas-viewport">
        <router-view v-slot="{ Component, route }">
          <transition name="seamless-fade" mode="out-in">
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
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { getMyFeedbackStats } from '@/api/feedback'
import {
  Platform, HomeFilled, Document, EditPen,
  ChatDotRound, Bell, Search, UserFilled,
  Reading, Collection, ArrowDown, ArrowRight, SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 角色直接写死为公众监督员
const roleName = '公众监督员'

// 面板显示状态
const showUserMenu = ref(false)
const userMenuAnchor = ref(null)

// 统计数据
const myStats = ref({ total: 0, pending: 0, completed: 0 })

const avatarUrl = computed(() => {
  const av = userStore.user?.avatar
  return av && av.length > 0 ? av : ''
})

// 菜单项简化，专属于公众监督员，路径匹配 index.js 里的子路由
const dynamicMenuItems = computed(() => {
  return [
    { path: '/ne/home', title: '首页概览', icon: HomeFilled },
    { path: '/ne/submit', title: '提交反馈', icon: EditPen },
    { path: '/ne/feedbacks', title: '我的反馈', icon: Document },
    { path: '/ne/news', title: '环保新闻', icon: Reading },
    { path: '/ne/knowledge', title: '环保知识', icon: Collection },
    { path: '/ne/ai', title: 'AI助手', icon: ChatDotRound }
  ]
})

// 导航跳转 + 关闭面板
function navigateTo(path) {
  showUserMenu.value = false
  router.push(path)
}

function onMenuClosed() {
  // 面板关闭后的清理工作（如有需要）
}

// 点击外部区域关闭面板
function handleClickOutside(e) {
  if (showUserMenu.value && userMenuAnchor.value) {
    if (!userMenuAnchor.value.contains(e.target)) {
      showUserMenu.value = false
    }
  }
}

// 按 ESC 键关闭面板
function handleKeydown(e) {
  if (e.key === 'Escape' && showUserMenu.value) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  userStore.fetchUser()
  document.addEventListener('click', handleClickOutside, true)
  document.addEventListener('keydown', handleKeydown)
  loadMyStats()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
  document.removeEventListener('keydown', handleKeydown)
})

// 监听面板打开时刷新数据
watch(showUserMenu, (val) => {
  if (val) loadMyStats()
})

async function loadMyStats() {
  try {
    const res = await getMyFeedbackStats()
    myStats.value = res.data || { total: 0, pending: 0, completed: 0 }
  } catch (e) {
    // 静默降级
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出 NEP 环保系统吗？', '退出确认', {
    confirmButtonText: '退出登录',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'swiss-message-box'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
/* ========== Design System Variables ========== */
:root {
  --color-surface: #F4F6F5;
  --color-text-primary: #1C2421;
  --color-text-secondary: #74807B;
  --color-accent: #2A483A;
  
  --glass-bg: rgba(255, 255, 255, 0.65);
  --glass-border: rgba(255, 255, 255, 0.8);
  --glass-shadow: 0 8px 32px -8px rgba(28, 36, 33, 0.05), 0 4px 16px -4px rgba(28, 36, 33, 0.03);
  
  --layout-spacing: 24px;
  --topbar-height: 56px;
  --dock-width: 64px;
}

/* ========== Global Layout ========== */
.swiss-spa-layout {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #F4F6F5;
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1C2421;
}

/* ========== 1. Ambient Background ========== */
.ambient-background { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.ambient-glow {
  position: absolute; filter: blur(140px); opacity: 0.5;
  animation: ethereal-drift 30s infinite alternate cubic-bezier(0.4, 0, 0.6, 1);
}
.glow-sage { width: 55vw; height: 55vw; background: rgba(205, 222, 214, 0.8); top: -10%; left: -5%; }
.glow-mist { width: 45vw; height: 45vw; background: rgba(228, 233, 237, 0.8); bottom: -10%; right: -5%; animation-delay: -10s; }
.glow-sand { width: 50vw; height: 50vw; background: rgba(240, 236, 228, 0.6); top: 25%; left: 25%; animation-delay: -20s; }

@keyframes ethereal-drift {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(3%, 4%) scale(1.05); }
}

.fine-grain-overlay {
  position: absolute; inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.015'/%3E%3C/svg%3E");
}

/* ========== 2. Crystal Topbar (The Refined Island) ========== */
.crystal-topbar {
  position: absolute;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  height: 56px;
  
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(40px) saturate(160%);
  -webkit-backdrop-filter: blur(40px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-bottom-color: rgba(255, 255, 255, 0.5);
  border-radius: 28px;
  box-shadow: 0 12px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.8);
  z-index: 100;
  
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 0 24px;
  gap: 32px;
  transition: box-shadow 0.4s ease;
}

.crystal-topbar:hover { box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.06), inset 0 2px 4px rgba(255, 255, 255, 0.9); }

.topbar-section { display: flex; align-items: center; }

.left-section { gap: 12px; }
.brand-mark { color: #2A483A; font-size: 20px; display: flex; }
.brand-name { font-size: 15px; font-weight: 600; letter-spacing: 0.5px; color: #1C2421; }

.center-section { padding: 0 24px; border-left: 1px solid rgba(28, 36, 33, 0.08); border-right: 1px solid rgba(28, 36, 33, 0.08); }
.status-indicator { display: flex; align-items: center; gap: 8px; }
.pulse-dot {
  width: 6px; height: 6px; border-radius: 50%; background-color: #2A483A;
  box-shadow: 0 0 0 0 rgba(42, 72, 58, 0.4);
  animation: gentle-pulse 3s infinite;
}
@keyframes gentle-pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(42, 72, 58, 0.4); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(42, 72, 58, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(42, 72, 58, 0); }
}
.status-message { font-size: 13px; font-weight: 500; color: #74807B; letter-spacing: 0.3px; }

.right-section { gap: 4px; }
.icon-action-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none; background: transparent;
  display: flex; justify-content: center; align-items: center;
  font-size: 16px; color: #74807B; cursor: pointer; transition: all 0.3s ease; position: relative;
}
.icon-action-btn:hover { background: rgba(28, 36, 33, 0.04); color: #1C2421; }

.notification-badge {
  position: absolute; top: 8px; right: 8px; width: 6px; height: 6px;
  background-color: #D9534F; border-radius: 50%; border: 1.5px solid #FFF;
}

.separator { width: 1px; height: 16px; background-color: rgba(28, 36, 33, 0.1); margin: 0 8px; }

.profile-capsule {
  display: flex; align-items: center; gap: 10px;
  padding: 4px 4px 4px 16px; border-radius: 20px;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  user-select: none; border: 1.5px solid transparent;
}
.profile-capsule:hover { background: rgba(28, 36, 33, 0.04); }
.profile-capsule.is-active {
  background: rgba(28, 36, 33, 0.05);
  border-color: rgba(28, 36, 33, 0.06);
  box-shadow: 0 0 0 3px rgba(28, 36, 33, 0.03);
}
.profile-name { font-size: 14px; font-weight: 500; color: #1C2421; }
.profile-avatar { background: #E4E9ED; color: #74807B; border: 1.5px solid #FFF; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.chevron-icon {
  font-size: 12px; color: #74807B; transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1);
  margin-left: -2px;
}
.chevron-icon.is-open { transform: rotate(180deg); }

/* ========== 3. Minimalist Dock ========== */
.minimal-dock {
  position: absolute;
  top: 50%;
  left: 24px;
  transform: translateY(-50%);
  z-index: 100;
  
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(40px) saturate(160%);
  -webkit-backdrop-filter: blur(40px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 32px;
  box-shadow: 0 12px 32px -8px rgba(0, 0, 0, 0.04), inset 0 2px 4px rgba(255, 255, 255, 0.6);
  
  padding: 12px 10px;
}

.dock-navigation { display: flex; flex-direction: column; gap: 8px; }

.dock-action {
  width: 44px; height: 44px;
  display: flex; justify-content: center; align-items: center;
  text-decoration: none; outline: none; position: relative;
}

.dock-icon-box {
  width: 40px; height: 40px; border-radius: 20px;
  display: flex; justify-content: center; align-items: center;
  font-size: 18px; color: #74807B;
  background: transparent;
  transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.dock-action:hover .dock-icon-box { color: #1C2421; background: rgba(255, 255, 255, 0.6); transform: scale(1.05); }

.dock-action.is-active .dock-icon-box {
  background: #FFFFFF; color: #2A483A;
  box-shadow: 0 4px 12px rgba(28, 36, 33, 0.06), inset 0 1px 2px rgba(255, 255, 255, 1);
}

/* ========== 4. Pristine Canvas ========== */
.pristine-canvas {
  position: absolute;
  top: calc(24px + 56px + 32px);
  left: calc(24px + 64px + 32px);
  right: 48px;
  bottom: 32px;
  z-index: 50;
  display: flex; flex-direction: column;
}

.canvas-viewport { flex: 1; overflow-y: auto; overflow-x: hidden; scrollbar-width: none; }
.canvas-viewport::-webkit-scrollbar { display: none; }

.viewport-content { height: 100%; }

/* Seamless Transition */
.seamless-fade-enter-active, .seamless-fade-leave-active { transition: opacity 0.6s ease, transform 0.6s cubic-bezier(0.2, 0.8, 0.2, 1); }
.seamless-fade-enter-from { opacity: 0; transform: translateY(10px); }
.seamless-fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>

<style>
/* ========== iOS 毛玻璃个人中心悬浮面板 ========== */
.user-menu-anchor { position: relative; }

/* 透明遮罩层（点按即关） */
.menu-backdrop {
  position: fixed; inset: 0; z-index: 999;
}

/* ===== 面板主体 — 真·毛玻璃 ===== */
.ios-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 288px;
  z-index: 1001;

  /* 毛玻璃核心：厚白底 + 强高斯模糊 + 色彩饱和 */
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(64px) saturate(220%);
  -webkit-backdrop-filter: blur(64px) saturate(220%);

  /* 微妙的边框与内发光，模拟玻璃边缘 */
  border: 0.5px solid rgba(255, 255, 255, 0.85);
  border-radius: 22px;

  /* 内发光模拟 iOS 玻璃面板的亮边 */
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.7),
    0 0 0 1px rgba(0, 0, 0, 0.03),
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 12px 40px rgba(0, 0, 0, 0.12),
    0 24px 64px rgba(0, 0, 0, 0.06);

  overflow: hidden;
  padding: 6px 0;
}

/* 三角箭头（继承毛玻璃） */
.panel-arrow {
  position: absolute; top: -6px; right: 26px;
  width: 12px; height: 12px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(64px) saturate(220%);
  -webkit-backdrop-filter: blur(64px) saturate(220%);
  border: 0.5px solid rgba(255, 255, 255, 0.85);
  border-right: none; border-bottom: none;
  transform: rotate(45deg);
  border-radius: 2px 0 0 0;
  clip-path: polygon(0 0, 100% 0, 0 100%);
}

/* ===== 用户信息卡片（紧凑横排，整行可点） ===== */
.panel-user-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-radius: 16px; margin: 2px 8px;
}
.panel-user-card:hover { background: rgba(0, 0, 0, 0.03); }
.panel-user-card:active { background: rgba(0, 0, 0, 0.06); }

.user-card-avatar { position: relative; flex-shrink: 0; }
.card-avatar-img {
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.online-dot {
  position: absolute; bottom: 1px; right: 1px;
  width: 11px; height: 11px; border-radius: 50%;
  background: #34C759; border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 2px rgba(52, 199, 89, 0.15);
  animation: dotPulse 2.5s infinite;
}
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(52, 199, 89, 0.15); }
  50% { box-shadow: 0 0 0 5px rgba(52, 199, 89, 0); }
}

.user-card-info { flex: 1; min-width: 0; }
.user-card-name {
  font-size: 16px; font-weight: 700; color: #1C2421;
  letter-spacing: 0.2px; line-height: 1.3;
}
.user-card-meta { display: flex; align-items: center; gap: 10px; margin-top: 3px; }
.user-card-role {
  font-size: 11px; font-weight: 600; color: #2AA876;
  background: rgba(42, 168, 118, 0.1); padding: 2px 8px;
  border-radius: 5px;
}
.user-card-phone {
  font-size: 11px; color: #A0AAB2;
  font-family: 'SF Mono', 'Menlo', monospace;
  letter-spacing: 0.3px;
}
.card-chevron {
  font-size: 14px; color: #C5CAD0; flex-shrink: 0;
  transition: transform 0.25s ease;
}
.panel-user-card:hover .card-chevron { transform: translateX(2px); color: #74807B; }

/* ===== 分隔线 ===== */
.panel-separator {
  height: 1px; margin: 4px 20px;
  background: rgba(0, 0, 0, 0.05);
}

/* ===== 快捷数据概览（三列，整行可点） ===== */
.panel-stats-row {
  display: flex; align-items: center;
  margin: 2px 8px; padding: 6px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.panel-stats-row:hover { background: rgba(0, 0, 0, 0.03); }

.stat-item {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 1px;
  padding: 6px 0;
}
.stat-value {
  font-size: 20px; font-weight: 700; color: #1C2421;
  font-variant-numeric: tabular-nums; line-height: 1.2;
}
.stat-value.pending { color: #F5A623; }
.stat-value.completed { color: #2AA876; }
.stat-label { font-size: 10px; font-weight: 500; color: #A0AAB2; letter-spacing: 0.2px; }

/* ===== 底部退出 ===== */
.panel-footer {
  padding: 4px 8px 2px;
}
.sign-out-btn {
  display: flex; align-items: center; justify-content: center; gap: 7px;
  padding: 10px 0; border-radius: 14px;
  font-size: 13px; font-weight: 600; color: rgba(217, 83, 79, 0.8);
  cursor: pointer; transition: all 0.2s ease;
}
.sign-out-btn:hover {
  background: rgba(217, 83, 79, 0.06);
  color: #D9534F;
}
.sign-out-btn:active { transform: scale(0.97); }

/* ========== 面板过渡动画 ========== */

/* 遮罩淡入淡出 */
.menu-fade-enter-active,
.menu-fade-leave-active { transition: opacity 0.25s ease; }
.menu-fade-enter-from,
.menu-fade-leave-to { opacity: 0; }

/* iOS 弹性入场 */
.menu-panel-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: top right;
}
.menu-panel-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: top right;
}
.menu-panel-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.94);
}
.menu-panel-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}
.swiss-tooltip.el-popper {
  background: rgba(28, 36, 33, 0.85) !important;
  backdrop-filter: blur(16px) !important;
  border: none !important;
  border-radius: 8px !important;
  color: #FFFFFF !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  letter-spacing: 0.5px !important;
  padding: 8px 14px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
}
.swiss-tooltip.el-popper .el-popper__arrow::before { background: rgba(28, 36, 33, 0.85) !important; border: none !important; }

.swiss-message-box {
  border-radius: 20px !important;
  border: 1px solid rgba(255, 255, 255, 0.8) !important;
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(40px) !important;
  box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.1) !important;
}
</style>