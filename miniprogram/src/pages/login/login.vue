<template>
  <view class="login-container">
    <view class="login-card">
      <view class="logo-section">
        <text class="logo-icon">⚡</text>
        <text class="system-title">电力监测系统</text>
        <text class="system-subtitle">特需用户用电监测与异常预警</text>
      </view>

      <view class="form-section">
        <view class="form-item">
          <view class="form-label">账号</view>
          <input 
            class="form-input" 
            v-model="form.username" 
            placeholder="请输入账号"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="form-item">
          <view class="form-label">密码</view>
          <input 
            class="form-input" 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="form-item-checkbox">
          <checkbox-group @change="onRememberChange">
            <label>
              <checkbox :checked="rememberMe" color="#409eff" />
              <text class="checkbox-text">记住我</text>
            </label>
          </checkbox-group>
        </view>

        <button 
          class="login-btn" 
          @click="handleLogin"
          :loading="loading"
          :disabled="loading"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <view class="register-link">
          <text>还没有账号？</text>
          <text class="link-text" @click="goToRegister">立即注册</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '../../utils/request'
import { setUserInfo } from '../../utils/auth'

const form = reactive({
  username: '',
  password: ''
})

const rememberMe = ref(false)
const loading = ref(false)

// 初始化表单 - 读取记住的登录信息
onMounted(() => {
  const savedUsername = uni.getStorageSync('rememberedUsername')
  const savedPassword = uni.getStorageSync('rememberedPassword')
  
  if (savedUsername && savedPassword) {
    form.username = savedUsername
    form.password = savedPassword
    rememberMe.value = true
  }
})

// 记住我选择变化
const onRememberChange = (e: any) => {
  rememberMe.value = e.detail.value.length > 0
}

// 登录处理
const handleLogin = async () => {
  // 表单验证
  if (!form.username) {
    uni.showToast({
      title: '请输入账号',
      icon: 'none'
    })
    return
  }
  
  if (!form.password) {
    uni.showToast({
      title: '请输入密码',
      icon: 'none'
    })
    return
  }

  loading.value = true

  try {
    const res: any = await request.post('/auth/login', {
      username: form.username,
      password: form.password
    })

    // 保存用户信息
    setUserInfo({
      token: res.token,
      username: res.username,
      role: res.role,
      userId: res.userId.toString()
    })

    // 处理记住我
    if (rememberMe.value) {
      uni.setStorageSync('rememberedUsername', form.username)
      uni.setStorageSync('rememberedPassword', form.password)
    } else {
      uni.removeStorageSync('rememberedUsername')
      uni.removeStorageSync('rememberedPassword')
    }

    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })

    // 根据角色跳转
    setTimeout(() => {
      if (res.role === 'MANAGER') {
        uni.switchTab({
          url: '/pages/user-profile/user-profile'
        })
      } else {
        uni.switchTab({
          url: '/pages/dashboard/dashboard'
        })
      }
    }, 1500)

  } catch (error) {
    console.error('登录失败:', error)
    uni.showToast({
      title: '登录失败，请检查账号密码',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 跳转注册
const goToRegister = () => {
  uni.navigateTo({
    url: '/pages/register/register'
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.login-card {
  width: 100%;
  max-width: 600rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.2);
}

.logo-section {
  text-align: center;
  margin-bottom: 60rpx;
}

.logo-icon {
  font-size: 100rpx;
  display: block;
  margin-bottom: 20rpx;
}

.system-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #303133;
  display: block;
  margin-bottom: 10rpx;
}

.system-subtitle {
  font-size: 24rpx;
  color: #909399;
  display: block;
}

.form-section {
  width: 100%;
}

.form-item {
  margin-bottom: 30rpx;
}

.form-label {
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.form-input {
  width: 100%;
  height: 88rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #303133;
  box-sizing: border-box;
}

.input-placeholder {
  color: #c0c4cc;
}

.form-item-checkbox {
  margin-bottom: 40rpx;
}

.form-item-checkbox label {
  display: flex;
  align-items: center;
}

.form-item-checkbox checkbox {
  margin-right: 12rpx;
}

.form-item-checkbox .checkbox-text {
  font-size: 26rpx;
  color: #606266;
}

.login-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: bold;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
}

.login-btn:active {
  opacity: 0.8;
}

.login-btn[disabled] {
  opacity: 0.6;
}

.register-link {
  text-align: center;
  font-size: 26rpx;
  color: #909399;
}

.register-link .link-text {
  color: #409eff;
  margin-left: 8rpx;
}
</style>

