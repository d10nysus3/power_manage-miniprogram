<template>
  <view class="page">
    <view class="card">
      <text class="title">审计日志留存</text>
      <text class="hint">日志保存在数据库中，系统每天 02:00 自动清理过期记录，启动时也会执行一次。</text>
      <view class="row">
        <text>留存天数</text>
        <input v-model="retentionDays" type="number" class="input" placeholder="1 至 3650" />
      </view>
      <button type="primary" size="mini" @click="saveRetention">保存设置</button>
    </view>

    <view class="card">
      <text class="title">当前可查询范围</text>
      <text class="detail">最早时间：{{ range.earliestTime || '暂无记录' }}</text>
      <text class="detail">最晚时间：{{ range.latestTime || '暂无记录' }}</text>
      <text class="detail">记录编号：{{ range.count ? `${range.minId} 至 ${range.maxId}` : '暂无记录' }}</text>
      <text class="detail">现存记录：{{ range.count }} 条</text>
    </view>

    <view class="card">
      <text class="title">查询审计日志</text>
      <view class="row"><text>开始日期</text><picker mode="date" @change="setStartDate"><text>{{ startDate || '请选择' }}</text></picker></view>
      <view class="row"><text>结束日期</text><picker mode="date" @change="setEndDate"><text>{{ endDate || '请选择' }}</text></picker></view>
      <view class="row"><text>起始编号</text><input v-model="minId" type="number" class="input" placeholder="不限" /></view>
      <view class="row"><text>结束编号</text><input v-model="maxId" type="number" class="input" placeholder="不限" /></view>
      <view class="actions"><button size="mini" type="primary" @click="search">查询</button><button size="mini" @click="reset">重置</button></view>
      <view v-for="log in logs" :key="log.id" class="log">
        <text class="detail">#{{ log.id }}　{{ log.timestamp }}</text>
        <text class="detail">{{ log.username }}：{{ log.action }}</text>
      </view>
      <text v-if="!logs.length" class="hint">暂无符合条件的记录</text>
      <button v-if="hasMore" size="mini" @click="loadMore">加载更多</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../../utils/request'

interface AuditLog { id: number; timestamp: string; username: string; action: string }
interface AuditRange { count: number; earliestTime: string; latestTime: string; minId: number; maxId: number }

const retentionDays = ref('365')
const range = ref<AuditRange>({ count: 0, earliestTime: '', latestTime: '', minId: 0, maxId: 0 })
const startDate = ref('')
const endDate = ref('')
const minId = ref('')
const maxId = ref('')
const logs = ref<AuditLog[]>([])
const page = ref(0)
const hasMore = ref(false)

const showError = (message: string) => uni.showToast({ title: message, icon: 'none' })

const loadSettings = async () => {
  try {
    const settings = await request.get('/settings') as { key: string; value: string }[]
    retentionDays.value = settings.find(item => item.key === 'audit.retention.days')?.value || '365'
    range.value = await request.get('/audit-logs/range') as AuditRange
  } catch (error) { showError('获取日志设置失败') }
}

const saveRetention = async () => {
  const days = Number(retentionDays.value)
  if (!Number.isInteger(days) || days < 1 || days > 3650) { showError('请输入 1 至 3650 的整数'); return }
  try {
    const result = await request.post('/settings', { key: 'audit.retention.days', value: String(days) }) as { success: boolean; message?: string }
    if (!result.success) { showError(result.message || '保存失败'); return }
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (error) { showError('保存失败') }
}

const loadLogs = async (append = false) => {
  if (startDate.value && endDate.value && startDate.value > endDate.value) { showError('开始日期不能晚于结束日期'); return }
  if (minId.value && maxId.value && Number(minId.value) > Number(maxId.value)) { showError('起始编号不能大于结束编号'); return }
  try {
    const result = await request.get('/audit-logs', {
      startDate: startDate.value || undefined, endDate: endDate.value || undefined,
      minId: minId.value || undefined, maxId: maxId.value || undefined,
      page: page.value, size: 20
    }) as { content: AuditLog[]; last: boolean }
    logs.value = append ? [...logs.value, ...result.content] : result.content
    hasMore.value = !result.last
    range.value = await request.get('/audit-logs/range') as AuditRange
  } catch (error) { showError('查询日志失败') }
}

const search = () => { page.value = 0; loadLogs() }
const loadMore = () => { page.value += 1; loadLogs(true) }
const reset = () => { startDate.value = ''; endDate.value = ''; minId.value = ''; maxId.value = ''; search() }
const setStartDate = (event: any) => { startDate.value = event.detail.value }
const setEndDate = (event: any) => { endDate.value = event.detail.value }
onMounted(() => { loadSettings(); loadLogs() })
</script>

<style scoped>
.page { padding: 24rpx; background: #f5f7fa; min-height: 100vh; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 24rpx; }
.title { display: block; font-size: 34rpx; font-weight: 600; margin-bottom: 20rpx; }
.hint { display: block; color: #777; font-size: 24rpx; margin: 16rpx 0; }
.detail { display: block; font-size: 26rpx; margin: 12rpx 0; }
.row { display: flex; justify-content: space-between; align-items: center; padding: 18rpx 0; border-bottom: 1rpx solid #eee; font-size: 28rpx; }
.input { width: 220rpx; text-align: right; }
.actions { display: flex; margin: 20rpx 0; }
.log { border-top: 1rpx solid #eee; padding: 16rpx 0; }
</style>
