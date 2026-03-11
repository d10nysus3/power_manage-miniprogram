<template>
  <view class="personal-info-container">
    <!-- 头像区域 -->
    <view class="avatar-section">
      <view class="avatar">{{ username.charAt(0).toUpperCase() }}</view>
      <text class="username">{{ username }}</text>
      <text class="role-badge">{{ roleDisplayName }}</text>
    </view>

    <!-- 基本信息卡片 -->
    <view class="card">
      <view class="card-title">基本信息</view>

      <view class="info-row">
        <text class="info-label">用户名</text>
        <text class="info-value">{{ username }}</text>
      </view>
      <view class="divider" />

      <view class="info-row">
        <text class="info-label">角色</text>
        <text class="info-value">{{ roleDisplayName }}</text>
      </view>
      <view class="divider" />

      <view class="info-row">
        <text class="info-label">最近登录</text>
        <text class="info-value">{{ lastLoginTime }}</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserInfo, clearUserInfo, getRoleDisplayName } from '../../utils/auth'

const username = ref('')
const role = ref('')
const roleDisplayName = ref('')
const lastLoginTime = ref('')

onMounted(() => {
  const userInfo = getUserInfo()
  username.value = userInfo.username || '用户'
  role.value = userInfo.role || ''
  roleDisplayName.value = getRoleDisplayName(userInfo.role || '')
  lastLoginTime.value = new Date().toLocaleString('zh-CN')
})

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearUserInfo()
        uni.showToast({ title: '已退出登录', icon: 'success' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/login' })
        }, 1500)
      }
    }
  })
}
</script>

<style scoped>
.personal-info-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}

/* 头像区域 */
.avatar-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 30rpx 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #ffffff;
}

.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.username {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 12rpx;
}

.role-badge {
  font-size: 24rpx;
  padding: 8rpx 24rpx;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.4);
}

/* 卡片 */
.card {
  margin: 0 20rpx 24rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
}

.card:first-of-type {
  margin-top: -40rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid #f0f2f5;
}

/* 基本信息行 */
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
}

.info-label {
  font-size: 28rpx;
  color: #606266;
  flex-shrink: 0;
  width: 180rpx;
}

.info-value {
  font-size: 28rpx;
  color: #303133;
  text-align: right;
  flex: 1;
}

.divider {
  height: 1rpx;
  background-color: #f0f2f5;
}

/* 退出登录 */
.logout-section {
  padding: 0 20rpx;
  margin-top: 8rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  background-color: #ffffff;
  color: #f56c6c;
  border: 2rpx solid #f56c6c;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-btn:active {
  opacity: 0.8;
}
</style>

