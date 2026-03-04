<template>
  <view class="trend-container">
    <!-- 筛选栏 -->
    <view class="filter-section">
      <view class="filter-row">
        <picker mode="selector" :range="userNames" :value="userIndex" @change="onUserChange">
          <view class="filter-item">
            <text>{{ userNames[userIndex] }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>
      
      <view class="filter-row" style="margin-top: 16rpx;">
        <picker mode="date" :value="startDate" @change="onStartDateChange">
          <view class="filter-item">
            <text>{{ startDate || '开始日期' }}</text>
            <text class="arrow">📅</text>
          </view>
        </picker>
        
        <picker mode="date" :value="endDate" @change="onEndDateChange">
          <view class="filter-item">
            <text>{{ endDate || '结束日期' }}</text>
            <text class="arrow">📅</text>
          </view>
        </picker>
      </view>
      
      <view class="filter-row" style="margin-top: 16rpx;">
        <button class="query-btn" @click="loadChartData">查询</button>
        <button class="reset-btn" @click="resetFilters">重置</button>
      </view>
    </view>

    <!-- 用电量趋势图 -->
    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">用电量趋势</text>
        <text class="section-subtitle">{{ selectedUserName }}</text>
      </view>
      <view class="chart-container">
        <l-echart
          ref="trendChartRef"
          type="2d"
          :beforeDelay="500"
          @finished="onTrendChartFinished"
        ></l-echart>
      </view>
    </view>

    <!-- 用电统计卡片 -->
    <view class="stats-cards">
      <view class="stat-card">
        <text class="stat-label">总用电量</text>
        <text class="stat-value">{{ totalElectricity }} kWh</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">平均日用电</text>
        <text class="stat-value">{{ avgElectricity }} kWh</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">最高日用电</text>
        <text class="stat-value">{{ maxElectricity }} kWh</text>
      </view>
      <view class="stat-card">
        <text class="stat-label">最低日用电</text>
        <text class="stat-value">{{ minElectricity }} kWh</text>
      </view>
    </view>

    <!-- 24小时用电分布 -->
    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">24小时用电分布</text>
        <text class="section-subtitle">平均值</text>
      </view>
      <view class="chart-container">
        <l-echart
          ref="hourChartRef"
          type="2d"
          :beforeDelay="500"
          @finished="onHourChartFinished"
        ></l-echart>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'

// 加载 echarts
let echarts: any = null
// #ifdef MP-WEIXIN
declare const require: any
try {
  echarts = require('../../uni_modules/lime-echart/static/echarts.min')
  console.log('[trend] echarts 加载成功', !!echarts)
} catch (e) {
  console.error('[trend] echarts 加载失败', e)
}
// #endif

interface User {
  id: number
  name: string
}

const userList = ref<User[]>([])
const userNames = computed(() => ['全部用户', ...userList.value.map(u => u.name)])
const userIndex = ref(0)
const selectedUserId = computed(() => {
  if (userIndex.value === 0) return null
  return userList.value[userIndex.value - 1]?.id
})
const selectedUserName = computed(() => userNames.value[userIndex.value])

// 日期范围
const getDefaultStartDate = () => {
  const date = new Date()
  date.setDate(date.getDate() - 7)
  return formatDate(date)
}

const getDefaultEndDate = () => {
  return formatDate(new Date())
}

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const startDate = ref(getDefaultStartDate())
const endDate = ref(getDefaultEndDate())

// 统计数据
const totalElectricity = ref('0')
const avgElectricity = ref('0')
const maxElectricity = ref('0')
const minElectricity = ref('0')

// 图表实例
const trendChartRef = ref<any>(null)
let trendChartIns: any = null
const hourChartRef = ref<any>(null)
let hourChartIns: any = null

// 获取用户列表
const getUserList = async () => {
  try {
    const data: any = await request.get('/users', { page: 1, pageSize: 100 })
    userList.value = (data.records || []).map((u: any) => ({
      id: u.id,
      name: u.name
    }))
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

// 初始化趋势图
const ensureTrendChart = async () => {
  if (trendChartIns) return trendChartIns
  if (!trendChartRef.value || !echarts) return null
  
  try {
    trendChartIns = await trendChartRef.value.init(echarts)
    trendChartRef.value?.resize?.()
    return trendChartIns
  } catch (e) {
    console.error('[trend] 趋势图初始化失败', e)
    return null
  }
}

// 初始化小时分布图
const ensureHourChart = async () => {
  if (hourChartIns) return hourChartIns
  if (!hourChartRef.value || !echarts) return null
  
  try {
    hourChartIns = await hourChartRef.value.init(echarts)
    hourChartRef.value?.resize?.()
    return hourChartIns
  } catch (e) {
    console.error('[trend] 小时分布图初始化失败', e)
    return null
  }
}

// 趋势图配置
const buildTrendOption = (xData: string[], yData: number[]) => {
  return {
    tooltip: { 
      trigger: 'axis',
      confine: true,
      formatter: '{b}<br/>{a}: {c} kWh'
    },
    grid: { left: 12, right: 12, top: 20, bottom: 12, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      axisLabel: { 
        color: '#606266', 
        fontSize: 10,
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      axisLabel: { color: '#606266', fontSize: 10 },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        name: '用电量',
        type: 'line',
        smooth: true,
        showSymbol: true,
        symbolSize: 6,
        lineStyle: { width: 2, color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.18)' },
        data: yData
      }
    ]
  }
}

// 小时分布图配置
const buildHourOption = (xData: string[], yData: number[]) => {
  return {
    tooltip: { 
      trigger: 'axis',
      confine: true,
      formatter: '{b}时<br/>{a}: {c} kWh'
    },
    grid: { left: 12, right: 12, top: 20, bottom: 12, containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { color: '#606266', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: 'kWh',
      axisLabel: { color: '#606266', fontSize: 10 },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        name: '平均用电量',
        type: 'bar',
        barWidth: '60%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: '#a0cfff' }
            ]
          }
        },
        data: yData
      }
    ]
  }
}

// 加载图表数据
const loadChartData = async () => {
  try {
    uni.showLoading({ title: '加载中...' })
    
    const params: any = {
      startDate: `${startDate.value} 00:00:00`,
      endDate: `${endDate.value} 23:59:59`
    }
    
    if (selectedUserId.value) {
      params.userId = selectedUserId.value
    }
    
    // 获取趋势数据
    let trendData: any[] = []
    try {
      trendData = await request.get('/electricity-data/trend', params)
    } catch (error) {
      console.warn('获取趋势数据失败，使用模拟数据', error)
      // 生成模拟数据
      const start = new Date(startDate.value)
      const end = new Date(endDate.value)
      const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
      
      trendData = Array.from({ length: days }, (_, i) => {
        const date = new Date(start)
        date.setDate(date.getDate() + i)
        return {
          date: formatDate(date),
          value: Math.floor(Math.random() * 50) + 20
        }
      })
    }
    
    const list = Array.isArray(trendData) ? trendData : []
    const xData = list.map(i => String(i?.date ?? '').substring(5))
    const yData = list.map(i => Number(i?.value ?? 0))
    
    // 计算统计数据
    if (yData.length > 0) {
      const total = yData.reduce((a, b) => a + b, 0)
      totalElectricity.value = total.toFixed(2)
      avgElectricity.value = (total / yData.length).toFixed(2)
      maxElectricity.value = Math.max(...yData).toFixed(2)
      minElectricity.value = Math.min(...yData).toFixed(2)
    }
    
    // 绘制趋势图
    const trendIns = await ensureTrendChart()
    if (trendIns) {
      trendIns.setOption(buildTrendOption(xData, yData), true)
    }
    
    // 获取24小时分布数据
    let hourData: any[] = []
    try {
      hourData = await request.get('/electricity-data/hourly-distribution', params)
    } catch (error) {
      console.warn('获取小时分布数据失败，使用模拟数据', error)
      // 生成模拟数据
      hourData = Array.from({ length: 24 }, (_, i) => ({
        hour: i,
        value: Math.floor(Math.random() * 5) + 1
      }))
    }
    
    const hourList = Array.isArray(hourData) ? hourData : []
    const hourXData = hourList.map(i => String(i?.hour ?? 0))
    const hourYData = hourList.map(i => Number(i?.value ?? 0))
    
    // 绘制小时分布图
    const hourIns = await ensureHourChart()
    if (hourIns) {
      hourIns.setOption(buildHourOption(hourXData, hourYData), true)
    }
    
  } catch (error) {
    console.error('加载图表数据失败:', error)
    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

// 事件处理
const onUserChange = (e: any) => {
  userIndex.value = e.detail.value
}

const onStartDateChange = (e: any) => {
  startDate.value = e.detail.value
}

const onEndDateChange = (e: any) => {
  endDate.value = e.detail.value
}

const resetFilters = () => {
  userIndex.value = 0
  startDate.value = getDefaultStartDate()
  endDate.value = getDefaultEndDate()
  loadChartData()
}

const onTrendChartFinished = async () => {
  console.log('[trend] 趋势图组件 finished')
  await ensureTrendChart()
  loadChartData()
}

const onHourChartFinished = async () => {
  console.log('[trend] 小时分布图组件 finished')
  await ensureHourChart()
}

// 下拉刷新
onPullDownRefresh(() => {
  loadChartData().finally(() => {
    uni.stopPullDownRefresh()
  })
})

onMounted(() => {
  console.log('[trend] 页面 mounted')
  getUserList()
})

onUnmounted(() => {
  try {
    trendChartIns?.dispose?.()
    hourChartIns?.dispose?.()
  } catch (e) {
    // ignore
  } finally {
    trendChartIns = null
    hourChartIns = null
  }
})
</script>

<style scoped>
.trend-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20rpx;
}

.filter-section {
  background-color: #ffffff;
  padding: 20rpx;
  margin-bottom: 20rpx;
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

.filter-item .arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
}

.query-btn, .reset-btn {
  flex: 1;
  height: 70rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.query-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.reset-btn {
  background-color: #f5f7fa;
  color: #606266;
}

.chart-section {
  margin: 0 20rpx 20rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}

.section-subtitle {
  font-size: 24rpx;
  color: #909399;
}

.chart-container {
  width: 100%;
  height: 400rpx;
  background-color: #ffffff;
  border-radius: 12rpx;
  overflow: hidden;
  position: relative;
}

.chart-container :deep(l-echart),
.chart-container :deep(.lime-echart),
.chart-container :deep(.lime-echart__canvas) {
  width: 100% !important;
  height: 100% !important;
}

.chart-container :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  padding: 0 20rpx 20rpx;
}

.stat-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 12rpx;
}

.stat-value {
  font-size: 32rpx;
  font-weight: bold;
  color: #409eff;
}
</style>


