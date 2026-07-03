import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ==================== 公共页面（无需布局） ====================
  {
    path: '/login', name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/register', name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', noAuth: true }
  },

  // ==================== NEPS 公众监督员端 ====================
  {
    path: '/ne',
    component: () => import('@/layouts/NEPSLayout.vue'),
    redirect: '/ne/home',
    meta: { role: ['NEPS'] },
    children: [
      { path: 'home', name: 'NEPSHome', component: () => import('@/views/neps/NEPSHome.vue'), meta: { title: '首页' } },
      { path: 'submit', name: 'FeedbackSubmit', component: () => import('@/views/FeedbackSubmit.vue'), meta: { title: '提交反馈' } },
      { path: 'feedbacks', name: 'MyFeedbacks', component: () => import('@/views/FeedbackList.vue'), meta: { title: '我的反馈' } },
      { path: 'news', name: 'NEPSNews', component: () => import('@/views/NewsList.vue'), meta: { title: '环保资讯' } },
      { path: 'news/:id', name: 'NEPSNewsDetail', component: () => import('@/views/NewsDetail.vue'), meta: { title: '资讯详情' } },
      { path: 'knowledge', name: 'NEPSKnowledge', component: () => import('@/views/KnowledgeList.vue'), meta: { title: '知识库' } },
      { path: 'knowledge/:id', name: 'NEPSKnowledgeDetail', component: () => import('@/views/KnowledgeDetail.vue'), meta: { title: '知识详情' } },
      { path: 'notifications', name: 'NEPSNotifications', component: () => import('@/views/NotificationCenter.vue'), meta: { title: '通知' } },
      { path: 'ai', name: 'NEPSAi', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI助手' } },
      { path: 'profile', name: 'NEPSProfile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
    ]
  },

  // ==================== NEPG 网格员端 ====================
  {
    path: '/nepg',
    component: () => import('@/layouts/NEPGLayout.vue'),
    redirect: '/nepg/home',
    meta: { role: ['NEPG'] },
    children: [
      { path: 'home', name: 'NEPGHome', component: () => import('@/views/nepg/NEPGHome.vue'), meta: { title: '工作台' } },
      { path: 'tasks', name: 'NEPGTasks', component: () => import('@/views/nepg/NEPGTasks.vue'), meta: { title: '检测任务' } },
      { path: 'records', name: 'NEPGRecords', component: () => import('@/views/nepg/NEPGRecords.vue'), meta: { title: '历史记录' } },
      { path: 'news', name: 'NEPGNews', component: () => import('@/views/NewsList.vue'), meta: { title: '环保资讯' } },
      { path: 'news/:id', name: 'NEPGNewsDetail', component: () => import('@/views/NewsDetail.vue'), meta: { title: '资讯详情' } },
      { path: 'knowledge', name: 'NEPGKnowledge', component: () => import('@/views/KnowledgeList.vue'), meta: { title: '知识库' } },
      { path: 'knowledge/:id', name: 'NEPGKnowledgeDetail', component: () => import('@/views/KnowledgeDetail.vue'), meta: { title: '知识详情' } },
      { path: 'notifications', name: 'NEPGNotifications', component: () => import('@/views/NotificationCenter.vue'), meta: { title: '通知' } },
      { path: 'ai', name: 'NEPGAi', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI助手' } },
      { path: 'profile', name: 'NEPGProfile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
    ]
  },

  // ==================== NEPM 管理员端 ====================
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { role: ['NEPM'] },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '管理首页' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'feedbacks', name: 'AdminFeedbacks', component: () => import('@/views/admin/FeedbackManage.vue'), meta: { title: '反馈管理' } },
      { path: 'news', name: 'AdminNews', component: () => import('@/views/admin/NewsManage.vue'), meta: { title: '新闻管理' } },
      { path: 'knowledge', name: 'AdminKnowledge', component: () => import('@/views/admin/KnowledgeManage.vue'), meta: { title: '文献发布管理' } },
      { path: 'knowledge/:id', name: 'AdminKnowledgeDetail', component: () => import('@/views/KnowledgeDetail.vue'), meta: { title: '文献详情' } },
      { path: 'statistics', name: 'AdminStats', component: () => import('@/views/Statistics.vue'), meta: { title: '数据统计' } },
      { path: 'notifications', name: 'AdminNotifications', component: () => import('@/views/NotificationCenter.vue'), meta: { title: '通知' } },
      { path: 'ai', name: 'AdminAi', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI助手' } },
    ]
  },

  // ==================== NEPV 决策者端 ====================
  {
    path: '/nepv',
    component: () => import('@/layouts/NEPVLayout.vue'),
    redirect: '/nepv/dashboard',
    meta: { role: ['NEPV', 'NEPM'] },
    children: [
      { path: 'dashboard', name: 'NEPVDashboard', component: () => import('@/views/nepv/NEPVDashboard.vue'), meta: { title: '数据大屏' } },
      { path: 'statistics', name: 'NEPVStats', component: () => import('@/views/Statistics.vue'), meta: { title: '详细统计' } },
      { path: 'news', name: 'NEPVNews', component: () => import('@/views/NewsList.vue'), meta: { title: '环保资讯' } },
      { path: 'news/:id', name: 'NEPVNewsDetail', component: () => import('@/views/NewsDetail.vue'), meta: { title: '资讯详情' } },
      { path: 'knowledge', name: 'NEPVKnowledge', component: () => import('@/views/KnowledgeList.vue'), meta: { title: '知识库' } },
      { path: 'knowledge/:id', name: 'NEPVKnowledgeDetail', component: () => import('@/views/KnowledgeDetail.vue'), meta: { title: '知识详情' } },
      { path: 'ai', name: 'NEPVAi', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI助手' } },
    ]
  },

  // 兜底：未匹配路由重定向
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 角色路由守卫 + 登录重定向
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 东软环保公众监督系统` : '东软环保公众监督系统'

  const role = localStorage.getItem('userRole') || ''
  const token = localStorage.getItem('token')

  // 未登录 → 只能去登录/注册
  if (!to.meta.noAuth && !token) {
    return next('/login')
  }

  // 已登录 → 不能去登录/注册
  if (to.meta.noAuth && token) {
    return next(getRoleHome(role))
  }

  // 角色权限检查
  if (to.meta.role && Array.isArray(to.meta.role)) {
    if (!to.meta.role.includes(role)) {
      return next(getRoleHome(role))
    }
  }

  next()
})

/** 根据角色返回首页路径 */
function getRoleHome(role) {
  switch (role) {
    case 'NEPS': return '/ne/home'
    case 'NEPG': return '/nepg/home'
    case 'NEPM': return '/admin/dashboard'
    case 'NEPV': return '/nepv/dashboard'
    default: return '/login'
  }
}

export default router
