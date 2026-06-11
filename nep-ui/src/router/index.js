import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  // ==================== 用户端 ====================
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    meta: { role: ['NEPS', 'NEPG', 'NEPM', 'NEPV'] },
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'feedback', name: 'Feedback', component: () => import('@/views/FeedbackList.vue'), meta: { title: '监督反馈' } },
      { path: 'feedback/submit', name: 'FeedbackSubmit', component: () => import('@/views/FeedbackSubmit.vue'), meta: { title: '提交反馈' } },
      { path: 'aqi', name: 'AqiDetection', component: () => import('@/views/AqiDetection.vue'), meta: { title: 'AQI检测' } },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/Statistics.vue'), meta: { title: '数据统计' } },
      { path: 'ai', name: 'AiChat', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI助手' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
    ]
  },
  // ==================== 管理员端 ====================
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { role: ['NEPM'], title: '管理员' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '管理首页' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'feedbacks', name: 'AdminFeedbacks', component: () => import('@/views/admin/FeedbackManage.vue'), meta: { title: '反馈管理' } },
      { path: 'statistics', name: 'AdminStats', component: () => import('@/views/Statistics.vue'), meta: { title: '数据统计' } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

// 角色路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 东软环保公众监督系统` : '东软环保公众监督系统'

  const role = localStorage.getItem('userRole') || ''
  const token = localStorage.getItem('token')

  // 未登录只能访问登录/注册
  if (!token && to.path !== '/login' && to.path !== '/register') {
    return next('/login')
  }

  // 角色权限检查
  if (to.meta.role && !to.meta.role.includes(role)) {
    return next('/home')
  }

  next()
})

export default router
