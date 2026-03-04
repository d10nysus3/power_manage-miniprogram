<template>
  <view class="dashboard-container">
    <!-- 顶部欢迎区域 -->
    <view class="welcome-section">
      <view class="welcome-text">
        <text class="greeting">你好，{{ username }}</text>
        <text class="subtitle">欢迎使用电力监测系统</text>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="status-cards">
      <view class="status-card" v-for="(item, index) in statusCards" :key="index">
        <view class="card-icon" :style="{ backgroundColor: item.color }">
          <text class="icon">{{ item.icon }}</text>
        </view>
        <view class="card-info">
          <text class="card-value">{{ item.value }}</text>
          <text class="card-label">{{ item.label }}</text>
          <text class="card-trend" :class="item.trendUp ? 'trend-up' : 'trend-down'">
            {{ item.trend }}
          </text>
        </view>
      </view>
    </view>

    <!-- 图表区域 -->
    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">用电量趋势</text>
        <picker mode="selector" :range="timeRanges" :value="timeRangeIndex" @change="onTimeRangeChange">
          <view class="time-picker">
            <text>{{ timeRanges[timeRangeIndex] }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="chart-container">
        <l-echart
          ref="chartRef"
          type="2d"
          :beforeDelay="500"
          @finished="onTrendChartFinished"
        ></l-echart>
      </view>
    </view>

    <!-- 异常类型分布 -->
    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">异常类型分布</text>
        <text class="section-subtitle">近30天</text>
      </view>
      <view class="chart-container">
        <l-echart
          ref="typeChartRef"
          type="2d"
          :beforeDelay="500"
          @finished="onTypeChartFinished"
        ></l-echart>
      </view>
    </view>

    <!-- 最近异常记录 -->
    <view class="records-section">
      <view class="section-header">
        <text class="section-title">最近异常记录</text>
        <text class="view-all" @click="viewAllRecords">查看全部 →</text>
      </view>
      
      <view class="records-list" v-if="recentRecords.length > 0">
        <view 
          class="record-item" 
          v-for="record in recentRecords" 
          :key="record.id"
          @click="viewRecordDetail(record)"
        >
          <view class="record-header">
            <text class="record-user">{{ record.userName }}</text>
            <view class="record-tag" :class="`tag-${getAnomalyTypeColor(record.alertType)}`">
              {{ formatAnomalyType(record.alertType) }}
            </view>
          </view>
          <view class="record-content">
            <text class="record-location">📍 {{ record.triggerReason }}</text>
            <text class="record-time">🕐 {{ formatDateTime(record.alertTime) }}</text>
          </view>
          <view class="record-footer">
            <view class="status-tag" :class="record.isProcessed ? 'tag-success' : 'tag-warning'">
              {{ record.isProcessed ? '已处理' : '待处理' }}
            </view>
          </view>
        </view>
      </view>
      
      <view class="empty-state" v-else>
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无异常记录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'
import { getUserInfo } from '../../utils/auth'
import { formatDateTime, formatAnomalyType, getAnomalyTypeColor } from '../../utils/format'

const username = ref('')
const timeRanges = ['近7天', '近30天', '近90天']
const timeRangeIndex = ref(0)

// 当前项目主要运行在微信小程序
let echarts: any = null
// #ifdef MP-WEIXIN
// require 仅支持相对路径
declare const require: any
try {
  echarts = require('../../uni_modules/lime-echart/static/echarts.min')
  console.log('[dashboard] echarts 加载成功', !!echarts)
} catch (e) {
  console.error('[dashboard] echarts 加载失败', e)
}
// #endif

interface StatusCard {
  icon: string
  label: string
  value: string
  trend: string
  trendUp: boolean
  color: string
}

interface AnomalyRecord {
  id: number
  userName: string
  alertType: string
  triggerReason: string
  alertTime: string
  isProcessed: boolean
}

const statusCards = reactive<StatusCard[]>([
  { icon: '👥', label: '用户总数', value: '0', trend: '+0%', trendUp: true, color: '#409eff' },
  { icon: '⚠️', label: '今日异常', value: '0', trend: '+0%', trendUp: false, color: '#f56c6c' },
  { icon: '✅', label: '处理率', value: '0%', trend: '+0%', trendUp: true, color: '#67c23a' },
  { icon: '📊', label: '今日用电', value: '0kWh', trend: '+0%', trendUp: true, color: '#e6a23c' }
])

const recentRecords = ref<AnomalyRecord[]>([])

// 图表实例（不要放到响应式里）
const chartRef = ref<any>(null)
let chartIns: any = null
const typeChartRef = ref<any>(null)
let typeChartIns: any = null

const getTrendDays = (): number => {
  const map = [7, 30, 90]
  return map[timeRangeIndex.value] || 7
}

const buildTrendOption = (xData: string[], yData: number[]) => {
  return {
    tooltip: { trigger: 'axis', confine: true },
    grid: { left: 12, right: 12, top: 20, bottom: 12, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      axisLabel: { color: '#606266', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#606266', fontSize: 10 },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        name: '用电量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.18)' },
        data: yData
      }
    ]
  }
}

const ensureChart = async () => {
  if (chartIns) {
    console.log('[dashboard] 趋势图已存在，直接返回')
    return chartIns
  }
  if (!chartRef.value) {
    console.error('[dashboard] chartRef.value 为空')
    return null
  }
  if (!echarts) {
    console.error('[dashboard] echarts 未加载，无法初始化趋势图')
    return null
  }
  console.log('[dashboard] 开始初始化趋势图...')
  try {
    chartIns = await chartRef.value.init(echarts)
    console.log('[dashboard] 趋势图初始化成功', !!chartIns)
    // 尺寸兜底
    chartRef.value?.resize?.()
    return chartIns
  } catch (e) {
    console.error('[dashboard] 趋势图初始化失败', e)
    return null
  }
}

const ensureTypeChart = async () => {
  if (typeChartIns) {
    console.log('[dashboard] 类型分布图已存在，直接返回')
    return typeChartIns
  }
  if (!typeChartRef.value) {
    console.error('[dashboard] typeChartRef.value 为空')
    return null
  }
  if (!echarts) {
    console.error('[dashboard] echarts 未加载，无法初始化类型分布图')
    return null
  }
  console.log('[dashboard] 开始初始化类型分布图...')
  try {
    typeChartIns = await typeChartRef.value.init(echarts)
    console.log('[dashboard] 类型分布图初始化成功', !!typeChartIns)
    typeChartRef.value?.resize?.()
    return typeChartIns
  } catch (e) {
    console.error('[dashboard] 类型分布图初始化失败', e)
    return null
  }
}

const onTrendChartFinished = async () => {
  console.log('[dashboard] 趋势图组件 finished 事件触发')
  // 组件 mounted 完成后再 init，避免容器尺寸为 0
  const ins = await ensureChart()
  if (ins) {
    console.log('[dashboard] 趋势图初始化完成，开始加载数据')
    // init 成功后再拉数据绘制
    loadElectricityTrend()
  } else {
    console.error('[dashboard] 趋势图初始化失败')
  }
}

const onTypeChartFinished = async () => {
  console.log('[dashboard] 类型分布图组件 finished 事件触发')
  const ins = await ensureTypeChart()
  if (ins) {
    console.log('[dashboard] 类型分布图初始化完成，开始加载数据')
    loadAnomalyTypeDistribution()
  } else {
    console.error('[dashboard] 类型分布图初始化失败')
  }
}

const loadElectricityTrend = async () => {
  try {
    console.log('[dashboard] 开始加载用电趋势数据')
    const days = getTrendDays()
    let stats: any[] = []
    
    try {
      stats = await request.get('/electricity-data/daily-stats', { days })
      console.log('[dashboard] 用电趋势数据获取成功', stats)
    } catch (error) {
      console.warn('[dashboard] 用电趋势数据获取失败，使用模拟数据', error)
      // 使用模拟数据
      stats = Array.from({ length: days }, (_, i) => {
        const date = new Date()
        date.setDate(date.getDate() - (days - i - 1))
        return {
          date: `${date.getMonth() + 1}/${date.getDate()}`,
          value: Math.floor(Math.random() * 500) + 300
        }
      })
    }
    
    const list = Array.isArray(stats) ? stats : []
    if (list.length === 0) {
      console.warn('[dashboard] 数据为空，使用默认数据')
      // 生成默认数据
      for (let i = 0; i < days; i++) {
        const date = new Date()
        date.setDate(date.getDate() - (days - i - 1))
        list.push({
          date: `${date.getMonth() + 1}/${date.getDate()}`,
          value: Math.floor(Math.random() * 500) + 300
        })
      }
    }
    
    const xData = list.map(i => String(i?.date ?? ''))
    const yData = list.map(i => Number(i?.value ?? 0))
    console.log('[dashboard] 处理后的数据', { xData, yData })
    
    const ins = await ensureChart()
    if (!ins) {
      console.error('[dashboard] 图表实例不存在，无法设置 option')
      return
    }
    console.log('[dashboard] 设置趋势图 option')
    ins.setOption(buildTrendOption(xData, yData), true)
  } catch (error) {
    console.error('获取用电趋势失败:', error)
  }
}

const buildTypeDistributionOption = (data: any[]) => {
  const seriesData = (Array.isArray(data) ? data : []).map((i: any) => ({
    name: String(i?.name ?? i?.anomalyType ?? ''),
    value: Number(i?.value ?? 0),
    itemStyle: i?.itemStyle
  }))

  return {
    tooltip: { trigger: 'item', confine: true },
    legend: {
      type: 'scroll',
      bottom: 0,
      textStyle: { color: '#606266', fontSize: 10 }
    },
    series: [
      {
        name: '异常类型',
        type: 'pie',
        radius: ['40%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: seriesData
      }
    ]
  }
}

const loadAnomalyTypeDistribution = async () => {
  try {
    console.log('[dashboard] 开始加载异常类型分布数据')
    let data: any[] = []
    
    try {
      data = await request.get('/anomalies/type-distribution')
      console.log('[dashboard] 异常类型分布数据获取成功', data)
    } catch (error) {
      console.warn('[dashboard] 异常类型分布数据获取失败，使用模拟数据', error)
      // 使用模拟数据
      data = [
        { name: '电压异常', value: 35, itemStyle: { color: '#f56c6c' } },
        { name: '电流异常', value: 28, itemStyle: { color: '#e6a23c' } },
        { name: '功率异常', value: 22, itemStyle: { color: '#409eff' } },
        { name: '频率异常', value: 15, itemStyle: { color: '#67c23a' } }
      ]
    }
    
    const list = Array.isArray(data) ? data : []
    if (list.length === 0) {
      console.warn('[dashboard] 数据为空，使用默认数据')
      data = [
        { name: '电压异常', value: 35, itemStyle: { color: '#f56c6c' } },
        { name: '电流异常', value: 28, itemStyle: { color: '#e6a23c' } },
        { name: '功率异常', value: 22, itemStyle: { color: '#409eff' } },
        { name: '频率异常', value: 15, itemStyle: { color: '#67c23a' } }
      ]
    }
    
    const ins = await ensureTypeChart()
    if (!ins) {
      console.error('[dashboard] 类型分布图实例不存在，无法设置 option')
      return
    }
    console.log('[dashboard] 设置类型分布图 option')
    ins.setOption(buildTypeDistributionOption(data), true)
  } catch (error) {
    console.error('获取异常类型分布失败:', error)
  }
}

// 获取系统状态
const getSystemStatus = async () => {
  try {
    const data: any = await request.get('/dashboard/system-status')
    
    if (data) {
      statusCards[0].value = data.totalUsers?.toLocaleString() || '0'
      statusCards[0].trend = String(data.userTrend || '+0%')
      statusCards[0].trendUp = String(data.userTrend || '').startsWith('+')
      
      statusCards[1].value = data.todayAnomalies?.toString() || '0'
      statusCards[1].trend = String(data.anomalyTrend || '+0%')
      statusCards[1].trendUp = String(data.anomalyTrend || '').startsWith('+')
      
      statusCards[2].value = `${data.processingRate || 0}%`
      statusCards[2].trend = String(data.rateTrend || '+0%')
      statusCards[2].trendUp = String(data.rateTrend || '').startsWith('+')
      
      statusCards[3].value = `${data.todayElectricity || 0}万kWh`
      statusCards[3].trend = String(data.electricityTrend || '+0%')
      statusCards[3].trendUp = String(data.electricityTrend || '').startsWith('+')
    }
  } catch (error) {
    console.error('获取系统状态失败:', error)
  }
}

// 获取最近异常记录
const getRecentRecords = async () => {
  try {
    const resp: any = await request.get('/anomalies/recent', { limit: 5 })
    const data = resp?.records || []
    
    const userNameMap: Record<string, string> = {
      '用户1': '张三', '用户2': '李四', '用户3': '王五',
      '用户4': '赵六', '用户5': '钱七'
    }
    
    recentRecords.value = data.map((record: any) => ({
      id: record.id || 0,
      userName: userNameMap[record.userName] || record.userName || '未知用户',
      alertType: record.alertType || record.anomalyType || '',
      triggerReason: record.triggerReason || record.location || '',
      alertTime: record.alertTime || record.detectionTime || '',
      isProcessed: record.isProcessed || false
    }))
  } catch (error) {
    console.error('获取异常记录失败:', error)
  }
}

// 时间范围变化
const onTimeRangeChange = (e: any) => {
  timeRangeIndex.value = e.detail.value
  loadElectricityTrend()
}

// 查看所有记录
const viewAllRecords = () => {
  uni.switchTab({
    url: '/pages/anomaly-detection/anomaly-detection'
  })
}

// 查看记录详情
const viewRecordDetail = (record: AnomalyRecord) => {
  uni.navigateTo({
    url: `/pages/anomaly-detection/anomaly-detection?recordId=${record.id}`
  })
}

// 下拉刷新
onPullDownRefresh(() => {
  Promise.all([
    getSystemStatus(),
    getRecentRecords(),
    loadElectricityTrend(),
    loadAnomalyTypeDistribution()
  ]).finally(() => {
    uni.stopPullDownRefresh()
  })
})

onMounted(() => {
  console.log('[dashboard] 页面 mounted')
  console.log('[dashboard] echarts 是否加载:', !!echarts)
  console.log('[dashboard] chartRef:', !!chartRef.value)
  console.log('[dashboard] typeChartRef:', !!typeChartRef.value)
  
  const userInfo = getUserInfo()
  username.value = userInfo.username || '用户'
  
  getSystemStatus()
  getRecentRecords()
  // 图表在 @finished 后初始化并绘制，这里不抢跑
})

onUnmounted(() => {
  try {
    chartIns?.dispose?.()
    typeChartIns?.dispose?.()
  } catch (e) {
    // ignore
  } finally {
    chartIns = null
    typeChartIns = null
  }
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20rpx;
}

.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 30rpx 40rpx;
  color: #ffffff;
}

.welcome-text .greeting {
  font-size: 40rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 10rpx;
}

.welcome-text .subtitle {
  font-size: 26rpx;
  opacity: 0.9;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  padding: 20rpx;
  margin-top: -40rpx;
}

.status-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
}

.card-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
}

.card-icon .icon {
  font-size: 40rpx;
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-value {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4rpx;
}

.card-label {
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 4rpx;
}

.card-trend {
  font-size: 22rpx;
  font-weight: 500;
}

.card-trend.trend-up {
  color: #f56c6c;
}

.card-trend.trend-down {
  color: #67c23a;
}

.chart-section, .records-section {
  margin: 20rpx;
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

.time-picker {
  font-size: 26rpx;
  color: #409eff;
  display: flex;
  align-items: center;
}

.time-picker .arrow {
  margin-left: 8rpx;
  font-size: 20rpx;
}

.view-all {
  font-size: 26rpx;
  color: #409eff;
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

.records-list .record-item {
  background-color: #f5f7fa;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.records-list .record-item:last-child {
  margin-bottom: 0;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.record-user {
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}

.record-tag {
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
}

.record-tag.tag-danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.record-tag.tag-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.record-tag.tag-info {
  background-color: #f4f4f5;
  color: #909399;
}

.record-content {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-bottom: 12rpx;
}

.record-location, .record-time {
  font-size: 24rpx;
  color: #606266;
}

.record-footer {
  display: flex;
  justify-content: flex-end;
}

.status-tag {
  padding: 4rpx 12rpx;
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

.empty-state {
  height: 300rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-state .empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-state .empty-text {
  font-size: 28rpx;
  color: #909399;
}
</style>

