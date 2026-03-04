<template>
  <div class="container">
    <h2>异常检测</h2>
    
    <div class="status-alert">
      <el-badge :value="15" type="warning" class="status-badge">
        <el-button type="text" class="status-button">实时监控中...</el-button>
      </el-badge>
    </div>

    <div class="detection-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="实时监控" name="realtime" />
        <el-tab-pane label="异常警报" name="alerts" />
        <el-tab-pane label="异常记录" name="records" />
        <el-tab-pane label="处理跟踪" name="tracking" />
      </el-tabs>
    </div>

    <div class="search-filters">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入用户ID或用户名"
        class="search-input"
        :prefix-icon="Search"
      />
      <el-select v-model="selectedUserId" placeholder="选择用户" class="filter-select">
        <el-option label="全部用户" value="" />
        <el-option v-for="user in userList" :key="user.id" :label="user.name" :value="user.id.toString()" />
      </el-select>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-picker"
      />
      <el-select v-model="anomalyTypeFilter" placeholder="异常类型" class="filter-select">
        <el-option label="全部" value="" />
        <el-option 
          v-for="option in anomalyTypeOptions" 
          :key="option.value" 
          :label="option.label" 
          :value="option.value" 
        />
      </el-select>
      <el-select v-model="riskLevelFilter" placeholder="严重程度" class="filter-select">
        <el-option label="全部" value="" />
        <el-option label="高风险" value="high" />
        <el-option label="中风险" value="medium" />
        <el-option label="低风险" value="low" />
      </el-select>
      <el-select v-model="processStatusFilter" placeholder="处理状态" class="filter-select">
        <el-option label="全部" value="" />
        <el-option label="未处理" value="pending" />
        <el-option label="已处理" value="completed" />
      </el-select>
      <el-button type="primary" @click="loadAnomalyData(true)">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    
    <div v-if="activeTab === 'realtime'" class="realtime-content">
      <div class="alert-dashboard">
        <h3>异常用电监控面板</h3>
        <el-switch v-model="autoRefresh" class="auto-refresh-switch" />
        <span class="auto-refresh-text">自动刷新</span>
        <el-button type="text" class="refresh-btn" @click="refreshData">刷新数据</el-button>
      </div>

      <div class="alert-stats">
        <el-card class="stat-card danger" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ alertStats.highRisk }}</div>
            <div class="stat-label">高风险告警</div>
            <div class="stat-icon">
              <el-icon size="32" color="#f56c6c"><Warning /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card warning" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ alertStats.mediumRisk }}</div>
            <div class="stat-label">中风险告警</div>
            <div class="stat-icon">
              <el-icon size="32" color="#e6a23c"><WarningFilled /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card info" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ alertStats.lowRisk }}</div>
            <div class="stat-label">低风险告警</div>
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><InfoFilled /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card success" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ alertStats.processedToday }}</div>
            <div class="stat-label">今日已处理</div>
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><Check /></el-icon>
            </div>
          </div>
        </el-card>
      </div>

      <div class="realtime-monitoring">
        <el-card class="monitoring-card">
          <template #header>
            <div class="card-header">
              <span>实时用电监控</span>
            </div>
          </template>
          <div ref="chartRef" class="chart" style="height: 300px;" />
        </el-card>
      </div>
    </div>

    <!-- 异常警报标签内容 -->
    <div v-if="activeTab === 'alerts'" class="alerts-content">
      <el-table :data="anomalyList" style="width: 100%" v-loading="loading">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="警报ID" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="userName" label="用户名" width="120" />
        <el-table-column prop="alertType" label="异常类型" width="120" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.alertType || '未知' }}
            </template>
          </el-table-column>
        <el-table-column prop="severityLevel" label="严重程度" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.severityLevel === 'high' ? 'danger' : (scope.row.severityLevel === 'medium' ? 'warning' : 'info')"
            >
              {{ scope.row.severityLevel === 'high' ? '高风险' : (scope.row.severityLevel === 'medium' ? '中风险' : '低风险') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectionTime" label="发生时间" width="160" />
        <el-table-column prop="processTime" label="处理时间" width="160" />
        <el-table-column prop="processor" label="处理人" width="120" />
        <el-table-column prop="isProcessed" label="处理状态" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.isProcessed ? 'success' : 'info'"
            >
              {{ scope.row.isProcessed ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isFalseAlarm" label="误报状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isFalseAlarm ? 'danger' : 'info'">{{ scope.row.isFalseAlarm ? '误报' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="viewAlertDetails(scope.row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="processAlert(scope.row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 异常记录标签内容 -->
    <div v-if="activeTab === 'records'" class="records-content">
      <el-table :data="anomalyList" style="width: 100%" v-loading="loading">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="记录ID" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="userName" label="用户名" width="120" />
        <el-table-column prop="alertType" label="异常类型" width="120" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.alertType || '未知' }}
            </template>
          </el-table-column>
        <el-table-column prop="severityLevel" label="严重程度" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.severityLevel === 'high' ? 'danger' : (scope.row.severityLevel === 'medium' ? 'warning' : 'info')"
            >
              {{ scope.row.severityLevel === 'high' ? '高风险' : (scope.row.severityLevel === 'medium' ? '中风险' : '低风险') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectionTime" label="发生时间" width="160" />
        <el-table-column prop="processTime" label="处理时间" width="160" />
        <el-table-column prop="processor" label="处理人" width="120" />
        <el-table-column prop="description" label="异常描述" />
        <el-table-column prop="isProcessed" label="处理状态" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.isProcessed ? 'success' : 'info'"
            >
              {{ scope.row.isProcessed ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="viewAlertDetails(scope.row)">查看详情</el-button>
            <el-button link v-if="!scope.row.isProcessed" type="primary" size="small" @click="processAlert(scope.row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 处理跟踪标签内容 -->
    <div v-if="activeTab === 'tracking'" class="tracking-content">
      <el-table :data="anomalyList.filter(item => item.isProcessed)" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="userName" label="用户名" width="120" />
        <el-table-column prop="alertType" label="异常类型" width="120" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.alertType || '未知' }}
            </template>
          </el-table-column>
        <el-table-column prop="detectionTime" label="发生时间" width="160" />
        <el-table-column prop="processTime" label="处理时间" width="160" />
        <el-table-column prop="processor" label="处理人" width="120" />
        <el-table-column prop="description" label="异常描述" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="viewAlertDetails(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalAnomalies"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 处理异常弹窗 -->
    <el-dialog
      v-model="processDialogVisible"
      title="处理异常警报"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="process-form">
        <el-form :model="processForm" label-width="100px">
          <el-form-item label="异常类型">
            <el-input v-model="processForm.alertType" disabled></el-input>
          </el-form-item>
          <el-form-item label="异常内容">
            <el-input v-model="processForm.alertContent" type="textarea" rows="3" disabled></el-input>
          </el-form-item>
          <el-form-item label="处理结果">
            <el-radio-group v-model="processForm.processType">
              <el-radio label="normal">正常处理</el-radio>
              <el-radio label="falseAlarm">警报有误</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="processForm.processType === 'normal'" label="处理说明">
            <el-input v-model="processForm.processNote" type="textarea" rows="3" placeholder="请输入处理说明"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="processDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmProcess">确认处理</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { ElMessage, ElMessageBox } from "element-plus";
import * as echarts from "echarts";
import apiClient from '@/utils/api';
import type { AnomalyData, UserOption } from '@/types';
// Prefer the browser build to avoid Node-like globals in lib build
import SockJS from 'sockjs-client/dist/sockjs';
import { Stomp } from '@stomp/stompjs';
import { InfoFilled, Search, Warning, WarningFilled, Check } from '@element-plus/icons-vue';
import { ElSelect, ElOption, ElButton, ElInput, ElTable, ElTableColumn, ElPagination, ElBadge } from 'element-plus';
import { AnomalyType, AnomalyTypeLabels, getAnomalyTypeColor, getAnomalyTypeText } from '../enums/AnomalyType';

interface AnomalyData {
  id: string | number; 
  userId?: number; 
  userName?: string; 
  user?: string; 
  alertType?: string; 
  anomalyType: string; 
  alertTime?: string; 
  detectionTime?: string; 
  timestamp?: string; 
  alertContent?: string; 
  description: string; 
  isProcessed: boolean; 
  isFalseAlarm: boolean;
  severityLevel: string; 
  processTime?: string; 
  processor?: string; 
}

const chartRef = ref<HTMLDivElement>();
const chartInstance = ref<echarts.ECharts | null>(null);
const isUnmounted = ref(false); 

const anomalyList = ref<AnomalyData[]>([]);
const totalAnomalies = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const searchKeyword = ref('');
const anomalyTypeFilter = ref('');
const timeRangeFilter = ref('');
const riskLevelFilter = ref('');
const processStatusFilter = ref(''); 
const activeTab = ref('realtime'); 
const autoRefresh = ref(true); 
const statsLoading = ref(false); 
const selectedDateRange = ref<[Date, Date] | null>(null);
const alertStats = ref({ total: 0, highRisk: 0, mediumRisk: 0, lowRisk: 0, processedToday: 0 });

const settings = ref<Map<string, string>>(new Map());
const isLoadingSettings = ref(false);
const alertThresholdHigh = ref(90); 

const userList = ref([]);

const anomalyTypeOptions = ref([]);

const selectedUserId = ref('');
const selectedDate = ref('');

const getAnomalyTypeOptions = async () => {
  try {
    // 后端提供的异常类型分布接口
    const response = await apiClient.get('anomalies/type-distribution') as any[];
    if (response && Array.isArray(response)) {
      // 兼容多种字段：name、typeName、anomalyType、type
      anomalyTypeOptions.value = response.map((item: any) => {
        const value = item.name || item.typeName || item.anomalyType || item.type;
        const label = item.name || item.typeName || item.anomalyType || item.type;
        return { value, label };
      });
    } else {
      console.warn('异常类型选项数据格式不正确:', response);
      anomalyTypeOptions.value = [
        { value: '瞬时断电', label: '瞬时断电' },
        { value: '功率突增', label: '功率突增' },
        { value: '功率突降', label: '功率突降' }
      ];
    }
  } catch (error) {
    console.error('获取异常类型选项失败:', error);
    ElMessage.error('获取异常类型选项失败');
    anomalyTypeOptions.value = [
      { value: '瞬时断电', label: '瞬时断电' },
      { value: '功率突增', label: '功率突增' },
      { value: '功率突降', label: '功率突降' }
    ];
  }
};

const loadUserList = async () => {
  try {
    const response = await apiClient.get('users', { params: { pageSize: 1000 } }) as any;
    // 兼容后端分页返回 { records, total, ... } 和直接数组两种情况
    const records = Array.isArray(response) ? response : response?.records;
    if (records && Array.isArray(records)) {
      userList.value = records;
    } else {
      console.warn('用户列表数据格式不正确:', response);
      userList.value = [];
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    ElMessage.error('获取用户列表失败');
    userList.value = [];
  }
};

const refreshData = () => {
  if (activeTab.value === 'realtime') {
    updateChart();
    getAnomalyStats();
  } else {
    loadAnomalyRecords();
  }
};

const startAutoRefresh = () => {
  if (autoRefresh.value && !refreshInterval) {
    refreshInterval = setInterval(refreshData, 5000); 
  }
};

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
};

watch(autoRefresh, (newValue) => {
  if (newValue) {
    startAutoRefresh();
  } else {
    stopAutoRefresh();
  }
});

const processDialogVisible = ref(false);
const processForm = reactive({
  id: 0,
  alertType: '',
  alertContent: '',
  processType: 'normal',
  processNote: ''
});

interface RealtimeData {
  currentUsage: number;
  status: string;
  timestamp: string;
  message?: string;
}

const realtimeData = ref<RealtimeData>({ currentUsage: 0, status: 'normal', timestamp: '' });

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadAnomalyData();
};

const handleCurrentChange = (current: number) => {
  currentPage.value = current;
  loadAnomalyData();
};

const handleTabChange = async (tabName: string) => {
  activeTab.value = tabName;
  currentPage.value = 1; 
  
  if (tabName === 'realtime') {
    // 切换回实时监控标签页时，DOM被重新创建，需要等待渲染并重新初始化图表
    await nextTick();
    // 重新初始化图表（initChart内部会调用updateChart加载数据）
    initChart();
    // 获取统计数据
    getAnomalyStats();
  } else {
    loadAnomalyData();
  }
};

const loadAnomalyData = (showTips: boolean = false) => {
  const tab = activeTab.value;
  if (tab === 'realtime') {
    updateChart();
    getAnomalyStats();
  } else {
    loadAnomalyRecords(showTips);
  }
};

const resetFilters = () => {
  searchKeyword.value = '';
  selectedUserId.value = '';
  selectedDate.value = new Date().toISOString().slice(0, 10);
  anomalyTypeFilter.value = '';
  riskLevelFilter.value = '';
  processStatusFilter.value = '';
  loadAnomalyData();
};

const getFilterParams = () => {
  const params: any = {};
  if (searchKeyword.value) {
    params.userInfo = searchKeyword.value;
  }
  if (selectedUserId.value) {
    params.userId = selectedUserId.value;
  }
  if (selectedDate.value) {
    const date = new Date(selectedDate.value);
    params.startDate = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0).toISOString().slice(0, 19).replace('T', ' ');
    params.endDate = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59).toISOString().slice(0, 19).replace('T', ' ');
  }
  if (anomalyTypeFilter.value) {
    // 与后端 AnomalyRecordsController 的参数一致
    params.anomalyType = anomalyTypeFilter.value;
  }
  if (riskLevelFilter.value) {
    params.severityLevel = riskLevelFilter.value;
  }
  return params;
};

const loadAnomalyRecords = async (showTips: boolean = false) => {
  loading.value = true;
  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...getFilterParams(),
    };

    const tab = activeTab.value;
    if (tab === 'alerts') {
      params.processStatus = 'unprocessed';
    } else if (tab === 'tracking') {
      params.processStatus = 'processed';
    }

    const response = await apiClient.get('anomaly-records/search', { params });

    const records = Array.isArray(response?.records) ? response.records : [];
    anomalyList.value = records;
    totalAnomalies.value = Number(response?.total ?? records.length ?? 0);

    if (records.length === 0 && showTips) {
      const hasActiveFilters = Boolean(
        searchKeyword.value ||
        selectedUserId.value ||
        selectedDate.value ||
        anomalyTypeFilter.value ||
        riskLevelFilter.value ||
        processStatusFilter.value
      );

      if (hasActiveFilters) {
        ElMessage.info('没有符合当前筛选条件的数据');
      } else {
        ElMessage.info('暂无异常记录');
      }
    }
  } catch (error: any) {
    console.error(`获取异常数据失败 (tab: ${activeTab.value}):`, {
      message: error?.message,
      status: error?.response?.status,
      data: error?.response?.data
    });
    if (showTips) {
      ElMessage.error('获取异常数据失败：通信异常或服务器错误');
    }
    anomalyList.value = [];
    totalAnomalies.value = 0;
  } finally {
    loading.value = false;
  }
};

const getAnomalyStats = async () => {
  statsLoading.value = true;
  try {
    const response = await apiClient.get('alert-stats/stats') as any;
    
    alertStats.value = {
      total: response?.total ?? 0,
      highRisk: response?.high ?? response?.highRisk ?? 0,
      mediumRisk: response?.medium ?? response?.mediumRisk ?? 0,
      lowRisk: response?.low ?? response?.lowRisk ?? 0,
      processedToday: response?.processedToday ?? 0
    };
  } catch (error) {
    console.error('获取异常统计数据失败:', error);
    ElMessage.error('获取异常统计数据失败');
    alertStats.value = { total: 0, highRisk: 0, mediumRisk: 0, lowRisk: 0, processedToday: 0 };
  } finally {
    statsLoading.value = false;
  }
};

const getRealtimeMonitoringData = async () => {
  try {
    // 修正时区问题：使用本地时间
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60 * 1000;
    const localNow = new Date(now.getTime() - offset);
    
    // 设置为当天 00:00:00
    const todayStart = new Date(localNow.getFullYear(), localNow.getMonth(), localNow.getDate(), 0, 0, 0);
    // 设置为当天 23:59:59
    const todayEnd = new Date(localNow.getFullYear(), localNow.getMonth(), localNow.getDate(), 23, 59, 59);

    // 根据是否选择了用户切换接口
    let endpoint = 'electricity-data/time-range';
    if (selectedUserId.value) {
      endpoint = `electricity-data/user/${selectedUserId.value}/time-range`;
    }

    const response = await apiClient.get(endpoint, { 
      params: {
        startTime: todayStart.toISOString().slice(0, 19).replace('T', ' '),
        endTime: todayEnd.toISOString().slice(0, 19).replace('T', ' ')
      }
    }) as any;
    
    
    if (response && Array.isArray(response) && response.length > 0) {
      
      const latestData = response[response.length - 1];
      realtimeData.value = {
        currentUsage: latestData.electricityConsumption || 0,
        status: latestData.electricityConsumption > 90 ? 'warning' : 'normal',
        timestamp: latestData.recordTime || new Date().toLocaleString('zh-CN'),
        message: latestData.electricityConsumption > 90 ? '当前用电量偏高' : '用电量正常'
      };
    } else {
      
      realtimeData.value = {
        currentUsage: 0,
        status: 'normal',
        timestamp: new Date().toLocaleString('zh-CN'),
        message: '暂无数据'
      };
    }
  } catch (error) {
    console.error('获取实时监控数据失败:', error);
    ElMessage.error('获取实时监控数据失败');
    
    
    realtimeData.value = {
      currentUsage: 0,
      status: 'normal',
      timestamp: new Date().toLocaleString('zh-CN'),
      message: '数据获取失败'
    };
  }
};

const viewAlertDetails = (record: AnomalyData) => {
  console.log('查看警报详情:', record);
  
  
  if (!record || !record.id) {
    ElMessage.error('记录数据异常，无法查看详情');
    return;
  }
  
  try {
    
    const detailsHtml = `
      <div style="text-align: left;">
        <p><strong>警报ID:</strong> ${record.id}</p>
        <p><strong>用户:</strong> ${record.user || '未知用户'}</p>
        <p><strong>异常类型:</strong> ${record.anomalyType}</p>
        <p><strong>严重程度:</strong> ${record.severityLevel === 'high' ? '高风险' : record.severityLevel === 'medium' ? '中风险' : '低风险'}</p>
        <p><strong>警报时间:</strong> ${record.detectionTime || '未知'}</p>
        <p><strong>异常说明:</strong> ${record.description}</p>
        <p><strong>处理状态:</strong> ${record.isProcessed ? '已处理' : '待处理'}</p>
        <p><strong>处理时间:</strong> ${record.processTime || '未处理'}</p>
        <p><strong>处理人:</strong> ${record.processor || '未指定'}</p>
        <p><strong>误报状态:</strong> ${record.isFalseAlarm ? '误报' : '正常'}</p>
      </div>
    `;
    
    ElMessageBox.alert(detailsHtml, `警报详情 - ID: ${record.id}`, {
      confirmButtonText: '关闭',
      dangerouslyUseHTMLString: true,
      customStyle: {
        width: '500px'
      }
    });
  } catch (error) {
    console.error('显示详情失败:', error);
    ElMessage.error('显示详情失败');
  }
};

const processAlert = (record: AnomalyData) => {
  console.log('处理警报:', record);
  
  
  if (!record || !record.id) {
    ElMessage.error('记录数据异常，无法处理');
    return;
  }
  
  
  if (record.isProcessed) {
    ElMessage.warning('此警报已经处理过了');
    return;
  }
  
  
  processForm.id = Number(record.id);
  processForm.alertType = record.anomalyType;
  processForm.alertContent = record.description;
  processForm.processType = 'normal';
  processForm.processNote = '';
  
  
  processDialogVisible.value = true;
};

const confirmProcess = async () => {
  try {
    if (processForm.processType === 'falseAlarm') {
      const response = await apiClient.post(`anomaly-records/${processForm.id}/false-alarm`, {});
      if (response.success) {
        ElMessage.success('已标记为误报');
      } else {
        throw new Error(response.message || '标记误报失败');
      }
    } else {
      await apiClient.post(`anomaly-records/process/${processForm.id}`, {
        processNote: processForm.processNote
      });
      ElMessage.success('异常处理成功');
    }
    processDialogVisible.value = false;
    await loadAnomalyData();
  } catch (error: any) {
    console.error('处理异常失败:', error);
    ElMessage.error(`处理异常失败: ${error.message || '未知错误'}`);
  }
};

const loadSettings = async () => {
  try {
    isLoadingSettings.value = true;
    const response = await apiClient.get("settings") as any;
    
    
    settings.value.clear();
    if (Array.isArray(response)) {
      response.forEach(setting => {
        if (setting.key && setting.value) {
          settings.value.set(setting.key, setting.value);
        }
      });
    }
    
    
    const highThreshold = settings.value.get('alert.threshold.high');
    if (highThreshold && !isNaN(Number(highThreshold))) {
      alertThresholdHigh.value = Number(highThreshold);
      console.log('已从API加载预警阈值:', alertThresholdHigh.value);
    } else {
      console.warn('未找到有效的预警阈值配置，使用默认值:', alertThresholdHigh.value);
    }
  } catch (error) {
    console.error('加载系统配置失败:', error);
    ElMessage.error('加载系统配置失败，使用默认阈值');
  } finally {
    isLoadingSettings.value = false;
  }
};

const generateTimeSlots = () => {
  const slots = [];
  for (let i = 0; i < 24; i++) {
    for (let j = 0; j < 60; j += 15) {
      slots.push(`${i.toString().padStart(2, '0')}:${j.toString().padStart(2, '0')}`);
    }
  }
  return slots;
};

const initChart = async () => {
  
  if (isUnmounted.value) return; 
  
  
  if (activeTab.value !== 'realtime') {
    console.log('当前不在实时监控标签页，跳过图表初始化');
    return;
  }
  
  
  await new Promise(resolve => setTimeout(resolve, 300));
  
  
  if (isUnmounted.value || activeTab.value !== 'realtime') {
    console.log('组件已卸载或标签页已切换，跳过图表初始化');
    return;
  }
  
  
  if (!chartRef.value) {
    console.error('图表容器元素不存在');
    return;
  }
  
  try {
    
    await loadSettings();
    
    
    if (chartInstance.value) {
      chartInstance.value.dispose();
      chartInstance.value = null;
    }
    
    console.log('初始化图表，容器尺寸:', chartRef.value.offsetWidth, 'x', chartRef.value.offsetHeight);
    
    
    chartInstance.value = echarts.init(chartRef.value);
    
    const timeSlots = generateTimeSlots();
    
    const emptyOption = {
      
      title: {
        text: '',
        show: false
      },
      tooltip: {
        trigger: 'axis',
        confine: true,
        formatter: (params: any) => {
          if (!params || params.length === 0) return '';
          
          const time = params[0].name;
          const usageData = params.find((p: any) => p.seriesName === '实际用电');
          const thresholdData = params.find((p: any) => p.seriesName === '预警阈值');
          
          // 获取实际值，如果为null或undefined则表示无数据
          const usageValue = usageData ? usageData.value : null;
          const hasData = usageValue !== null && usageValue !== undefined;
          
          // initChart 中无法访问 dynamicThreshold 和 anomalyTimePoints，使用默认值
          const threshold = thresholdData?.value || alertThresholdHigh.value;
          const isAnomaly = false;
          
          let usageHtml = '';
          if (hasData) {
            usageHtml = `实际用电: <span style="font-weight: bold; margin-left: 5px; color: ${isAnomaly ? '#f56c6c' : '#67c23a'};">${Number(usageValue).toFixed(2)} kWh</span>`;
          } else {
            usageHtml = `实际用电: <span style="font-weight: bold; margin-left: 5px; color: #909399;">暂无数据</span>`;
          }
          
          return `
            <div style="font-weight: bold; margin-bottom: 8px;">时间: ${time}</div>
            <div style="display: flex; align-items: center; margin-bottom: 5px;">
              <span style="display: inline-block; width: 10px; height: 10px; background-color: ${usageData?.color || '#409eff'}; border-radius: 50%; margin-right: 8px;"></span>
              ${usageHtml}
            </div>
            <div style="display: flex; align-items: center; margin-bottom: 5px;">
              <span style="display: inline-block; width: 10px; height: 10px; background-color: ${thresholdData?.color || '#f56c6c'}; border-radius: 50%; margin-right: 8px;"></span>
              预警阈值: <span style="font-weight: bold; margin-left: 5px;">${threshold.toFixed(2)} kWh</span>
            </div>
          `;
        }
      },
      legend: {
        data: ['实际用电', '预警阈值'],
        top: 10
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: timeSlots,
        axisLabel: {
          interval: 3, // 96个点，每隔3个点（即每4个点）显示一个标签，即每小时显示一个
          formatter: (value: string) => {
            // 只显示整点时间，如 00:00, 01:00
            return value.endsWith('00') ? value : '';
          }
        }
      },
      yAxis: {
        type: 'value',
        name: '用电量(kWh)',
        min: 0
        
      },
      series: [
        {
          name: '实际用电',
          type: 'line',
          data: Array(timeSlots.length).fill(0),
          smooth: true,
          symbolSize: 6,
          lineStyle: {
            width: 2,
            color: '#409eff'
          }
        }
      ]
    };
    chartInstance.value.setOption(emptyOption);
    
    
    setTimeout(() => {
      if (!isUnmounted.value && chartInstance.value && activeTab.value === 'realtime') {
        updateChart();
      }
    }, 100);
    
    
    handleResize = () => {
      if (chartInstance.value && !isUnmounted.value && activeTab.value === 'realtime') {
        chartInstance.value.resize();
      }
    };
    
    window.addEventListener('resize', handleResize);
  } catch (error) {
    console.error('初始化图表失败:', error);
    ElMessage.error('图表初始化失败，请刷新页面重试');
  }
};

const updateChart = async () => {
  
  if (isUnmounted.value || !chartInstance.value || activeTab.value !== 'realtime') {
    console.warn('组件已卸载、图表实例不存在或不在实时监控标签页，跳过图表更新');
    return;
  }
  
  try {
    
    chartInstance.value.showLoading('default', {
      text: '正在获取用电数据...',
      color: '#3b82f6',
      textColor: '#000',
      maskColor: 'rgba(255, 255, 255, 0.8)',
      zlevel: 0
    });

    
    await loadSettings();

    
    const timeSlots = generateTimeSlots();
    let usageData: any[] = [];
    let thresholdData: number[] = [];
    
    
    let startTime: string;
    let endTime: string;
    
    const today = new Date();
    const date = selectedDate.value ? new Date(selectedDate.value) : today;

    // 修正时区问题：直接构造本地时间字符串
    const y = date.getFullYear();
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    
    startTime = `${y}-${m}-${d} 00:00:00`;
    endTime = `${y}-${m}-${d} 23:59:59`;
    
    
    const params: any = {
      ...getFilterParams(), 
      startTime,
      endTime
    };
    
    // 如果选择了用户，使用特定用户的接口
    let endpoint = 'electricity-data/time-range';
    if (selectedUserId.value) {
      endpoint = `electricity-data/user/${selectedUserId.value}/time-range`;
      // 特定用户接口不需要在params里再传userId（虽然传了也没事，但保持整洁）
      delete params.userId;
    }
    
    console.log('发送API请求:', endpoint, params);
    
    // 并行获取用电数据和异常记录
    const [response, anomalyResponse] = await Promise.all([
      apiClient.get(endpoint, { params }) as any,
      apiClient.get('anomaly-records/search', { 
        params: {
          startDate: startTime,
          endDate: endTime,
          page: 1,
          pageSize: 1000,
          userInfo: selectedUserId.value || undefined
        }
      }) as any
    ]);
    
    console.log('API响应状态:', response ? '成功' : '失败');
    console.log('用电数据:', response);
    console.log('异常记录:', anomalyResponse);
    
    // 处理异常记录，提取发生异常的时间点
    const anomalyTimePoints = new Set<string>();
    if (anomalyResponse && Array.isArray(anomalyResponse.records)) {
      anomalyResponse.records.forEach((record: any) => {
        if (record.detectionTime) {
          const dt = new Date(record.detectionTime);
          const h = dt.getHours().toString().padStart(2, '0');
          // 将异常时间也归整到最近的15分钟刻度
          const m = Math.floor(dt.getMinutes() / 15) * 15;
          const mStr = m.toString().padStart(2, '0');
          const timeKey = `${h}:${mStr}`;
          anomalyTimePoints.add(timeKey);
        }
      });
    }

    const dynamicThreshold = alertThresholdHigh.value;
    // 初始化为对象数组，确保数据格式一致性
    usageData = timeSlots.map(() => ({
      value: null,
      symbol: 'none', // 无数据时不显示点
      itemStyle: { color: '#409eff' }
    }));
    thresholdData = timeSlots.map(() => dynamicThreshold); 
    
    
    if (response && Array.isArray(response) && response.length > 0) {
      
      response.forEach((data: any) => {
        if (data.recordTime && data.electricityConsumption !== undefined) {
          const dt = new Date(data.recordTime);
          const h = dt.getHours().toString().padStart(2, '0');
          // 数据映射：将分钟数归整到最近的15分钟刻度
          const m = Math.floor(dt.getMinutes() / 15) * 15;
          const mStr = m.toString().padStart(2, '0');
          const timeKey = `${h}:${mStr}`;
          const index = timeSlots.indexOf(timeKey);
          
          if (index >= 0) {
            const value = Number(data.electricityConsumption) || 0;
            const isAnomaly = anomalyTimePoints.has(timeKey);
            
            // 填充有数据的点
            usageData[index] = {
              value: value,
              symbol: 'circle',
              symbolSize: isAnomaly ? 8 : 6,
              itemStyle: {
                color: isAnomaly ? '#f56c6c' : '#409eff',
                borderColor: isAnomaly ? '#f56c6c' : '#409eff',
                borderWidth: 1
              }
            };
          }
        }
      });
      console.log('使用真实用电数据:', { dataCount: response.length });
    } else {
      console.warn('未获取到真实用电数据，显示基础数据');
      
      usageData = timeSlots.map(() => ({ value: null, symbol: 'none' }));
      
      
      ElMessage.info('当前时间段暂无用电数据，显示基础图表');
    }
    
    
    
    // 计算最大值时需要从对象中提取value，或者处理null
    const validUsageValues = usageData.map(item => item && item.value !== null ? item.value : 0);
    const maxUsage = Math.max(...validUsageValues, 0);
    const yAxisMax = maxUsage + 20; 
    
    
    const option = {
      title: {
        show: false
      },
      tooltip: {
        show: true,
        trigger: 'axis',
        confine: true,
        backgroundColor: 'rgba(50, 50, 50, 0.9)',
        borderColor: '#409eff',
        borderWidth: 1,
        textStyle: {
          color: '#fff',
          fontSize: 14
        },
        formatter: (params: any) => {
          if (!params || !Array.isArray(params) || params.length === 0) return '';
          
          const time = params[0].name; // 获取时间（x轴类别）
          
          // 查找相关系列的数据
          let usageItem = params.find((p: any) => p.seriesName === '实际用电');
          
          // 解析实际用电量
          let usageValue: number | null = null;
          if (usageItem) {
             // 如果 data 是对象 {value: 123, ...}，ECharts 通常会自动提取 value 到 usageItem.value
             // 但为了保险，我们检查 data 对象
             if (usageItem.data && typeof usageItem.data === 'object' && usageItem.data.value !== null && usageItem.data.value !== undefined) {
               usageValue = usageItem.data.value;
             } else if (usageItem.value !== null && usageItem.value !== undefined) {
               usageValue = Array.isArray(usageItem.value) ? usageItem.value[1] : usageItem.value;
             }
          }
          
          const hasData = usageValue !== null;
          
          const isAnomaly = anomalyTimePoints.has(time);
          
          let usageHtml = '';
          if (hasData) {
            const color = isAnomaly ? '#f56c6c' : '#67c23a';
            usageHtml = `实际用电: <span style="font-weight: bold; margin-left: 5px; color: ${color};">${Number(usageValue).toFixed(2)} kWh</span>`;
          } else {
            usageHtml = `实际用电: <span style="font-weight: bold; margin-left: 5px; color: #909399;">暂无数据</span>`;
          }
          
          return `
            <div style="font-weight: bold; margin-bottom: 8px;">时间: ${time}</div>
            <div style="display: flex; align-items: center; margin-bottom: 5px;">
              <span style="display: inline-block; width: 10px; height: 10px; background-color: ${usageItem?.color || '#409eff'}; border-radius: 50%; margin-right: 8px;"></span>
              ${usageHtml}
            </div>
            ${isAnomaly ? `
              <div style="font-size: 12px; color: #f56c6c; margin-top: 5px; padding-top: 5px; border-top: 1px solid #555;">
                ⚠️ 检测到异常数据
              </div>
            ` : (hasData ? `
              <div style="font-size: 12px; color: #67c23a; margin-top: 5px; padding-top: 5px; border-top: 1px solid #555;">
                ✓ 用电正常
              </div>
            ` : '')}
          `;
        }
      },
      legend: {
        data: ['实际用电'],
        top: 10
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: timeSlots,
        axisLabel: {
          interval: 3, // 96个点，每隔3个点（即每4个点）显示一个标签，即每小时显示一个
          formatter: (value: string) => {
            // 只显示整点时间，如 00:00, 01:00
            return value.endsWith('00') ? value : '';
          }
        }
      },

      yAxis: {
        type: 'value',
        name: '用电量(kWh)',
        min: 0,
        max: yAxisMax 
      },
      series: [
        {
          name: '实际用电',
          type: 'line',
          data: usageData,
          smooth: true,
          connectNulls: false, // 不连接空数据点，以准确反映数据缺失
          symbol: 'circle', // 确保显示圆形点
          showSymbol: true, // 显示所有数据点
          symbolSize: 6,    // 默认点大小
          lineStyle: {
            width: 3,
            color: '#409eff'
          },
          itemStyle: {
            color: '#409eff'
          }
        }
      ]
    };

    
    console.log('设置图表选项:', option);
    chartInstance.value.setOption(option);
    
    
    chartInstance.value.hideLoading();
  } catch (error) {
    console.error('更新图表失败:', error);
    ElMessage.error('获取用电数据失败，请稍后重试');
    
    
    if (!isUnmounted.value && chartInstance.value) {
      chartInstance.value.hideLoading();
      
      
      const errorOption = {
        title: {
          text: '数据加载失败',
          subtext: '请检查网络连接或稍后重试',
          left: 'center',
          top: 'center',
          textStyle: {
            color: '#ff4d4f'
          },
          subtextStyle: {
            color: '#999'
          }
        }
      };
      chartInstance.value.setOption(errorOption);
    }
  }
};

let handleResize = () => {
  if (!isUnmounted.value && chartInstance.value) {
    chartInstance.value.resize();
  }
};

const generateMockUsageData = (): number[] => {
  const baseUsage = 60;
  const 波动范围 = 30;
  // 96个时间点（每15分钟一个）
  const points = Array.from({ length: 96 }, (_, i) => i); 
  return points.map((_: number, index: number) => {
    
    // 将96个点映射到24小时，以模拟高峰
    const hour = Math.floor(index / 4);
    
    let peakFactor = 1;
    if (hour >= 7 && hour <= 9) peakFactor = 1.5; 
    if (hour >= 18 && hour <= 21) peakFactor = 1.8; 
    
    const randomVariation = (Math.random() - 0.5) * 波动范围;
    
    return Math.max(0, baseUsage * peakFactor + randomVariation);
  });
};

let refreshInterval: number | null = null;

onMounted(async () => {
  
  const token = localStorage.getItem('token');
  console.log('当前认证令牌:', token);
  
  
  await loadUserList();
  
  // 默认选中今天
  selectedDate.value = new Date().toISOString().slice(0, 10);
  
  await getAnomalyTypeOptions();
  
  
  await Promise.all([
    loadAnomalyData(),
    getAnomalyStats(),
    getRealtimeMonitoringData()
  ]);
  
  
  if (activeTab.value === 'realtime') {
    
    await nextTick();
    await initChart();
  }
  
  
  connectWebSocket();
  
  
  refreshInterval = setInterval(() => {
    if (autoRefresh.value) {
      refreshData();
    }
  }, 10000);
  
  
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  
  isUnmounted.value = true;
  
  
  disconnectWebSocket();
  
  if (chartInstance.value) {
    chartInstance.value.dispose();
    chartInstance.value = null;
  }
  
  
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
  
  
  window.removeEventListener('resize', handleResize);
});

const stompClient = ref<any | null>(null);
const isConnected = ref(false);

const connectWebSocket = () => {
  
  const socket = new SockJS('/ws');
  stompClient.value = Stomp.over(socket);
  
  
  stompClient.value.connect({}, () => {
    console.log('WebSocket连接成功');
    isConnected.value = true;
    
    // 订阅全系统告警广播 (用于管理员或实时监控大屏)
    stompClient.value?.subscribe('/topic/alerts', (message: any) => {
      try {
        const parsedMessage = JSON.parse(message.body);
        if (parsedMessage) {
          // 如果消息结构直接是 alert 对象
          handleNewAlert(parsedMessage);
        }
      } catch (e) {
        console.error('解析广播告警消息失败:', e);
      }
    });
    
    const token = localStorage.getItem('token');
    if (token) {
      stompClient.value?.subscribe('/user/queue/user-alerts', (message: any) => {
        try {
          const parsedMessage = JSON.parse(message.body);
          if (parsedMessage && parsedMessage.data) {
             handleNewAlert(parsedMessage.data);
          } else if (parsedMessage) {
             // 兼容直接发送对象的情况
             handleNewAlert(parsedMessage);
          }
        } catch (e) {
          console.error('解析用户告警消息失败:', e);
        }
      });
    }
  }, (error: any) => {
    console.error('WebSocket连接失败:', error);
    isConnected.value = false;
    
    setTimeout(connectWebSocket, 5000);
  });
};

const disconnectWebSocket = () => {
  if (stompClient.value && isConnected.value) {
    stompClient.value.disconnect();
    stompClient.value = null;
    isConnected.value = false;
    console.log('WebSocket连接已断开');
  }
};

const handleNewAlert = (alertData: any) => {
  console.log('收到新警报:', alertData);
  
  // 去重处理：如果列表中已存在相同ID的告警，则忽略
  if (alertData.id && anomalyList.value.some(item => String(item.id) === String(alertData.id))) {
    console.log('忽略重复告警:', alertData.id);
    return;
  }

  // 数据字段映射，确保与表格显示字段一致
  if (alertData.anomalyType && !alertData.alertType) {
    alertData.alertType = alertData.anomalyType;
  }
  if (alertData.notes && !alertData.description) {
    alertData.description = alertData.notes;
  }
  
  if (alertData.severityLevel === 'high') {
    alertStats.highRisk++;
  } else if (alertData.severityLevel === 'medium') {
    alertStats.mediumRisk++;
  } else {
    alertStats.lowRisk++;
  }
  
  
  ElMessage({
    message: `收到新的${alertData.severityLevel === 'high' ? '高' : alertData.severityLevel === 'medium' ? '中' : '低'}风险警报: ${alertData.notes || alertData.description}`,
    type: alertData.severityLevel === 'high' ? 'error' : alertData.severityLevel === 'medium' ? 'warning' : 'info',
    duration: 5000
  });
  
  
  anomalyList.value.unshift(alertData);
  totalAnomalies.value++;
  
  
  
  
  
  
  if (activeTab.value === 'realtime') {
    updateChart();
  }
};
</script>

<style scoped>
.container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.status-alert {
  margin-bottom: 20px;
}

.status-badge {
  margin-right: 10px;
}

.status-button {
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
}

.detection-tabs {
  margin-bottom: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 16px;
}

.search-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
}

.search-input {
  width: 250px;
}

.filter-select {
  width: 150px;
}

.realtime-content {
  margin-bottom: 20px;
}

.alert-dashboard {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.alert-dashboard h3 {
  margin: 0;
  margin-right: 20px;
  color: #303133;
}

.auto-refresh-switch {
  margin-right: 8px;
}

.auto-refresh-text {
  margin-right: 20px;
}

.alert-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
  display: flex;
  align-items: center;
}

.stat-card.danger {
  border-left: 4px solid #f56c6c;
}

.stat-card.warning {
  border-left: 4px solid #e6a23c;
}

.stat-card.info {
  border-left: 4px solid #409eff;
}

.stat-card.success {
  border-left: 4px solid #67c23a;
}

.stat-content {
  display: flex;
  align-items: center;
  width: 100%;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  margin-right: 16px;
}

.stat-label {
  flex: 1;
  font-size: 16px;
}

.stat-icon {
  margin-left: auto;
}

.realtime-monitoring {
  margin-bottom: 20px;
}

.monitoring-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  width: 100%;
}

.alerts-content {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.records-content {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.tracking-content {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
.date-picker {
  width: 180px;
}
</style>
