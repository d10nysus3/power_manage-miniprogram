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
          :beforeDelay="30"
          @finished="onTrendChartFinished"
        ></l-echart>
      </view>
    </view>

    <!-- 异常等级分布 -->
    <view class="chart-section">
      <view class="section-header">
        <text class="section-title">异常等级分布</text>
        <text class="section-subtitle">累计</text>
      </view>
      <view class="chart-container">
        <l-echart
          ref="typeChartRef"
          type="2d"
          :beforeDelay="30"
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

// 延迟加载 echarts，避免模块顶层同步 require 1MB 文件阻塞 WAService 线程导致 timeout
let echarts: any = null
// #ifdef MP-WEIXIN
declare const require: any
// #endif
const loadEcharts = (): any => {
  if (echarts) return echarts
  // #ifdef MP-WEIXIN
  try {
    echarts = require('../../uni_modules/lime-echart/static/echarts.min')
    console.log('[dashboard] echarts 加载成功', !!echarts)
  } catch (e) {
    console.error('[dashboard] echarts 加载失败', e)
  }
  // #endif
  return echarts
}

interface StatusCard {
  icon: string
  label: string
  value: string
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
  { icon: '👥', label: '用户总数', value: '0', color: '#409eff' },
  { icon: '⚠️', label: '今日异常', value: '0', color: '#f56c6c' },
  { icon: '✅', label: '处理率', value: '0%', color: '#67c23a' },
  { icon: '📊', label: '今日用电', value: '0kWh', color: '#e6a23c' }
])

const recentRecords = ref<AnomalyRecord[]>([])

// 图表实例（不要放到响应式里）
const chartRef = ref<any>(null)
let chartIns: any = null
let chartInitPromise: Promise<any> | null = null
const typeChartRef = ref<any>(null)
let typeChartIns: any = null
let typeChartInitPromise: Promise<any> | null = null
let trendRequestId = 0

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
    return chartIns
  }
  if (chartInitPromise) return chartInitPromise
  if (!chartRef.value) {
    console.error('[dashboard] chartRef.value 为空')
    return null
  }
  const echartsModule = loadEcharts()
  if (!echartsModule) {
    console.error('[dashboard] echarts 未加载，无法初始化趋势图')
    return null
  }
  chartInitPromise = (async () => {
    try {
      chartIns = await chartRef.value.init(echartsModule)
      chartRef.value?.resize?.()
      return chartIns
    } catch (e) {
      console.error('[dashboard] 趋势图初始化失败', e)
      return null
    } finally {
      chartInitPromise = null
    }
  })()
  return chartInitPromise
}

const ensureTypeChart = async () => {
  if (typeChartIns) {
    return typeChartIns
  }
  if (typeChartInitPromise) return typeChartInitPromise
  if (!typeChartRef.value) {
    console.error('[dashboard] typeChartRef.value 为空')
    return null
  }
  const echartsModule = loadEcharts()
  if (!echartsModule) {
    console.error('[dashboard] echarts 未加载，无法初始化类型分布图')
    return null
  }
  typeChartInitPromise = (async () => {
    try {
      typeChartIns = await typeChartRef.value.init(echartsModule)
      typeChartRef.value?.resize?.()
      return typeChartIns
    } catch (e) {
      console.error('[dashboard] 类型分布图初始化失败', e)
      return null
    } finally {
      typeChartInitPromise = null
    }
  })()
  return typeChartInitPromise
}

const onTrendChartFinished = () => {
  loadElectricityTrend()
}

const onTypeChartFinished = () => {
  loadAnomalyTypeDistribution()
}

const loadElectricityTrend = async () => {
  try {
    const requestId = ++trendRequestId
    const days = getTrendDays()
    // 请求与 Canvas 初始化同时进行，避免串行等待。
    const statsPromise = request.get('/electricity-data/daily-stats', { days })
    const chartPromise = ensureChart()
    let stats: any[] = []
    
    try {
      stats = await statsPromise
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
    const ins = await chartPromise
    if (!ins) {
      console.error('[dashboard] 图表实例不存在，无法设置 option')
      return
    }
    if (requestId !== trendRequestId) return
    ins.setOption(buildTrendOption(xData, yData), true)
  } catch (error) {
    console.error('获取用电趋势失败:', error)
  }
}

interface DistributionCounts {
  high: number
  medium: number
  low: number
  unclassified: number
  manual: number
  other: number
}

const buildTypeDistributionOption = (counts: DistributionCounts) => {
  const count = (value: unknown) => {
    const number = Number(value ?? 0)
    return Number.isFinite(number) && number > 0 ? number : 0
  }
  const seriesData = [
    { name: '高风险异常', value: count(counts.high), itemStyle: { color: '#f56c6c' } },
    { name: '中风险异常', value: count(counts.medium), itemStyle: { color: '#e6a23c' } },
    { name: '低风险异常', value: count(counts.low), itemStyle: { color: '#67c23a' } },
    { name: '用户主动报警', value: count(counts.manual), itemStyle: { color: '#409eff' } }
  ]
  if (counts.unclassified > 0) {
    seriesData.push({ name: '未分级设备警告', value: count(counts.unclassified), itemStyle: { color: '#909399' } })
  }
  if (counts.other > 0) {
    seriesData.push({ name: '其他异常', value: count(counts.other), itemStyle: { color: '#7d82ee' } })
  }
  const hasData = seriesData.some(item => item.value > 0)

  return {
    tooltip: { trigger: 'item', confine: true, formatter: '{b}：{c} 条（{d}%）' },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'middle',
      data: seriesData.map(item => item.name),
      formatter: (name: string) => `${name} ${seriesData.find(item => item.name === name)?.value ?? 0}起`,
      textStyle: { color: '#606266', fontSize: 10 }
    },
    graphic: hasData ? [] : [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: { text: '暂无异常或报警数据', fill: '#909399', fontSize: 12 }
    }],
    series: [
      {
        name: '异常与报警',
        type: 'pie',
        radius: ['35%', '60%'],
        center: ['28%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: hasData ? seriesData : []
      }
    ]
  }
}

const loadAnomalyTypeDistribution = async () => {
  try {
    const distributionPromise = request.get('/anomalies/type-distribution')
    const chartPromise = ensureTypeChart()
    const distribution = await distributionPromise
    if (!Array.isArray(distribution)) throw new Error('异常类型分布数据格式错误')

    const counts: DistributionCounts = { high: 0, medium: 0, low: 0, unclassified: 0, manual: 0, other: 0 }
    const deviceTypes: Array<{ type: string; total: number }> = []
    for (const item of distribution) {
      const type = String(item?.anomalyType ?? item?.type ?? item?.name ?? '').trim()
      const name = String(item?.name ?? type).trim()
      const value = Number(item?.value ?? 0)
      if (!Number.isFinite(value) || value <= 0) continue
      if (/^(manual|user[_-]?alarm|user[_-]?report)$/i.test(type) ||
          /^(manual|user[_-]?alarm|user[_-]?report)$/i.test(name) || /主动报警|用户报警/.test(name)) {
        counts.manual += value
      } else if (/^device([_-]?(warning|alert|alarm))?$/i.test(type) ||
                 /^device([_-]?(warning|alert|alarm))?$/i.test(name) || /设备.*(警告|告警|报警)/.test(name)) {
        deviceTypes.push({ type, total: value })
      } else {
        counts.other += value
      }
    }

    await Promise.all(deviceTypes.map(async ({ type, total }) => {
      try {
        const records = await request.get(`/anomalies/type/${encodeURIComponent(type)}`)
        if (!Array.isArray(records) || records.length !== total) {
          counts.unclassified += total
          return
        }
        for (const record of records) {
          let level = String(record?.severityLevel ?? '').trim().toLowerCase()
          if (!level && record?.anomalyScore != null) {
            const score = Number(record.anomalyScore)
            if (Number.isFinite(score)) level = score > 0.8 ? 'high' : score > 0.5 ? 'medium' : 'low'
          }
          if (level === 'high' || level === 'critical' || level === '高') counts.high++
          else if (level === 'medium' || level === '中') counts.medium++
          else if (level === 'low' || level === '低') counts.low++
          else counts.unclassified++
        }
      } catch (error) {
        console.warn('[dashboard] 设备警告等级获取失败', error)
        counts.unclassified += total
      }
    }))
    const ins = await chartPromise
    if (!ins) {
      console.error('[dashboard] 异常等级分布图实例不存在，无法设置 option')
      return
    }
    ins.setOption(buildTypeDistributionOption(counts), true)
  } catch (error) {
    console.error('获取异常等级分布失败:', error)
  }
}

// 获取系统状态
const getSystemStatus = async () => {
  try {
    const data: any = await request.get('/dashboard/system-status')
    
    if (data) {
      statusCards[0].value = data.totalUsers?.toLocaleString() || '0'
      
      statusCards[1].value = data.todayAnomalies?.toString() || '0'
      
      statusCards[2].value = `${data.processingRate || 0}%`
      
      statusCards[3].value = `${data.todayElectricity || 0}kWh`
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
  // TabBar 页面不能通过 navigateTo 打开，也不能通过 switchTab 携带查询参数。
  // 先暂存记录 ID，由异常检测页在显示时读取并打开对应详情。
  uni.setStorageSync('pendingAnomalyRecordId', record.id)
  uni.switchTab({
    url: '/pages/anomaly-detection/anomaly-detection'
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
  const userInfo = getUserInfo()
  username.value = userInfo.username || '用户'
  
  getSystemStatus()
  getRecentRecords()
  // 图表在 @finished 后初始化并绘制，这里不抢跑
})

onUnmounted(() => {
  trendRequestId++
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
