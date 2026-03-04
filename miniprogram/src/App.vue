<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
  // 检查登录状态
  checkLoginStatus()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})

// 检查登录状态
const checkLoginStatus = () => {
  const token = uni.getStorageSync('token')
  if (!token) {
    // 如果没有token，跳转到登录页
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 通用容器 */
.container {
  padding: 20rpx;
}

/* 卡片样式 */
.card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

/* 按钮样式 */
.btn-primary {
  background-color: #409eff;
  color: #ffffff;
  border-radius: 8rpx;
  padding: 20rpx 40rpx;
  border: none;
}

.btn-success {
  background-color: #67c23a;
  color: #ffffff;
}

.btn-warning {
  background-color: #e6a23c;
  color: #ffffff;
}

.btn-danger {
  background-color: #f56c6c;
  color: #ffffff;
}

/* 标签样式 */
.tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
  font-size: 24rpx;
}

.tag-success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.tag-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.tag-danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.tag-info {
  background-color: #f4f4f5;
  color: #909399;
}

/* 文本样式 */
.text-primary {
  color: #409eff;
}

.text-success {
  color: #67c23a;
}

.text-warning {
  color: #e6a23c;
}

.text-danger {
  color: #f56c6c;
}

.text-info {
  color: #909399;
}

/* 间距工具类 */
.mt-10 { margin-top: 10rpx; }
.mt-20 { margin-top: 20rpx; }
.mt-30 { margin-top: 30rpx; }
.mb-10 { margin-bottom: 10rpx; }
.mb-20 { margin-bottom: 20rpx; }
.mb-30 { margin-bottom: 30rpx; }
.ml-10 { margin-left: 10rpx; }
.mr-10 { margin-right: 10rpx; }

.pt-10 { padding-top: 10rpx; }
.pt-20 { padding-top: 20rpx; }
.pb-10 { padding-bottom: 10rpx; }
.pb-20 { padding-bottom: 20rpx; }

/* Flex 布局 */
.flex {
  display: flex;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-column {
  display: flex;
  flex-direction: column;
}
</style>

