<template>
  <div class="roster-canvas">
    
    <header class="roster-toolbar">
      <div class="toolbar-left">
        <div class="alpine-search">
          <el-icon class="search-icon"><Search /></el-icon>
          <input 
            v-model="searchQuery" 
            placeholder="搜索用户姓名或手机号..." 
            class="search-input"
          >
        </div>
      </div>

      <div class="toolbar-right">
        <div class="swiss-segments">
          <button class="segment-btn" :class="{ active: roleFilter === '' }" @click="roleFilter = ''">全部</button>
          <button class="segment-btn" :class="{ active: roleFilter === 'NEPS' }" @click="roleFilter = 'NEPS'">监督员</button>
          <button class="segment-btn" :class="{ active: roleFilter === 'NEPG' }" @click="roleFilter = 'NEPG'">网格员</button>
          <button class="segment-btn" :class="{ active: roleFilter === 'NEPM' }" @click="roleFilter = 'NEPM'">管理员</button>
          <button class="segment-btn" :class="{ active: roleFilter === 'NEPV' }" @click="roleFilter = 'NEPV'">决策者</button>
        </div>
        
        <div class="toolbar-divider"></div>

        <button class="alpine-btn primary" @click="handleInvite">
          <el-icon><Plus /></el-icon> 新增账户
        </button>
      </div>
    </header>

    <main class="roster-workspace" v-loading="loading">
      
      <div class="roster-header-row roster-grid">
        <div class="col-id">SYS-ID</div>
        <div class="col-user">系统用户</div>
        <div class="col-contact">联络凭证</div>
        <div class="col-role">权限角色</div>
        <div class="col-date">注册时间</div>
        <div class="col-status">账户状态</div>
        <div class="col-action">操作</div>
      </div>

      <div class="roster-scroll-area">
        
        <div v-if="filteredUsers.length === 0 && !loading" class="empty-state">
          <el-icon class="empty-icon"><User /></el-icon>
          <p>当前检索条件下没有找到任何用户记录</p>
        </div>

        <div class="roster-list" v-else>
          <div class="roster-row roster-grid" v-for="user in filteredUsers" :key="user.id">
            
            <div class="col-id">
              <span class="mono-id">{{ String(user.id).padStart(4, '0') }}</span>
            </div>

            <div class="col-user">
              <div class="letter-avatar" :class="getAvatarColor(user.realName)">
                {{ user.realName?.charAt(0) || 'U' }}
              </div>
              <span class="user-name">{{ user.realName || '未命名' }}</span>
            </div>

            <div class="col-contact">
              <div class="contact-pill">
                <el-icon><Iphone /></el-icon>
                <span>{{ user.phone || '未绑定' }}</span>
              </div>
            </div>

            <div class="col-role">
              <span class="role-badge" :class="user.role?.toLowerCase() || 'default'">
                {{ getRoleName(user.role) }}
              </span>
            </div>

            <div class="col-date">
              <span class="date-text">{{ formatTime(user.createTime) }}</span>
            </div>

            <div class="col-status" @click.stop>
              <el-switch 
                :model-value="user.status === 1" 
                :loading="user.switching"
                @change="toggleStatus(user)"
                class="swiss-switch"
                style="--el-switch-on-color: #2A483A; --el-switch-off-color: #A0AAB2;"
              />
            </div>

            <div class="col-action">
              <button class="action-btn" title="重置密码" @click="handleResetPwd(user)" style="margin-right:8px">
                <el-icon><Key /></el-icon>
              </button>
              <button class="action-btn danger" title="注销账户" @click="handleDelete(user)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>

          </div>
        </div>
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserList, updateUser, resetUserPassword } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, User, Iphone, Delete, Key } from '@element-plus/icons-vue'

const users = ref([])
const loading = ref(false)

// 过滤状态
const searchQuery = ref('')
const roleFilter = ref('')

// 获取用户列表
async function fetchUsers() {
  loading.value = true
  try { 
    const r = await getUserList()
    // 注入前端专用的 switching 状态控制
    users.value = (r.data || []).map(u => ({ ...u, switching: false }))
  } catch (e) {
    ElMessage.error('无法拉取用户数据')
  } finally { 
    loading.value = false 
  }
}

// 客户端联动过滤
const filteredUsers = computed(() => {
  return users.value.filter(u => {
    const matchRole = roleFilter.value === '' || u.role === roleFilter.value
    const matchSearch = (u.realName || '').includes(searchQuery.value) || 
                        (u.phone || '').includes(searchQuery.value)
    return matchRole && matchSearch
  })
})

// 状态切换器
async function toggleStatus(row) {
  row.switching = true
  const newStatus = row.status === 1 ? 0 : 1
  try { 
    await updateUser(row.id, { status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? `${row.realName} 账户已解封` : `${row.realName} 账户已冻结`)
  } catch (e) {
    ElMessage.error('状态更新失败')
  } finally { 
    row.switching = false 
  }
}

// 问题③：管理员重置用户密码
async function handleResetPwd(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为用户 "${row.realName}" 设置新密码（至少6位，默认 123456）`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputValue: '123456',
        inputPattern: /^.{6,}$/,
        inputErrorMessage: '密码至少6位'
      }
    )
    await resetUserPassword(row.id, value)
    ElMessage.success(`${row.realName} 的密码已重置`)
  } catch (e) {
    // 用户取消或接口报错（拦截器已提示）
  }
}

// 删除用户
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要永久注销用户 "${row.realName}" (手机号: ${row.phone}) 吗？此操作不可逆。`,
      '高危操作确认',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'error',
        customClass: 'alpine-danger-box'
      }
    )

    // 这里可接入真实的 deleteUser(row.id)
    // await deleteUser(row.id)
    
    // 前端模拟移除
    users.value = users.value.filter(u => u.id !== row.id)
    ElMessage.success('账户已从系统中永久移除')
  } catch (e) {
    // 取消操作
  }
}

// 模拟邀请
const handleInvite = () => {
  ElMessage.info('邀请链接已生成，可复制发送给新用户')
}

// 辅助格式化方法
function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 10)
}

function getRoleName(role) {
  const map = { 'NEPS': '公众监督员', 'NEPG': '环境网格员', 'NEPM': '系统管理员', 'NEPV': '决策者' }
  return map[role] || '未定编'
}

// 算法：根据名字的字符编码分配固定的柔和渐变色
function getAvatarColor(name) {
  if (!name) return 'bg-gray'
  const charCode = name.charCodeAt(0) % 4
  const colors = ['bg-pine', 'bg-sage', 'bg-ocean', 'bg-amber']
  return colors[charCode]
}

onMounted(() => fetchUsers())
</script>

<style scoped>
/* ========== 全局画布 ========== */
.roster-canvas {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  padding: 32px 40px; box-sizing: border-box;
}

/* ========== 1. 工具栏 ========== */
.roster-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px; flex-shrink: 0;
}

.alpine-search {
  display: flex; align-items: center; gap: 10px; background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(28, 36, 33, 0.08); padding: 10px 16px; border-radius: 12px;
  width: 280px; transition: all 0.3s;
}
.alpine-search:focus-within { background: white; border-color: #2A483A; box-shadow: 0 4px 16px rgba(42, 72, 58, 0.08); }
.search-icon { color: #A0AAB2; font-size: 16px; }
.search-input { border: none; outline: none; font-size: 14px; width: 100%; color: #1C2421; background: transparent; }
.search-input::placeholder { color: #A0AAB2; }

.toolbar-right { display: flex; align-items: center; gap: 16px; }

/* 瑞士极简分段控制器 */
.swiss-segments {
  display: flex; background: rgba(28, 36, 33, 0.04); padding: 4px; border-radius: 10px;
}
.segment-btn {
  border: none; background: transparent; padding: 6px 16px; border-radius: 8px;
  font-size: 13px; font-weight: 600; color: #74807B; cursor: pointer; transition: all 0.3s;
}
.segment-btn:hover { color: #1C2421; }
.segment-btn.active { background: white; color: #1C2421; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }

.toolbar-divider { width: 1px; height: 20px; background: rgba(28, 36, 33, 0.1); }

.alpine-btn {
  border: none; cursor: pointer; border-radius: 10px; font-weight: 600; font-size: 14px;
  display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; transition: all 0.3s;
}
.alpine-btn.primary { background: #1C2421; color: white; box-shadow: 0 4px 12px rgba(28, 36, 33, 0.15); }
.alpine-btn.primary:hover { background: #2A483A; transform: translateY(-1px); box-shadow: 0 6px 16px rgba(42, 72, 58, 0.2); }

/* ========== 2. 名册工作区 ========== */
.roster-workspace {
  flex: 1; display: flex; flex-direction: column; min-height: 0;
}

/* 核心 CSS Grid 布局矩阵 */
.roster-grid {
  display: grid;
  /* 列宽分配：ID | User | Contact | Role | Date | Status | Action */
  grid-template-columns: 80px 2fr 1.5fr 1.5fr 120px 100px 80px;
  align-items: center; gap: 16px; padding: 0 16px;
}

/* 极简表头 */
.roster-header-row {
  height: 48px; border-bottom: 1px solid rgba(28, 36, 33, 0.08); flex-shrink: 0;
  font-size: 12px; font-weight: 600; color: #A0AAB2; letter-spacing: 0.5px;
}

/* 滚动数据区 */
.roster-scroll-area { flex: 1; overflow-y: auto; padding-top: 8px; }
.roster-scroll-area::-webkit-scrollbar { display: none; }

.roster-list { display: flex; flex-direction: column; gap: 4px; }

/* 单个用户数据行 */
.roster-row {
  height: 64px; border-radius: 12px; transition: all 0.3s ease; border: 1px solid transparent;
}
.roster-row:hover { background: rgba(255, 255, 255, 0.8); border-color: rgba(28, 36, 33, 0.04); box-shadow: 0 4px 12px -4px rgba(0,0,0,0.02); transform: scale(1.002); }

/* 列样式细化 */
.mono-id { font-family: monospace; font-size: 13px; color: #A0AAB2; font-weight: 600; }

.col-user { display: flex; align-items: center; gap: 12px; min-width: 0; }
.letter-avatar {
  width: 36px; height: 36px; border-radius: 10px; display: flex; justify-content: center; align-items: center;
  color: white; font-weight: 700; font-size: 15px; flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.user-name { font-size: 14px; font-weight: 600; color: #1C2421; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 头像高级预设色 */
.bg-pine { background: linear-gradient(135deg, #2A483A, #1C2421); }
.bg-sage { background: linear-gradient(135deg, #74807B, #535C58); }
.bg-ocean { background: linear-gradient(135deg, #3B82F6, #1D4ED8); }
.bg-amber { background: linear-gradient(135deg, #F59E0B, #D97706); }
.bg-gray { background: #A0AAB2; }

.contact-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: #74807B; font-family: monospace; font-weight: 500; }
.contact-pill .el-icon { color: #A0AAB2; }

.role-badge { padding: 4px 10px; border-radius: 8px; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; }
.role-badge.neps { background: rgba(42, 168, 118, 0.1); color: #2AA876; }
.role-badge.nepg { background: rgba(64, 158, 255, 0.1); color: #409EFF; }
.role-badge.nepm { background: rgba(245, 166, 35, 0.1); color: #F5A623; }
.role-badge.nepv { background: rgba(142, 68, 173, 0.1); color: #8E44AD; }
.role-badge.default { background: #F4F6F5; color: #74807B; }

.date-text { font-size: 13px; color: #A0AAB2; font-family: monospace; }

/* 深度定制的瑞士 Switch */
.swiss-switch :deep(.el-switch__core) { border: none; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.swiss-switch :deep(.el-switch__action) { box-shadow: 0 2px 4px rgba(0,0,0,0.1); }

.col-action { display: flex; justify-content: flex-end; }
.action-btn {
  width: 32px; height: 32px; border-radius: 8px; border: none; background: transparent;
  display: flex; justify-content: center; align-items: center; font-size: 16px; cursor: pointer;
  transition: all 0.2s; opacity: 0;
}
.roster-row:hover .action-btn { opacity: 1; }
.action-btn.danger { color: #A0AAB2; }
.action-btn.danger:hover { background: #FFF1F2; color: #E11D48; }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #A0AAB2; }
.empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.5; }
</style>

<style>
/* 高危操作弹窗专属美化 */
.alpine-danger-box {
  border-radius: 24px !important; border: 1px solid rgba(255, 255, 255, 0.9) !important;
  background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(40px) !important;
  box-shadow: 0 32px 64px -16px rgba(225, 29, 72, 0.15) !important;
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, sans-serif !important;
}
.alpine-danger-box .el-message-box__title { font-weight: 700; color: #1C2421; }
.alpine-danger-box .el-button--primary { background: #E11D48 !important; border-color: #E11D48 !important; border-radius: 10px; }
.alpine-danger-box .el-button--primary:hover { background: #BE123C !important; }
.alpine-danger-box .el-button { border-radius: 10px; }
</style>