<template>
  <view class="user-profile-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input 
        class="search-input" 
        v-model="searchKeyword" 
        placeholder="搜索用户姓名或地址"
        @confirm="handleSearch"
      />
      <button class="search-btn" @click="handleSearch">搜索</button>
    </view>

    <!-- 筛选器 -->
    <view class="filter-bar">
      <picker mode="selector" :range="userTypes" :value="userTypeIndex" @change="onUserTypeChange">
        <view class="filter-item">
          <text>{{ userTypes[userTypeIndex] }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
    </view>

    <!-- 用户列表 -->
    <view class="user-list" v-if="userList.length > 0">
      <view 
        class="user-item" 
        v-for="user in userList" 
        :key="user.id"
        @click="viewUserDetail(user)"
      >
        <view class="user-header">
          <view class="user-avatar">{{ user.name.charAt(0) }}</view>
          <view class="user-info">
            <text class="user-name">{{ user.name }}</text>
            <text class="user-id">ID: {{ user.id }}</text>
          </view>
          <view class="user-type-tag" :class="`tag-${getUserTypeColor(null, user)}`">
            {{ formatUserType(null, user) }}
          </view>
        </view>
        
        <view class="user-details">
          <view class="detail-item">
            <text class="detail-icon">📱</text>
            <text class="detail-text">{{ user.phoneNumber }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-icon">📍</text>
            <text class="detail-text">{{ user.address }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">👤</text>
      <text class="empty-text">暂无用户数据</text>
    </view>

    <!-- 分页 -->
    <view class="pagination" v-if="total > pageSize">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1"
        @click="prevPage"
      >
        上一页
      </button>
      <text class="page-info">{{ currentPage }} / {{ totalPages }}</text>
      <button 
        class="page-btn" 
        :disabled="currentPage === totalPages"
        @click="nextPage"
      >
        下一页
      </button>
    </view>

    <!-- 用户详情弹窗 -->
    <view class="detail-modal" v-if="showDetailModal" @click="closeDetailModal">
      <view class="detail-content" @click.stop>
        <view class="detail-header">
          <text class="detail-title">用户详情</text>
        </view>
        
        <view class="detail-body">
          <view class="detail-row">
            <text class="detail-label">用户ID</text>
            <text class="detail-value">{{ selectedUser?.id }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">姓名</text>
            <text class="detail-value">{{ selectedUser?.name }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">电话</text>
            <text class="detail-value">{{ selectedUser?.phoneNumber || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">地址</text>
            <text class="detail-value">{{ selectedUser?.address || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">紧急联系人</text>
            <text class="detail-value">{{ selectedUser?.emergencyContactName || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">紧急联系人电话</text>
            <text class="detail-value">{{ selectedUser?.emergencyContactPhone || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">类型</text>
            <text class="detail-value">{{ selectedUserType }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'

interface User {
  id: number
  name: string
  phoneNumber: string
  address: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  isSingleAlone?: boolean
  isCoupleAlone?: boolean
  isMobilityImpaired?: boolean
  isRelianceEquipment?: boolean
  isChronicDisease?: boolean
  isRelianceEnvironment?: boolean
  isProduction?: boolean
  deviceCount?: number
}

const searchKeyword = ref('')
const userTypes = [
  '全部类型',
  '单人独居老人',
  '双人独居老人',
  '行动不便老人',
  '医疗设备依赖',
  '康复/慢病',
  '环境依赖',
  '小型生产用户'
]
const userTypeIndex = ref(0)

const userList = ref<User[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showDetailModal = ref(false)
const selectedUser = ref<User | null>(null)
const selectedUserType = ref('')

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

type SpecialTag = { text: string; color: string }

const getSpecialTag = (user: User): SpecialTag => {
  if (user.isSingleAlone) return { text: '单人独居老人', color: 'warning' }
  if (user.isCoupleAlone) return { text: '双人独居老人', color: 'warning' }
  if (user.isMobilityImpaired) return { text: '行动不便老人', color: 'danger' }
  if (user.isRelianceEquipment) return { text: '医疗设备依赖', color: 'danger' }
  if (user.isChronicDisease) return { text: '康复/慢病', color: 'info' }
  if (user.isRelianceEnvironment) return { text: '环境依赖', color: 'info' }
  if (user.isProduction) return { text: '小型生产用户', color: 'success' }
  return { text: '普通用户', color: 'info' }
}

const getSpecialEndpointByIndex = (idx: number): string | null => {
  const map: Array<string | null> = [
    null,
    '/users/special/single-alone',
    '/users/special/couple-alone',
    '/users/special/mobility-impaired',
    '/users/special/reliance-equipment',
    '/users/special/chronic-disease',
    '/users/special/reliance-environment',
    '/users/special/production'
  ]
  return map[idx] ?? null
}

// 获取用户列表
const getUserList = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    
    const keyword = (searchKeyword.value || '').trim()
    const specialEndpoint = getSpecialEndpointByIndex(userTypeIndex.value)

    // 特殊用户：后端接口返回 List<User>，这里做本地关键词过滤 + 本地分页
    if (specialEndpoint) {
      const list: any[] = await request.get(specialEndpoint)
      const filtered = (Array.isArray(list) ? list : []).filter((u: any) => {
        if (!keyword) return true
        const term = keyword.toLowerCase()
        return (
          String(u?.id ?? '').includes(term) ||
          String(u?.name ?? '').toLowerCase().includes(term) ||
          String(u?.phoneNumber ?? '').includes(term) ||
          String(u?.idCard ?? '').includes(term) ||
          String(u?.meterNumber ?? '').includes(term)
        )
      })

      total.value = filtered.length
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      userList.value = filtered.slice(start, end) as any
      return
    }

    // 全部用户：走后端分页参数 page/pageSize（page 从 1 开始）
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (keyword) params.keyword = keyword

    const data: any = await request.get('/users', params)
    userList.value = data.records || []
    total.value = Number(data.total || 0)
    
  } catch (error) {
    console.error('获取用户列表失败:', error)
    uni.showToast({
      title: '获取数据失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getUserList()
}

// 用户类型变化
const onUserTypeChange = (e: any) => {
  userTypeIndex.value = e.detail.value
  currentPage.value = 1
  getUserList()
}

// 兼容旧模板调用（现在通过布尔字段动态计算标签）
const formatUserType = (_: any, user?: User): string => {
  return getSpecialTag(user || ({} as User)).text
}
const getUserTypeColor = (_: any, user?: User): string => {
  return getSpecialTag(user || ({} as User)).color
}

// 查看用户详情
const viewUserDetail = (user: User) => {
  const tag = getSpecialTag(user)
  selectedUser.value = user
  selectedUserType.value = tag.text
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedUser.value = null
  selectedUserType.value = ''
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    getUserList()
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    getUserList()
  }
}

// 下拉刷新
onPullDownRefresh(() => {
  currentPage.value = 1
  getUserList().finally(() => {
    uni.stopPullDownRefresh()
  })
})

onMounted(() => {
  getUserList()
})
</script>

<style scoped>
.user-profile-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20rpx;
}

.search-bar {
  background-color: #ffffff;
  padding: 20rpx;
  display: flex;
  gap: 16rpx;
}

.search-input {
  flex: 1;
  height: 70rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.search-btn {
  width: 140rpx;
  height: 70rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-radius: 12rpx;
  font-size: 28rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-bar {
  background-color: #ffffff;
  padding: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.filter-item {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  background-color: #f5f7fa;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #606266;
}

.filter-item .arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
}

.user-list {
  padding: 20rpx;
}

.user-item {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.user-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.user-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: bold;
  margin-right: 16rpx;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4rpx;
}

.user-id {
  font-size: 24rpx;
  color: #909399;
}

.user-type-tag {
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.user-type-tag.tag-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.user-type-tag.tag-danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.user-type-tag.tag-info {
  background-color: #f4f4f5;
  color: #909399;
}

.user-type-tag.tag-success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #606266;
}

.detail-icon {
  margin-right: 12rpx;
  font-size: 28rpx;
}

.empty-state {
  height: 600rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-state .empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-state .empty-text {
  font-size: 28rpx;
  color: #909399;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx 20rpx;
  gap: 20rpx;
}

.page-btn {
  padding: 16rpx 32rpx;
  background-color: #ffffff;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #606266;
}

.page-btn[disabled] {
  opacity: 0.5;
}

.page-info {
  font-size: 26rpx;
  color: #606266;
}

/* 用户详情弹窗 */
.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.detail-content {
  width: 600rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.detail-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.detail-body {
  padding: 30rpx;
  max-height: 800rpx;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 30rpx;
  padding-bottom: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.detail-row:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.detail-label {
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 12rpx;
}

.detail-value {
  font-size: 28rpx;
  color: #303133;
  line-height: 1.6;
}
</style>

