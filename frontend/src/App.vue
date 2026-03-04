<script setup lang="ts">
import { RouterView } from 'vue-router';
import { HomeFilled, UserFilled, PieChart, Warning, BellFilled, Monitor, Files } from '@element-plus/icons-vue';
import { ref, onMounted, onUnmounted, computed } from 'vue';
import router from './router';
import { ElMessage } from 'element-plus';
import { useDevice } from './composables/useDevice';

const isCollapse = ref(false);
const { isMobile } = useDevice();

const userRole = ref<string | null>(null);
const username = ref<string | null>(null);
const menuItems = [
  { path: '/', icon: HomeFilled, text: '首页', roles: ['ADMIN', 'MANAGER'] },
  { path: '/user-profile', icon: UserFilled, text: '档案管理', roles: ['ADMIN', 'MANAGER'] },
  { path: '/electricity-pattern', icon: PieChart, text: '用电分析', roles: ['ADMIN'] },
  { path: '/anomaly-detection', icon: Warning, text: '异常检测', roles: ['ADMIN'] },
  { path: '/emergency-processing', icon: BellFilled, text: '应急处理', roles: ['ADMIN'] },
];


const accessibleMenuItems = computed(() => {
  if (!userRole.value) return [];
  console.log('计算可访问菜单项，当前角色:', userRole.value);
  const items = menuItems.filter(item => item.roles.includes(userRole.value!));
  console.log('可访问菜单项:', items);
  return items;
});

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value;
};

// 监听设备类型变化，确保移动端侧边栏默认折叠
const handleDeviceChange = () => {
  if (isMobile.value) {
    isCollapse.value = true;
  }
};

const handleMenuClick = (path: string) => {
  router.push(path);
  if (isMobile.value) {
    isCollapse.value = true;
  }
};

const handlePersonalInfo = () => {
  router.push('/personal-info');
};

const handleSystemSettings = () => {
  router.push('/settings');
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  userRole.value = null;
  username.value = null;
  router.push('/login');
  ElMessage.success('退出登录成功');
};



const handleMainClick = () => {
  if (isMobile.value && !isCollapse.value) {
    isCollapse.value = true;
  }
};


const initializeUserInfo = () => {
  // 优先从 localStorage 读取
  let role = localStorage.getItem('role');
  let user = localStorage.getItem('username');
  
  // 如果 localStorage 没有，尝试从 sessionStorage 读取
  if (!role) {
    role = sessionStorage.getItem('role');
  }
  if (!user) {
    user = sessionStorage.getItem('username');
  }
  
  userRole.value = role;
  username.value = user;
  console.log('用户信息已初始化:', { role: userRole.value, username: username.value });
};


const handleStorageChange = (e: StorageEvent) => {
  if (e.key === 'role') {
    userRole.value = e.newValue;
  } else if (e.key === 'username') {
    username.value = e.newValue;
  } else if (e.key === null) {
    
    userRole.value = null;
    username.value = null;
  }
};

onMounted(() => {
  if (isMobile.value) {
    isCollapse.value = true;
  }
  window.addEventListener('storage', handleStorageChange);
  window.addEventListener('resize', handleDeviceChange);
  
  window.addEventListener('login-success', initializeUserInfo);
  
  initializeUserInfo();
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange);
  window.removeEventListener('resize', handleDeviceChange);
});
</script>

<template>
  <div class="app-container">
    
    <el-header class="header">
      <div class="header-left">
        <el-button
          type="primary"
          icon="Menu"
          @click="toggleCollapse"
          :size="isCollapse ? 'large' : 'default'"
          circle
          class="menu-toggle"
        />
        <h1 class="system-title">特需用户用电监测与异常预警系统</h1>
      </div>
      <div class="header-right">
        <el-dropdown>
          <span class="user-info">
    <el-avatar>{{ username && username.trim().length > 0 ? username.charAt(0) : '系' }}</el-avatar>
    <span>{{ username || '请登录' }}<span v-if="userRole">({{ userRole === 'ADMIN' ? '电力公司人员' : '社区人员' }})</span></span>
  </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handlePersonalInfo">
                <UserFilled />
                <span>个人信息</span>
              </el-dropdown-item>
              <el-dropdown-item v-if="userRole === 'ADMIN'" @click="handleSystemSettings">
                <span>系统设置</span>
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <div class="main-content">
      <!-- 移动设备上的遮罩层 -->
      <div 
        v-if="isMobile && !isCollapse" 
        class="sidebar-overlay"
        @click="isCollapse = true"
      ></div>
      
      <!-- 侧边栏导航 -->
      <el-aside 
        :width="isCollapse ? '64px' : '200px'" 
        class="aside" 
        :class="{ 'mobile-aside': isMobile, 'el-aside--collapse': isCollapse }"
      >
        <el-menu
          :default-active="$route.path"
          class="el-menu-vertical-demo"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b"
          :collapse="isCollapse"
        >
          <!-- 根据用户角色动态渲染菜单项 -->
          <template v-for="item in accessibleMenuItems" :key="item.path">
            <el-menu-item 
              :index="item.path" 
              @click="handleMenuClick(item.path)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.text }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-main class="main" @click="handleMainClick">
        <RouterView />
      </el-main>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .header {
    padding: 0 10px;
  }
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.menu-toggle {
  margin-right: 16px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .menu-toggle {
    margin-right: 8px;
  }
}

.system-title {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .system-title {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .system-title {
    font-size: 14px;
    max-width: 200px;
  }
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.message-button {
  margin-right: 16px;
  font-size: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.user-info .el-avatar {
  margin-right: 8px;
}

.user-role {
  margin-left: 8px;
  font-size: 12px;
  color: #606266;
}

@media (max-width: 480px) {
  .user-info {
    padding: 6px 8px;
  }
  
  .user-info .el-avatar {
    margin-right: 4px;
    width: 24px;
    height: 24px;
  }
  
  .user-info span {
    font-size: 12px;
  }
}

.main-content {
  display: flex;
  flex: 1;
  width: 100%;
  overflow: hidden;
  position: relative;
}


.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: block;
}

.aside {
  background-color: #545c64;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.mobile-aside {
  position: fixed;
  left: 0;
  top: 60px;
  height: calc(100vh - 60px);
  z-index: 1000;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
  transform: translateX(-100%);
}

/* 移动端侧边栏展开状态 */
.mobile-aside:not(.el-aside--collapse) {
  transform: translateX(0);
}

@media (max-width: 768px) {
  .aside:not(.mobile-aside) {
    display: none;
  }
}

.main {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding: 0;
}

@media (max-width: 768px) {
  .main {
    width: 100%;
    margin-left: 0;
  }
}
</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

#app {
  width: 100%;
  height: 100vh;
}


.container, .dashboard-container, .anomaly-records, .real-time-monitoring {
  width: 100% !important;
  max-width: none !important;
}


.el-menu {
  border-right: none;
}

.el-menu-item {
  transition: background-color 0.3s;
}

.el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.el-menu-item.is-active {
  background-color: rgba(255, 208, 75, 0.2) !important;
}


@media (max-width: 768px) {
  .el-table {
    font-size: 12px;
  }
  
  .el-table .el-table__cell {
    padding: 8px 4px;
  }
  
  .el-pagination {
    justify-content: center;
  }
  
  .el-pagination .el-pager li {
    min-width: 24px;
    height: 24px;
    line-height: 24px;
    font-size: 12px;
  }
}


@media (max-width: 480px) {
  .el-button {
    font-size: 12px;
    padding: 6px 10px;
  }
  
  .el-button--small {
    font-size: 10px;
    padding: 4px 6px;
  }
}


@media (max-width: 768px) {
  .el-input__inner {
    font-size: 14px;
  }
  
  .el-select .el-input__inner {
    font-size: 14px;
  }
}


@media (max-width: 768px) {
  .el-card {
    margin-bottom: 10px;
  }
  
  .el-card__header {
    padding: 12px 16px;
  }
  
  .el-card__body {
    padding: 12px 16px;
  }
}


@media (max-width: 768px) {
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  
  ::-webkit-scrollbar-track {
    background: #f1f1f1;
  }
  
  ::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
  }
  
  ::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
}
</style>
