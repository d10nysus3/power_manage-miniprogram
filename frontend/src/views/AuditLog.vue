<template>
  <div class="audit-log">
    <h2>审计日志</h2>
    <el-alert type="info" :closable="false" show-icon>
      日志写入数据库，按系统设置中的留存天数每天 02:00 自动清理。日期和编号范围以当前数据库中的记录为准。
    </el-alert>

    <el-descriptions title="当前可查询范围" :column="2" border class="range">
      <el-descriptions-item label="最早记录时间">{{ range.earliestTime || '暂无记录' }}</el-descriptions-item>
      <el-descriptions-item label="最晚记录时间">{{ range.latestTime || '暂无记录' }}</el-descriptions-item>
      <el-descriptions-item label="最小记录编号">{{ range.minId || '暂无记录' }}</el-descriptions-item>
      <el-descriptions-item label="最大记录编号">{{ range.maxId || '暂无记录' }}</el-descriptions-item>
      <el-descriptions-item label="现存记录数">{{ range.count }}</el-descriptions-item>
    </el-descriptions>

    <div class="filters">
      <el-date-picker v-model="dates" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期"
        format="YYYY-MM-DD" value-format="YYYY-MM-DD" clearable />
      <el-input-number v-model="minId" :min="1" :precision="0" placeholder="起始编号" controls-position="right" />
      <el-input-number v-model="maxId" :min="1" :precision="0" placeholder="结束编号" controls-position="right" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" row-key="id" style="width: 100%">
      <el-table-column prop="id" label="记录编号" width="110" />
      <el-table-column prop="timestamp" label="时间" width="190" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="action" label="动作" />
    </el-table>
    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
      @current-change="load" @size-change="search" class="pagination" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '../utils/api'

interface AuditLog { id: number; timestamp: string; username: string; action: string }
interface AuditRange { count: number; earliestTime: string; latestTime: string; minId: number; maxId: number }

const rows = ref<AuditLog[]>([])
const range = ref<AuditRange>({ count: 0, earliestTime: '', latestTime: '', minId: 0, maxId: 0 })
const dates = ref<[string, string] | null>(null)
const minId = ref<number | undefined>(undefined)
const maxId = ref<number | undefined>(undefined)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

const load = async () => {
  if (minId.value && maxId.value && minId.value > maxId.value) {
    ElMessage.warning('起始编号不能大于结束编号')
    return
  }
  loading.value = true
  try {
    const result = await apiClient.get('/audit-logs', { params: {
      startDate: dates.value?.[0], endDate: dates.value?.[1],
      minId: minId.value, maxId: maxId.value, page: page.value - 1, size: pageSize.value
    } }) as { content: AuditLog[]; totalElements: number }
    rows.value = result.content
    total.value = result.totalElements
    range.value = await apiClient.get('/audit-logs/range') as AuditRange
  } catch (error) {
    ElMessage.error('获取审计日志失败')
  } finally {
    loading.value = false
  }
}

const search = () => { page.value = 1; load() }
const reset = () => { dates.value = null; minId.value = undefined; maxId.value = undefined; search() }
onMounted(load)
</script>

<style scoped>
.audit-log { padding: 20px; }
.range { margin: 20px 0; }
.filters { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 18px; justify-content: flex-end; }
</style>
