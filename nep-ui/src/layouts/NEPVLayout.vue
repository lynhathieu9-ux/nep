<template>
  <div class="lay">
    <header class="bar">
      <div class="bar-l">
        <span class="brand">NEP 环保决策中枢</span>
      </div>
      <div class="bar-c">
        <router-link to="/nepv/dashboard" class="t" active-class="t-on">数据大屏</router-link>
        <router-link to="/nepv/statistics" class="t" active-class="t-on">详细统计</router-link>
        <router-link to="/nepv/news" class="t" active-class="t-on">环保资讯</router-link>
        <router-link to="/nepv/knowledge" class="t" active-class="t-on">知识库</router-link>
        <router-link to="/nepv/ai" class="t" active-class="t-on">AI助手</router-link>
      </div>
      <div class="bar-r">
        <span class="tm">{{ currentTime }}</span>
        <span class="loc">📍 中国</span>
        <span class="badge">决策者</span>
        <el-dropdown trigger="click">
          <span class="usr">
            <el-avatar :size="24" icon="UserFilled" />
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
    <main class="m">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
const router = useRouter()
const userStore = useUserStore()
const userName = computed(function() { return userStore.userName || 'ZhaoLi' })
const currentTime = ref('')
function updateTime() { var n = new Date(); currentTime.value = String(n.getHours()).padStart(2,'0')+':'+String(n.getMinutes()).padStart(2,'0')+':'+String(n.getSeconds()).padStart(2,'0') }
var timer = null
onMounted(function() { updateTime(); timer = setInterval(updateTime, 1000) })
onUnmounted(function() { if (timer) clearInterval(timer) })
function handleLogout() { localStorage.clear(); router.push('/login') }
</script>

<style scoped>
.lay { display:flex; flex-direction:column; height:100vh; background:#030A18; color:#e8e8e8; font-family:"SF Pro Display",-apple-system,BlinkMacSystemFont,sans-serif; overflow:hidden; }
.bar { display:flex; justify-content:space-between; align-items:center; padding:0 28px; height:52px; background:rgba(10,22,45,0.9); backdrop-filter:blur(16px); border-bottom:1px solid rgba(255,255,255,0.06); flex-shrink:0; z-index:100; }
.brand { font-size:15px; font-weight:700; color:#00E5FF; letter-spacing:1px; text-shadow:0 0 8px rgba(0,229,255,0.3); }
.bar-c { display:flex; gap:2px; background:rgba(255,255,255,0.03); padding:3px; border-radius:10px; border:1px solid rgba(255,255,255,0.05); }
.t { padding:7px 20px; color:#8A9EBC; text-decoration:none; font-size:13px; font-weight:500; border-radius:8px; transition:all 0.25s; letter-spacing:0.3px; }
.t:hover { color:#c8d6e5; }
.t-on { color:#fff!important; background:rgba(0,229,255,0.15); box-shadow:0 0 12px rgba(0,229,255,0.2); font-weight:600; }
.bar-r { display:flex; align-items:center; gap:14px; }
.tm { font-size:12px; color:#8A9EBC; font-family:monospace; }
.loc { font-size:12px; color:#8A9EBC; }
.badge { padding:4px 12px; color:#00E5FF; font-size:11px; font-weight:600; background:rgba(0,229,255,0.08); border-radius:8px; border:1px solid rgba(0,229,255,0.2); }
.usr { display:flex; align-items:center; gap:6px; cursor:pointer; color:#c8d6e5; font-weight:500; font-size:12px; }
.m { flex:1; overflow-y:auto; overflow-x:hidden; }
.m::-webkit-scrollbar { width:5px; }
.m::-webkit-scrollbar-track { background:rgba(0,0,0,0.2); }
.m::-webkit-scrollbar-thumb { background:rgba(0,229,255,0.15); border-radius:3px; }
</style>
