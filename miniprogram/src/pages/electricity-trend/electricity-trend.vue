<template>
  <view class="trend-container">
    <!-- 用户选择器 -->
    <view class="user-selector">
      <picker mode="selector" :range="userList" range-key="name" :value="selectedUserIndex" @change="onUserChange">
        <view class="selector-item">
          <text class="selector-label">选择用户</text>
          <view class="selector-value">
            <text>{{ selectedUser ? selectedUser.name : '请选择用户' }}</text>
            <text class="arrow">▼</text>
          </view>
        </view>
      </picker>
    </view>

    <!-- 时间范围选择 - 移除，固定为7天 -->
    <!-- <view class="time-range-selector">
      <view 
        class="range-item" 
        :class="{ active: timeRange === 7 }"
        @click="selectTimeRange(7)"
      >
        近7天
      </view>
      <view 
        class="range-item" 
        :class="{ active: timeRange === 30 }"
        @click="selectTimeRange(30)"
      >
        近30天
      </view>
    </view> -->

    <!-- 图表容器 -->
    <view class="chart-container" v-if="selectedUser">
      <view class="chart-title">{{ selectedUser.name }} - 每日用电趋势</view>
      <view class="chart-wrapper">
        <l-echart ref="chartRef" type="2d" :beforeDelay="500" @finished="onChartInit"></l-echart>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">📊</text>
      <text class="empty-text">请选择用户查看用电趋势</text>
    </view>

    <!-- 统计信息 -->
    <view class="statistics" v-if="selectedUser && chartData.length > 0">
      <view class="stat-card">
        <text class="stat-label">总用电量</text>
        <text class="stat-value">{{ totalConsumption.toFixed(2) }} kWh</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">平均日用电量</text>
        <text class="stat-value">{{ avgConsumption.toFixed(2) }} kWh</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">最高日用电量</text>
        <text class="stat-value">{{ maxConsumption.toFixed(2) }} kWh</text>
      </view>
    </view>

    <!-- 无数据提示 -->
    <view class="no-data-tip" v-if="selectedUser && chartData.length === 0">
      <text class="tip-icon">📭</text>
      <text class="tip-text">该用户在所选时间段内暂无用电数据</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../../utils/request'

interface User {
  id: number
  name: string
  address: string
}

interface ElectricityData {
  recordTime: string
  electricityConsumption: number
  power: number
}

// 加载 echarts
let echarts: any = null
// #ifdef MP-WEIXIN
declare const require: any
try {
  echarts = require('../../uni_modules/lime-echart/static/echarts.min')
  console.log('[electricity-trend] echarts 加载成功', !!echarts)
} catch (e) {
  console.error('[electricity-trend] echarts 加载失败', e)
}
// #endif

const userList = ref<User[]>([])
const selectedUserIndex = ref(-1)
const selectedUser = ref<User | null>(null)
const timeRange = ref(7) // 固定为7天
const chartData = ref<any[]>([])
const chartRef = ref<any>(null)
let chartInstance: any = null

// 统计数据
const totalConsumption = computed(() => {
  return chartData.value.reduce((sum, item) => sum + item.value, 0)
})

const avgConsumption = computed(() => {
  return chartData.value.length > 0 ? totalConsumption.value / chartData.value.length : 0
})

const maxConsumption = computed(() => {
  return chartData.value.length > 0 ? Math.max(...chartData.value.map(item => item.value)) : 0
})

// 获取用户列表
const getUserList = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    const data: any = await request.get('/users', { page: 1, pageSize: 1000 })
    userList.value = data.records || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
    uni.showToast({
      title: '获取用户列表失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

// 用户选择变化
const onUserChange = (e: any) => {
  selectedUserIndex.value = e.detail.value
  selectedUser.value = userList.value[selectedUserIndex.value]
  loadElectricityData()
}

// 获取用电数据
const loadElectricityData = async () => {
  if (!selectedUser.value) return

  try {
    uni.showLoading({ title: '加载数据...' })
    
    // 计算时间范围
    const endTime = new Date()
    const startTime = new Date()
    startTime.setDate(startTime.getDate() - timeRange.value)
    
    const formatDateTime = (date: Date) => {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
    
    const startTimeStr = formatDateTime(startTime)
    const endTimeStr = formatDateTime(endTime)
    
    console.log('请求用电数据:', {
      userId: selectedUser.value.id,
      startTime: startTimeStr,
      endTime: endTimeStr
    })
    
    // 使用与档案管理相同的方式获取用电数据
    const data: ElectricityData[] = await request.get(
      `/electricity-data/user/${selectedUser.value.id}/time-range`,
      {
        startTime: startTimeStr,
        endTime: endTimeStr
      }
    )
    
    console.log('获取到的用电数据:', data)
    
    if (!data || data.length === 0) {
      chartData.value = []
      const ins = await ensureChart()
      if (ins) {
        ins.clear()
      }
      return
    }
    
    // 处理数据：按日期聚合
    const dailyData = aggregateByDay(data)
    chartData.value = dailyData
    
    console.log('聚合后的每日数据:', dailyData)
    
    // 更新图表
    updateChart(dailyData)
    
  } catch (error) {
    console.error('获取用电数据失败:', error)
    uni.showToast({
      title: '获取数据失败',
      icon: 'none'
    })
    chartData.value = []
  } finally {
    uni.hideLoading()
  }
}

// 按日期聚合数据，只保留最近7天
const aggregateByDay = (data: ElectricityData[]) => {
  const dailyMap = new Map<string, number>()
  
  // 聚合每天的用电量
  data.forEach(item => {
    const date = item.recordTime.split(' ')[0]
    const current = dailyMap.get(date) || 0
    dailyMap.set(date, current + item.electricityConsumption)
  })
  
  // 转换为数组并排序
  let result = Array.from(dailyMap.entries())
    .map(([date, value]) => ({ date, value }))
    .sort((a, b) => a.date.localeCompare(b.date))
  
  // 只保留最近7天的数据
  if (result.length > 7) {
    result = result.slice(-7)
  }
  
  console.log('[electricity-trend] 聚合后保留最近7天数据:', result)
  
  return result
}

// 确保图表实例存在
const ensureChart = async () => {
  if (chartInstance) {
    console.log('[electricity-trend] 图表已存在，直接返回')
    return chartInstance
  }
  if (!chartRef.value) {
    console.error('[electricity-trend] chartRef.value 为空')
    return null
  }
  if (!echarts) {
    console.error('[electricity-trend] echarts 未加载，无法初始化图表')
    return null
  }
  console.log('[electricity-trend] 开始初始化图表...')
  try {
    chartInstance = await chartRef.value.init(echarts)
    console.log('[electricity-trend] 图表初始化成功', !!chartInstance)
    chartRef.value?.resize?.()
    return chartInstance
  } catch (e) {
    console.error('[electricity-trend] 图表初始化失败', e)
    return null
  }
}

// 图表初始化回调
const onChartInit = async () => {
  console.log('[electricity-trend] 图表组件 finished 事件触发')
  const ins = await ensureChart()
  if (ins) {
    console.log('[electricity-trend] 图表初始化完成')
    // 如果已经有数据，立即更新图表
    if (selectedUser.value && chartData.value.length > 0) {
      updateChart(chartData.value)
    }
  } else {
    console.error('[electricity-trend] 图表初始化失败')
  }
}

// 更新图表
const updateChart = async (data: any[]) => {
  const ins = await ensureChart()
  if (!ins || data.length === 0) {
    console.error('[electricity-trend] 图表实例不存在或数据为空')
    return
  }
  
  const dates = data.map(item => {
    // 格式化日期为 MM/DD
    const parts = item.date.split('-')
    return `${parts[1]}/${parts[2]}`
  })
  const values = data.map(item => Number(item.value.toFixed(2)))
  
  console.log('[electricity-trend] 设置图表 option', { dates, values })
  
  // 使用与首页相同的图表配置
  const option = {
    tooltip: { 
      trigger: 'axis', 
      confine: true 
    },
    grid: { 
      left: 12, 
      right: 12, 
      top: 20, 
      bottom: 12, 
      containLabel: true 
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: { 
        color: '#606266', 
        fontSize: 10 
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: { 
        color: '#606266', 
        fontSize: 10 
      },
      splitLine: { 
        lineStyle: { 
          color: '#ebeef5' 
        } 
      }
    },
    series: [
      {
        name: '用电量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { 
          width: 2, 
          color: '#409eff' 
        },
        areaStyle: { 
          color: 'rgba(64,158,255,0.18)' 
        },
        data: values
      }
    ]
  }
  
  ins.setOption(option, true)
  console.log('[electricity-trend] 图表更新成功')
}

onMounted(() => {
  console.log('[electricity-trend] 页面 mounted')
  console.log('[electricity-trend] echarts 是否加载:', !!echarts)
  getUserList()
})

onUnmounted(() => {
  try {
    chartInstance?.dispose?.()
  } catch (e) {
    // ignore
  } finally {
    chartInstance = null
  }
})
</script>

<style scoped>
.trend-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20rpx;
}

.user-selector {
  background-color: #ffffff;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.selector-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.selector-label {
  font-size: 24rpx;
  color: #909399;
}

.selector-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx;
  background-color: #f5f7fa;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #303133;
}

.selector-value .arrow {
  font-size: 20rpx;
  color: #909399;
}

.chart-container {
  background-color: #ffffff;
  padding: 20rpx;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.chart-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20rpx;
  text-align: center;
}

.chart-wrapper {
  width: 100%;
  height: 400rpx;
  background-color: #ffffff;
  border-radius: 12rpx;
  overflow: hidden;
  position: relative;
}

.chart-wrapper :deep(l-echart),
.chart-wrapper :deep(.lime-echart),
.chart-wrapper :deep(.lime-echart__canvas) {
  width: 100% !important;
  height: 100% !important;
}

.chart-wrapper :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
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

.no-data-tip {
  height: 400rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.no-data-tip .tip-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.no-data-tip .tip-text {
  font-size: 26rpx;
  color: #909399;
  text-align: center;
  padding: 0 40rpx;
}

.statistics {
  display: flex;
  gap: 20rpx;
  padding: 0 20rpx;
}

.stat-card {
  flex: 1;
  background-color: #ffffff;
  padding: 24rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.stat-label {
  font-size: 24rpx;
  color: #909399;
}

.stat-value {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}
</style>

