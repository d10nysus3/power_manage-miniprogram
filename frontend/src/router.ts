import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import Dashboard from './views/Dashboard.vue';
import Login from './views/Login.vue';
import Register from './views/Register.vue';
import Settings from './views/SystemSettings.vue';
import UserProfileManagement from './views/UserProfileManagement.vue';
import UserElectricityPatternAnalysis from './views/UserElectricityPatternAnalysis.vue';
import AnomalyElectricityDetection from './views/AnomalyElectricityDetection.vue';
import EmergencyProcessing from './views/EmergencyProcessing.vue';



import PersonalInfo from './views/PersonalInfo.vue';



const routes: Array<RouteRecordRaw> = [
    {
    path: '/',
    name: 'dashboard',
    component: Dashboard,
    meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] }
  },

  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register,
    meta: { requiresAuth: false }
  },
  {
    path: '/settings',
    name: 'settings',
    component: Settings,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/personal-info',
    name: 'personal-info',
    component: PersonalInfo,
    meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/user-profile',
    name: 'user-profile',
    component: UserProfileManagement,
    meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] }
  },
    {
    path: '/electricity-pattern',
    name: 'electricity-pattern',
    component: UserElectricityPatternAnalysis,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },

  {
    path: '/anomaly-detection',
    name: 'anomaly-detection',
    component: AnomalyElectricityDetection,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/emergency-processing',
    name: 'emergency-processing',
    component: EmergencyProcessing,
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  
  
  
  
  
  
  

  {
    path: '/reports',
    name: 'Reports',
    component: () => import('./views/Reports.vue'),
    meta: { title: '生成报告', requiresAuth: true, roles: ['ADMIN', 'MANAGER'] },
  },
  {
    path: '/audit-log',
    name: 'AuditLog',
    component: () => import('./views/AuditLog.vue'),
    meta: { title: '审计日志', requiresAuth: true, roles: ['ADMIN'] },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});


router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  const role = localStorage.getItem('role') || sessionStorage.getItem('role');
  
  // 允许未登录用户访问登录页和注册页
  if (!token && to.path !== '/login' && to.path !== '/register') {
    next('/login');
    return;
  }
  
  // 已登录用户尝试访问登录页时重定向到合适的页面
  if (token && to.path === '/login') {
    if (role === 'MANAGER' || role === 'ADMIN') {
      next('/');
    } else {
      // 角色未知时，清除token并返回登录页
      localStorage.removeItem('token');
      sessionStorage.removeItem('token');
      next('/login');
    }
    return;
  }
  
  
  // 角色权限检查
  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login');
      return;
    }
    
    // 检查用户角色是否有访问权限
    if (to.meta.roles && Array.isArray(to.meta.roles)) {
      if (!role || !to.meta.roles.includes(role)) {
        // 避免循环重定向：检查目标页面是否就是当前页面
        if ((role === 'MANAGER' || role === 'ADMIN') && to.path !== '/') {
          // 重定向到首页
          next('/');
        } else {
          // 防止循环重定向，当已经在正确的页面时，直接停止导航
          next(false);
        }
        return;
      }
    }
  }
  
  // 允许导航继续
  next();
});

export default router;