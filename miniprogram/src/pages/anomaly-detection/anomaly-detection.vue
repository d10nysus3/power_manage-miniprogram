<template>
  <view class="anomaly-container">
    <!-- 筛选栏 -->
    <view class="filter-section">
      <view class="filter-row">
        <picker mode="selector" :range="anomalyTypes" :value="typeIndex" @change="onTypeChange">
          <view class="filter-item">
            <text>{{ anomalyTypes[typeIndex] }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
        
        <picker mode="selector" :range="statusList" :value="statusIndex" @change="onStatusChange">
          <view class="filter-item">
            <text>{{ statusList[statusIndex] }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>
      
      <view class="filter-row" style="margin-top: 16rpx;">
        <picker mode="date" :value="selectedDate" @change="onDateChange">
          <view class="filter-item filter-date">
            <text>{{ selectedDate || '选择日期' }}</text>
            <text class="arrow">📅</text>
            <text class="clear-icon" v-if="selectedDate" @click.stop="clearDate">✕</text>
          </view>
        </picker>
      </view>
    </view>

    <!-- 异常列表 -->
    <view class="anomaly-list" v-if="anomalyList.length > 0">
      <view 
        class="anomaly-item" 
        v-for="item in anomalyList" 
        :key="item.id"
        @click="viewDetail(item)"
      >
        <view class="item-header">
          <view class="header-left">
            <text class="user-name">{{ item.userName }}</text>
            <view class="type-tag" :class="`tag-${getAnomalyTypeColor(item.alertType)}`">
              {{ formatAnomalyType(item.alertType) }}
            </view>
          </view>
          <view class="severity-tag" :class="`severity-${item.severityLevel}`">
            {{ formatSeverity(item.severityLevel) }}
          </view>
        </view>

        <view class="item-content">
          <view class="content-row">
            <text class="label">地址:</text>
            <text class="value">{{ item.userAddress }}</text>
          </view>
          <view class="content-row">
            <text class="label">时间:</text>
            <text class="value">{{ formatDateTime(item.alertTime) }}</text>
          </view>
          <view class="content-row">
            <text class="label">详情:</text>
            <text class="value">{{ item.alertContent }}</text>
          </view>
        </view>

        <view class="item-footer">
          <view class="status-tag" :class="item.isProcessed ? 'tag-success' : 'tag-warning'">
            {{ item.isProcessed ? '✓ 已处理' : '⏳ 待处理' }}
          </view>
          <text class="view-detail">查看详情 →</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">⚠️</text>
      <text class="empty-text">暂无异常记录</text>
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

    <!-- 异常详情弹窗 -->
    <view class="detail-modal" v-if="showDetailModal" @click="closeDetailModal">
      <view class="detail-content" @click.stop>
        <view class="detail-header">
          <text class="detail-title">异常详情</text>
        </view>
        
        <view class="detail-body">
          <view class="detail-row">
            <text class="detail-label">异常ID</text>
            <text class="detail-value">{{ selectedAnomaly?.id }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">用户姓名</text>
            <text class="detail-value">{{ selectedAnomaly?.userName }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">异常类型</text>
            <text class="detail-value">{{ formatAnomalyType(selectedAnomaly?.alertType || '') }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">严重程度</text>
            <text class="detail-value">{{ formatSeverity(selectedAnomaly?.severityLevel || '') }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">检测时间</text>
            <text class="detail-value">{{ formatDateTime(selectedAnomaly?.alertTime || '') }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">用户地址</text>
            <text class="detail-value">{{ selectedAnomaly?.userAddress || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">异常详情</text>
            <text class="detail-value">{{ selectedAnomaly?.alertContent || '-' }}</text>
          </view>
          
          <view class="detail-row">
            <text class="detail-label">处理状态</text>
            <text class="detail-value">{{ selectedAnomaly?.isProcessed ? '已处理' : '待处理' }}</text>
          </view>
        </view>
        
        <view class="detail-footer" v-if="!selectedAnomaly?.isProcessed">
          <button class="process-btn" @click="handleMarkAsProcessed">标记为已处理</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'
import { formatDateTime, formatAnomalyType, formatSeverity, getAnomalyTypeColor } from '../../utils/format'

interface AnomalyRecord {
  id: number
  userName: string
  alertType: string
  userAddress: string
  alertTime: string
  severityLevel: string
  alertContent: string
  isProcessed: boolean
}

// 与后端 /api/anomaly-records/search 返回的 anomalyType 文案保持一致

const anomalyTypes = ['全部类型', '高峰异常', '模式异常', '趋势异常']
const statusList = ['全部状态', '待处理', '已处理']
const typeIndex = ref(0)
const statusIndex = ref(0)

// 获取当前日期 yyyy-MM-dd 格式
const getTodayDate = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const selectedDate = ref(getTodayDate())

const anomalyList = ref<AnomalyRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showDetailModal = ref(false)
const selectedAnomaly = ref<AnomalyRecord | null>(null)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 根据用户ID获取用户信息
const getUserInfo = async (userId: string) => {
  try {
    const user: any = await request.get(`/users/${userId}`)
    return user
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return null
  }
}

// 获取异常列表
const getAnomalyList = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    
    if (typeIndex.value > 0) {
      params.anomalyType = anomalyTypes[typeIndex.value]
    }
    
    if (statusIndex.value > 0) {
      params.processStatus = statusIndex.value === 2 ? 'processed' : 'unprocessed'
    }
    
    if (selectedDate.value) {
      // 将日期转换为后端需要的格式：yyyy-MM-dd HH:mm:ss
      params.startDate = `${selectedDate.value} 00:00:00`
      params.endDate = `${selectedDate.value} 23:59:59`
    }
    
    const data: any = await request.get('/anomaly-records/search', params)

    const records = Array.isArray(data?.records) ? data.records : []
    
    // 获取每个异常记录对应的用户信息
    const recordsWithUserInfo = await Promise.all(
      records.map(async (record: any) => {
        const userId = record.userId
        let userAddress = '未知地址'
        let userName = record.userName || '未知用户'
        
        // 根据用户ID从用户档案获取完整的用户信息
        if (userId) {
          const userInfo = await getUserInfo(userId)
          if (userInfo) {
            userName = userInfo.name || userName
            userAddress = userInfo.address || '未知地址'
          }
        }
        
        return {
          id: Number(record.id || 0),
          userName: userName,
          alertType: record.anomalyType || '',
          userAddress: userAddress,
          alertTime: record.detectionTime || '',
          severityLevel: record.severityLevel || 'low',
          alertContent: record.description || record.anomalyDetails || '',
          isProcessed: !!record.isProcessed
        }
      })
    )
    
    anomalyList.value = recordsWithUserInfo

    total.value = Number(data?.total || records.length || 0)
    
  } catch (error) {
    console.error('获取异常列表失败:', error)
    uni.showToast({
      title: '获取数据失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

// 类型变化
const onTypeChange = (e: any) => {
  typeIndex.value = e.detail.value
  currentPage.value = 1
  getAnomalyList()
}

// 状态变化
const onStatusChange = (e: any) => {
  statusIndex.value = e.detail.value
  currentPage.value = 1
  getAnomalyList()
}

// 日期变化
const onDateChange = (e: any) => {
  selectedDate.value = e.detail.value
  currentPage.value = 1
  getAnomalyList()
}

// 清除日期筛选
const clearDate = () => {
  selectedDate.value = ''
  currentPage.value = 1
  getAnomalyList()
}

// 查看详情
const viewDetail = (item: AnomalyRecord) => {
  selectedAnomaly.value = item
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedAnomaly.value = null
}

// 处理标记为已处理
const handleMarkAsProcessed = () => {
  if (selectedAnomaly.value) {
    markAsProcessed(selectedAnomaly.value.id)
  }
}

// 标记为已处理
const markAsProcessed = async (id: number) => {
  try {
    // 后端接口为 POST /api/anomaly-records/process/{id}
    await request.post(`/anomaly-records/process/${id}`, {})
    uni.showToast({
      title: '已标记为处理',
      icon: 'success'
    })
    closeDetailModal()
    getAnomalyList()
  } catch (error) {
    console.error('标记失败:', error)
    uni.showToast({
      title: '操作失败',
      icon: 'none'
    })
  }
}

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    getAnomalyList()
  }
}

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    getAnomalyList()
  }
}

// 下拉刷新
onPullDownRefresh(() => {
  currentPage.value = 1
  getAnomalyList().finally(() => {
    uni.stopPullDownRefresh()
  })
})

onMounted(() => {
  getAnomalyList()
})
</script>

<style scoped>
.anomaly-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20rpx;
}

.filter-section {
  background-color: #ffffff;
  padding: 20rpx;
}

.filter-row {
  display: flex;
  gap: 16rpx;
}

.filter-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 24rpx;
  background-color: #f5f7fa;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #606266;
}

.filter-date {
  flex: 2;
  position: relative;
}

.filter-item .arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
}

.clear-icon {
  margin-left: 12rpx;
  font-size: 28rpx;
  color: #909399;
  font-weight: bold;
  padding: 0 8rpx;
}

.anomaly-list {
  padding: 20rpx;
}

.anomaly-item {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.user-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.type-tag {
  padding: 6rpx 12rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
}

.type-tag.tag-danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.type-tag.tag-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.type-tag.tag-info {
  background-color: #f4f4f5;
  color: #909399;
}

.severity-tag {
  padding: 6rpx 12rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: bold;
}

.severity-tag.severity-low {
  background-color: #f4f4f5;
  color: #909399;
}

.severity-tag.severity-medium {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.severity-tag.severity-high,
.severity-tag.severity-critical {
  background-color: #fef0f0;
  color: #f56c6c;
}

.item-content {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-bottom: 16rpx;
}

.content-row {
  display: flex;
  font-size: 26rpx;
}

.content-row .label {
  color: #909399;
  margin-right: 8rpx;
  flex-shrink: 0;
}

.content-row .value {
  color: #606266;
  flex: 1;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.status-tag {
  padding: 6rpx 12rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
}

.status-tag.tag-success {
  background-color: #f0f9ff;
  color: #67c23a;
}

.status-tag.tag-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.view-detail {
  font-size: 24rpx;
  color: #409eff;
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

/* 异常详情弹窗 */
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
  max-height: 80vh;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.detail-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.detail-body {
  padding: 30rpx;
  overflow-y: auto;
  flex: 1;
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
  word-break: break-all;
}

.detail-footer {
  padding: 20rpx 30rpx 30rpx;
  border-top: 1rpx solid #f0f0f0;
  flex-shrink: 0;
}

.process-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #ffffff;
  border-radius: 12rpx;
  font-size: 28rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

