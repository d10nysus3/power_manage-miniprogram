<template>
  <view class="register-container">
    <view class="register-card">
      <view class="logo-section">
        <text class="logo-icon">⚡</text>
        <text class="system-title">用户注册</text>
        <text class="system-subtitle">特需用户用电监测与异常预警</text>
      </view>

      <view class="form-section">
        <!-- 用户名 -->
        <view class="form-item">
          <view class="form-label">用户名</view>
          <input
            class="form-input"
            :class="{ 'form-input-error': errors.username }"
            v-model="form.username"
            placeholder="请输入用户名（至少3个字符）"
            placeholder-class="input-placeholder"
          />
          <text v-if="errors.username" class="error-tip">{{ errors.username }}</text>
        </view>

        <!-- 密码 -->
        <view class="form-item">
          <view class="form-label">密码</view>
          <input
            class="form-input"
            :class="{ 'form-input-error': errors.password }"
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6个字符）"
            placeholder-class="input-placeholder"
          />
          <text v-if="errors.password" class="error-tip">{{ errors.password }}</text>
        </view>

        <!-- 确认密码 -->
        <view class="form-item">
          <view class="form-label">确认密码</view>
          <input
            class="form-input"
            :class="{ 'form-input-error': errors.confirmPassword }"
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            placeholder-class="input-placeholder"
          />
          <text v-if="errors.confirmPassword" class="error-tip">{{ errors.confirmPassword }}</text>
        </view>

        <!-- 角色选择 -->
        <view class="form-item">
          <view class="form-label">角色</view>
          <picker
            mode="selector"
            :range="roleOptions"
            range-key="label"
            :value="selectedRoleIndex"
            @change="onRoleChange"
          >
            <view class="picker-input" :class="{ 'form-input-error': errors.role }">
              <text :class="form.role ? 'picker-text-selected' : 'picker-text-placeholder'">
                {{ form.role ? roleOptions.find(r => r.value === form.role)?.label : '请选择角色' }}
              </text>
              <text class="picker-arrow">▼</text>
            </view>
          </picker>
          <text v-if="errors.role" class="error-tip">{{ errors.role }}</text>
        </view>

        <!-- 注册按钮 -->
        <button
          class="register-btn"
          @click="handleRegister"
          :loading="loading"
          :disabled="loading"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <!-- 返回登录 -->
        <view class="login-link">
          <text>已有账号？</text>
          <text class="link-text" @click="goToLogin">返回登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import request from '../../utils/request'

const loading = ref(false)

const roleOptions = [
  { label: '电力公司人员', value: 'admin' },
  { label: '社区人员', value: 'normal' }
]
const selectedRoleIndex = ref(-1)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: ''
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: ''
})

// 角色选择
const onRoleChange = (e: any) => {
  selectedRoleIndex.value = e.detail.value
  form.role = roleOptions[selectedRoleIndex.value].value
  errors.role = ''
}

// 表单验证
const validateForm = (): boolean => {
  errors.username = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.role = ''

  let isValid = true

  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    isValid = false
  } else if (form.username.trim().length < 3) {
    errors.username = '用户名长度至少为3个字符'
    isValid = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (form.password.length < 6) {
    errors.password = '密码长度至少为6个字符'
    isValid = false
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
    isValid = false
  } else if (form.confirmPassword !== form.password) {
    errors.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }

  if (!form.role) {
    errors.role = '请选择角色'
    isValid = false
  }

  return isValid
}

// 注册处理
const handleRegister = async () => {
  if (!validateForm()) return

  loading.value = true
  try {
    await request.request({
      url: '/auth/register',
      method: 'POST',
      data: {
        username: form.username.trim(),
        password: form.password,
        role: form.role
      },
      skipAuthRedirect: true
    })

    uni.showToast({
      title: '注册成功，请登录',
      icon: 'success'
    })

    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error: any) {
    console.error('注册失败:', error)
    const msg: string = error?.message || ''

    if (msg.includes('409') || msg.includes('已存在')) {
      // 用户名冲突
      errors.username = '用户名已存在，请选择其他用户名'
    } else if (
      msg.includes('未授权') ||
      msg.includes('权限不足') ||
      msg.includes('资源不存在') ||
      msg.includes('服务器错误') ||
      msg.includes('网络连接失败')
    ) {
      // request.ts 已弹出 toast，无需重复提示
    } else {
      uni.showToast({ title: '注册失败，请稍后重试', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

// 返回登录页
const goToLogin = () => {
  uni.navigateBack()
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.register-card {
  width: 100%;
  max-width: 600rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.2);
}

.logo-section {
  text-align: center;
  margin-bottom: 50rpx;
}

.logo-icon {
  font-size: 80rpx;
  display: block;
  margin-bottom: 16rpx;
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
  margin-bottom: 28rpx;
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
  border: 2rpx solid transparent;
}

.form-input-error {
  border-color: #f56c6c !important;
  background-color: #fff0f0;
}

.input-placeholder {
  color: #c0c4cc;
}

.error-tip {
  font-size: 22rpx;
  color: #f56c6c;
  margin-top: 8rpx;
  display: block;
}

/* 角色选择器 */
.picker-input {
  width: 100%;
  height: 88rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid transparent;
}

.picker-text-placeholder {
  font-size: 28rpx;
  color: #c0c4cc;
}

.picker-text-selected {
  font-size: 28rpx;
  color: #303133;
}

.picker-arrow {
  font-size: 22rpx;
  color: #909399;
}

/* 注册按钮 */
.register-btn {
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
  margin-top: 10rpx;
  margin-bottom: 30rpx;
}

.register-btn:active {
  opacity: 0.8;
}

.register-btn[disabled] {
  opacity: 0.6;
}

/* 返回登录 */
.login-link {
  text-align: center;
  font-size: 26rpx;
  color: #909399;
}

.login-link .link-text {
  color: #409eff;
  margin-left: 8rpx;
}
</style>
