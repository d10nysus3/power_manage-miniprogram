<template>
  <view class="personal-info-container">
    <!-- 头像区域 -->
    <view class="avatar-section">
      <view class="avatar">{{ username.charAt(0) }}</view>
      <text class="username">{{ username }}</text>
      <text class="role">{{ roleDisplayName }}</text>
    </view>

    <!-- 信息列表 -->
    <view class="info-section">
      <view class="info-item" @click="editProfile">
        <view class="item-left">
          <text class="item-icon">👤</text>
          <text class="item-label">个人资料</text>
        </view>
        <text class="item-arrow">→</text>
      </view>

      <view class="info-item" @click="changePassword">
        <view class="item-left">
          <text class="item-icon">🔒</text>
          <text class="item-label">修改密码</text>
        </view>
        <text class="item-arrow">→</text>
      </view>

      <view class="info-item" @click="viewNotifications">
        <view class="item-left">
          <text class="item-icon">🔔</text>
          <text class="item-label">消息通知</text>
        </view>
        <view class="item-right">
          <text class="badge" v-if="unreadCount > 0">{{ unreadCount }}</text>
          <text class="item-arrow">→</text>
        </view>
      </view>

      <view class="info-item" v-if="role === 'ADMIN'" @click="viewSettings">
        <view class="item-left">
          <text class="item-icon">⚙️</text>
          <text class="item-label">系统设置</text>
        </view>
        <text class="item-arrow">→</text>
      </view>

      <view class="info-item" @click="viewAbout">
        <view class="item-left">
          <text class="item-icon">ℹ️</text>
          <text class="item-label">关于系统</text>
        </view>
        <text class="item-arrow">→</text>
      </view>
    </view>

    <!-- 退出登录按钮 -->
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
const unreadCount = ref(0)

onMounted(() => {
  const userInfo = getUserInfo()
  username.value = userInfo.username || '用户'
  role.value = userInfo.role || ''
  roleDisplayName.value = getRoleDisplayName(userInfo.role || '')
})

// 编辑个人资料
const editProfile = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 修改密码
const changePassword = () => {
  uni.showModal({
    title: '修改密码',
    editable: true,
    placeholderText: '请输入新密码',
    success: (res) => {
      if (res.confirm && res.content) {
        uni.showToast({
          title: '密码修改成功',
          icon: 'success'
        })
      }
    }
  })
}

// 查看通知
const viewNotifications = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 系统设置
const viewSettings = () => {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

// 关于系统
const viewAbout = () => {
  uni.showModal({
    title: '关于系统',
    content: '特需用户用电监测与异常预警系统\n版本: 1.0.0\n\n为特需用户提供实时用电监测和异常预警服务',
    showCancel: false
  })
}

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearUserInfo()
        uni.showToast({
          title: '已退出登录',
          icon: 'success'
        })
        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/login/login'
          })
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
}

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
  margin-bottom: 10rpx;
}

.role {
  font-size: 26rpx;
  opacity: 0.9;
  padding: 8rpx 20rpx;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
}

.info-section {
  margin: -40rpx 20rpx 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item:active {
  background-color: #f5f7fa;
}

.item-left {
  display: flex;
  align-items: center;
}

.item-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
}

.item-label {
  font-size: 28rpx;
  color: #303133;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.badge {
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  background-color: #f56c6c;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-arrow {
  font-size: 28rpx;
  color: #c0c4cc;
}

.logout-section {
  padding: 40rpx 20rpx;
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

