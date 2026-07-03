<template>
  <div class="nepv-layout">
    <!-- 顶栏 -->
    <header class="nepv-topbar">
      <div class="brand">
        <span class="brand-icon">📊</span>
        <span class="brand-name">NEP 环保决策中枢</span>
      </div>
      <div class="topbar-tabs">
        <router-link to="/nepv/dashboard" class="tab" active-class="tab-active">数据大屏</router-link>
        <router-link to="/nepv/statistics" class="tab" active-class="tab-active">详细统计</router-link>
        <router-link to="/nepv/news" class="tab" active-class="tab-active">环保资讯</router-link>
        <router-link to="/nepv/knowledge" class="tab" active-class="tab-active">知识库</router-link>
        <router-link to="/nepv/ai" class="tab" active-class="tab-active">AI 助手</router-link>
      </div>
      <div class="topbar-right">
        <span class="vip-badge">决策者</span>
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-avatar :size="32" icon="UserFilled" />
            <span>{{ userName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="nepv-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const userName = computed(() => userStore.userName || '决策者')

function handleLogout() { localStorage.clear(); router.push('/login') }
</script>

<style scoped>
.nepv-layout {
  display: flex; flex-direction: column; height: 100vh;
  background: #F4F6F5; color: #1C2421;
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, Arial, sans-serif;
}

/* 顶栏 — 毛玻璃 */
.nepv-topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 28px; height: 56px;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(40px) saturate(160%);
  -webkit-backdrop-filter: blur(40px) saturate(160%);
  border-bottom: 1px solid rgba(255,255,255,0.7);
  flex-shrink: 0; z-index: 10;
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand-icon { font-size: 20px; }
.brand-name { font-size: 17px; font-weight: 700; color: #1C2421; letter-spacing: 0.3px; }

.topbar-tabs { display: flex; gap: 2px; }
.tab {
  padding: 8px 22px; color: #74807B; text-decoration: none;
  font-size: 14px; font-weight: 500; border-radius: 10px;
  transition: all 0.2s;
}
.tab:hover { color: #1C2421; background: rgba(28,36,33,0.04); }
.tab-active { color: #2A483A !important; background: rgba(42,72,58,0.08) !important; font-weight: 600; }

.topbar-right { display: flex; align-items: center; gap: 14px; }
.vip-badge {
  padding: 5px 14px; color: #2A483A;
  font-size: 12px; font-weight: 600;
  background: rgba(42,72,58,0.08); border-radius: 10px;
}
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #1C2421; font-weight: 500; }

.nepv-main { flex: 1; overflow-y: auto; }
</style>
