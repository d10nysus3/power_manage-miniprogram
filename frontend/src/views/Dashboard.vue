<template>

  <div class="dashboard-container">

    <div class="dashboard-header">

      <h2>特需用户用电监测与异常预警系统</h2>

      <p>欢迎使用系统，实时监控电力使用情况</p>

    </div>



    

    <div class="status-cards">

      <el-card class="status-card" v-loading="loading">

        <div class="card-content">

          <div class="card-icon">

            <el-icon size="40" color="#409eff"><UserFilled /></el-icon>

          </div>

          <div class="card-data">

            <div class="card-value">{{ systemStatus.totalUsers.toLocaleString() }}</div>

            <div class="card-label">用户总数</div>

          </div>

          <div class="card-trend">

            <span :class="systemStatus.userTrend.startsWith('+') ? 'trend-up' : 'trend-down'">{{ systemStatus.userTrend }}</span>

            <span class="trend-text">较上月</span>

          </div>

        </div>

      </el-card>



      <el-card class="status-card" v-loading="loading">

        <div class="card-content">

          <div class="card-icon">

            <el-icon size="40" color="#f56c6c"><WarnTriangleFilled /></el-icon>

          </div>

          <div class="card-data">

            <div class="card-value">{{ systemStatus.todayAnomalies }}</div>

            <div class="card-label">今日异常</div>

          </div>

          <div class="card-trend">

            <span :class="systemStatus.anomalyTrend.startsWith('+') ? 'trend-up' : 'trend-down'">{{ systemStatus.anomalyTrend }}</span>

            <span class="trend-text">较昨日</span>

          </div>

        </div>

      </el-card>



      <el-card class="status-card" v-loading="loading">

        <div class="card-content">

          <div class="card-icon">

            <el-icon size="40" color="#67c23a"><Check /></el-icon>

          </div>

          <div class="card-data">

            <div class="card-value">{{ systemStatus.processingRate }}</div>

            <div class="card-label">处理率</div>

          </div>

          <div class="card-trend">

            <span :class="systemStatus.rateTrend.startsWith('+') ? 'trend-up' : 'trend-down'">{{ systemStatus.rateTrend }}</span>

            <span class="trend-text">较上周</span>

          </div>

        </div>

      </el-card>



      <el-card class="status-card" v-loading="loading">

        <div class="card-content">

          <div class="card-icon">

            <el-icon size="40" color="#e6a23c"><TrendCharts /></el-icon>

          </div>

          <div class="card-data">

            <div class="card-value">{{ systemStatus.todayElectricity }}</div>

            <div class="card-label">今日用电量</div>

          </div>

          <div class="card-trend">

            <span :class="systemStatus.electricityTrend.startsWith('+') ? 'trend-up' : 'trend-down'">{{ systemStatus.electricityTrend }}</span>

            <span class="trend-text">较昨日</span>

          </div>

        </div>

      </el-card>

    </div>



    

    <div class="charts-container">

      <el-card class="chart-card">

        <template #header>

          <div class="card-header">

            <span>用电量趋势图</span>

            <el-select v-model="timeRange" placeholder="选择时间范围" class="time-select" @change="updateTrendChart">

              <el-option label="近7天" value="7days" />

              <el-option label="近30天" value="30days" />

              <el-option label="近90天" value="90days" />

            </el-select>

          </div>

        </template>

        <div ref="trendChartRef" class="chart" style="height: 350px;" v-loading="trendChartLoading" />

      </el-card>



      <el-card class="chart-card">

        <template #header>

          <div class="card-header">

            <span>异常类型分布</span>

          </div>

        </template>

        <div ref="typeChartRef" class="chart" style="height: 350px;" v-loading="typeChartLoading" />

      </el-card>

    </div>



    <!-- 最近异常记录 -->

    <div class="recent-records">

      <el-card class="records-card">

        <template #header>

          <div class="card-header">

            <span>最近异常记录</span>

            <el-button type="text" @click="viewAllRecords">查看全部</el-button>

          </div>

        </template>

        <el-table :data="recentRecords" style="width: 100%" v-loading="loading">

          <el-table-column prop="id" label="ID" width="80" />

          <el-table-column prop="userName" label="用户" width="120" />

          <el-table-column prop="alertType" label="异常类型" width="120">

            <template #default="scope">

              <el-tag

                :type="scope.row.alertType === 'overload' ? 'danger' : (scope.row.alertType === 'leakage' ? 'warning' : 'info')"

              >

                {{ scope.row.alertType === 'overload' ? '过载' : (scope.row.alertType === 'leakage' ? '漏电' : '异常波动') }}

              </el-tag>

            </template>

          </el-table-column>

          <el-table-column prop="triggerReason" label="位置" width="150" />

          <el-table-column prop="alertTime" label="发生时间" width="160" />

          <el-table-column prop="isProcessed" label="状态" width="100">

            <template #default="scope">

              <el-tag

                :type="scope.row.isProcessed ? 'success' : 'info'"

              >

                {{ scope.row.isProcessed ? '已处理' : '待处理' }}

              </el-tag>

            </template>

          </el-table-column>

          <el-table-column label="操作" width="80" fixed="right">

            <template #default="scope">

              <el-button link type="primary" size="small" @click="viewDetail(scope.row)">查看</el-button>

            </template>

          </el-table-column>

        </el-table>

      </el-card>

    </div>

  </div>

</template>



<script setup lang="ts">

import { ref, onMounted, onUnmounted, nextTick, reactive } from 'vue';

import { UserFilled, Check, WarnTriangleFilled, TrendCharts } from '@element-plus/icons-vue';

import * as echarts from 'echarts';

import type { ECharts } from 'echarts';

import router from '../router';

import apiClient from '../utils/api';

import { ElMessage } from 'element-plus';





interface AnomalyRecord {

  id: number; 

  userName: string; 

  alertType: string; 

  triggerReason: string; 

  alertTime: string; 

  severityLevel: string; 

  alertContent: string; 

  isProcessed: boolean; 

  processTime?: string; 

  processor?: string; 

}



interface SystemStatus {

  totalUsers: number;

  todayAnomalies: number;

  processingRate: string;

  todayElectricity: string;

  userTrend: string;

  anomalyTrend: string;

  rateTrend: string;

  electricityTrend: string;

}



interface AnomalyTypeDistribution {

  name: string;

  value: number;

  itemStyle: { color: string };

}





const timeRange = ref('7days');

const trendChartRef = ref<HTMLElement>();

const typeChartRef = ref<HTMLElement>();

const trendChartInstance = ref<ECharts | null>(null);

const typeChartInstance = ref<ECharts | null>(null);





const recentRecords = ref<AnomalyRecord[]>([]);

const systemStatus = reactive<SystemStatus>({

  totalUsers: 0,

  todayAnomalies: 0,

  processingRate: '0%',

  todayElectricity: '0kWh',

  userTrend: '',

  anomalyTrend: '',

  rateTrend: '',

  electricityTrend: ''

});





const loading = ref(false);

const trendChartLoading = ref(false);

const typeChartLoading = ref(false);

































const getSystemStatus = async () => {

  try {

    loading.value = true;

    

    const data = await apiClient.get('dashboard/system-status') as any;

    

    

    if (data && data.error) {

      console.error('后端返回错误:', data.error);

      ElMessage.error('系统状态数据获取失败: ' + data.error);

      return;

    }

    

    

    const hasValidData = data && 

                       (data.totalUsers > 0 || 

                        data.todayAnomalies > 0 || 

                        data.processingRate > 0 || 

                        data.todayElectricity > 0);



    if (hasValidData) {

      systemStatus.totalUsers = data.totalUsers || 0;

      systemStatus.todayAnomalies = data.todayAnomalies || 0;

      systemStatus.processingRate = `${data.processingRate || 0}%`;

      systemStatus.todayElectricity = `${data.todayElectricity || 0}万kWh`;

      

      systemStatus.userTrend = data.userTrend !== undefined ? (data.userTrend > 0 ? `+${data.userTrend}%` : `${data.userTrend}%`) : '';

      systemStatus.anomalyTrend = data.anomalyTrend !== undefined ? (data.anomalyTrend > 0 ? `+${data.anomalyTrend}%` : `${data.anomalyTrend}%`) : '';

      systemStatus.rateTrend = data.rateTrend !== undefined ? (data.rateTrend > 0 ? `+${data.rateTrend}%` : `${data.rateTrend}%`) : '';

      systemStatus.electricityTrend = data.electricityTrend !== undefined ? (data.electricityTrend > 0 ? `+${data.electricityTrend}%` : `${data.electricityTrend}%`) : '';

    } else {

      

      console.warn('API返回的数据无效');

      ElMessage.error('无法获取系统状态数据，请检查数据源');

    }

  } catch (error) {

    console.error('获取系统状态数据失败:', error);

    ElMessage.error('获取系统状态数据失败，请检查网络连接或联系管理员');

  } finally {

    loading.value = false;

  }

};





const getRecentRecords = async () => {

  const userNameMap: Record<string, string> = {
    '用户1': '张三',
    '用户2': '李四',
    '用户3': '王五',
    '用户4': '赵六',
    '用户5': '钱七',
    '用户6': '孙八',
    '用户7': '周九',
    '用户8': '吴十',
    '用户9': '郑十一',
    '用户10': '王十二'
  };
  
  try {
    
    const resp = await apiClient.get('anomalies/recent', { params: { limit: 5 } }) as any;

    

    

    if (resp && resp.error) {

      console.error('后端返回错误:', resp.error);

      ElMessage.error('异常记录数据获取失败: ' + resp.error);

      recentRecords.value = [];

      return;

    }

    

    const data = resp?.records || [];

    

    

    if (data.length > 0) {

      recentRecords.value = data.map((record: any) => ({

        id: record.id || 0,

        

        userName: userNameMap[record.userName as string] || record.userName || record.user?.name || '未知用户',

        alertType: record.alertType || record.anomalyType || '',

        triggerReason: record.triggerReason || record.location || '',

        alertTime: record.alertTime || record.detectionTime || '',

        severityLevel: record.severityLevel || 'low',

        alertContent: record.alertContent || record.anomalyDetails || record.description || '',

        isProcessed: record.isProcessed || false,

        processTime: record.processTime || '',

        processor: record.processor || ''

      }));

    } else {

      

      console.warn('未获取到异常记录数据');

      recentRecords.value = [];

    }

  } catch (error) {

    console.error('获取最近异常记录失败:', error);

    ElMessage.error('获取最近异常记录失败，请检查网络连接或联系管理员');

    

    recentRecords.value = [];

  }

};





const getElectricityData = async (days: number) => {

  try {

    
    const resp = await apiClient.get('electricity-data/daily-stats', { params: { days } }) as any;

    

    

    if (resp && resp.error) {

      console.error('后端返回错误:', resp.error);

      ElMessage.error('用电量数据获取失败: ' + resp.error);

      return { dates: [], values: [] };

    }

    

    

    const dates: string[] = [];

    const values: number[] = [];

    

    

    if (resp && Array.isArray(resp) && resp.length > 0 && !resp[0].error) {

      resp.forEach((item: any) => {

        dates.push(item.date);

        values.push(item.value);

      });

    } else {

      

      console.warn('用电量数据无效');

      ElMessage.warning('用电量数据获取异常');

      return { dates: [], values: [] };

    }

    

    return { dates, values };

  } catch (error) {

    console.error('获取用电量数据失败:', error);

    ElMessage.error('获取用电量数据失败，请检查网络连接或联系管理员');

    

    return { dates: [], values: [] };

  }

  
};





const getAnomalyTypeDistribution = async (): Promise<AnomalyTypeDistribution[]> => {
  try {
    
    const stats = await apiClient.get('anomalies/type-distribution') as any;

    

    

    if (stats && stats.error) {

      console.error('后端返回错误:', stats.error);

      ElMessage.error('异常类型分布数据获取失败: ' + stats.error);

      return [];

    }

    

    if (stats && Array.isArray(stats)) {

      return stats as any;

    } else {

      

      console.warn('异常类型分布数据无效');

      ElMessage.warning('异常类型分布数据获取异常');

      return [];

    }

  } catch (error) {

    console.error('获取异常类型分布数据失败:', error);

    ElMessage.error('获取异常类型分布数据失败，请检查网络连接或联系管理员');

    

    return [];

  }

};





const initTrendChart = () => {

  if (!trendChartRef.value) return;

  

  
  if (trendChartInstance.value) {

    trendChartInstance.value.dispose();

  }

  

  
  trendChartInstance.value = echarts.init(trendChartRef.value);

  

  
  updateTrendChart();

};



const initTypeChart = () => {

  if (!typeChartRef.value) return;

  

  
  if (typeChartInstance.value) {

    typeChartInstance.value.dispose();

  }

  

  
  typeChartInstance.value = echarts.init(typeChartRef.value);

  

  
  updateTypeChart();

};





const updateTrendChart = async () => {

  if (!trendChartInstance.value) return;

  

  try {

    trendChartLoading.value = true;

    const days = timeRange.value === '7days' ? 7 : (timeRange.value === '30days' ? 30 : 90);

    const { dates, values } = await getElectricityData(days);

    

    const option = {

      tooltip: {

        trigger: 'axis',

        backgroundColor: 'rgba(50, 50, 50, 0.8)',

        borderColor: '#409eff',

        borderWidth: 1,

        textStyle: {

          color: '#fff',

          fontSize: 14

        },

        formatter: (params: any) => {

          const param = params[0];

          const date = param.axisValue;

          const value = param.value;

          

          
          const isLatestData = param.dataIndex === params.length - 1;

          

          
          let changeInfo = '';

          if (param.dataIndex > 0) {

            const prevValue = params[0].data[param.dataIndex - 1];

            if (prevValue && prevValue !== null) {

              const changeValue = (value - prevValue) / prevValue * 100;
              const change = changeValue.toFixed(1);

              const changeColor = changeValue > 0 ? '#f56c6c' : '#67c23a';

              const changeSymbol = changeValue > 0 ? '↑' : '↓';

              changeInfo = `

                <div style="font-size: 12px; color: #ccc; margin-top: 5px; padding-top: 5px; border-top: 1px solid #555;">

                  较前日: <span style="color: ${changeColor}; font-weight: bold;">${changeSymbol} ${Math.abs(Number(change))}%</span>

                </div>

              `;

            }

          }

          

          return `

            <div style="font-weight: bold; margin-bottom: 8px;">${date}</div>

            <div style="display: flex; align-items: center; margin-bottom: 5px;">

              <span style="display: inline-block; width: 10px; height: 10px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>

              用电量: <span style="font-weight: bold; margin-left: 5px; color: ${isLatestData ? '#e6a23c' : '#409eff'};">${value} 万kWh</span>

            </div>

            ${isLatestData ? `

              <div style="font-size: 12px; color: #e6a23c; margin-top: 5px; padding-top: 5px; border-top: 1px solid #555;">

                📊 最新数据

              </div>

            ` : ''}

            ${changeInfo}

          `;

        }

      },

      graphic: (!dates || dates.length === 0) ? [{

        type: 'text',

        left: 'center',

        top: 'middle',

        style: { text: '暂无数据', fill: '#909399', fontSize: 14 }

      }] : undefined,

      grid: {

        left: '3%',

        right: '4%',

        bottom: '15%',  

        top: '10%',

        containLabel: true

      },

      xAxis: {

        type: 'category',

        boundaryGap: false,

        data: dates,

        axisLabel: {

          interval: days > 30 ? Math.floor(dates.length / 10) : (days > 7 ? 1 : 0),

          rotate: days > 30 ? 45 : 0,

          fontSize: 12,

          margin: 15  

        },

        axisLine: {

          lineStyle: {

            color: '#ccc'

          }

        }

      },

      yAxis: {

        type: 'value',

        name: '用电量(万kWh)',

        nameTextStyle: {

          fontSize: 12,

          padding: [0, 0, 10, 0]  

        },

        axisLabel: {

          fontSize: 12

        },

        axisLine: {

          lineStyle: {

            color: '#ccc'

          }

        },

        splitLine: {

          lineStyle: {

            color: '#f0f0f0'

          }

        }

      },

      series: [

        {

          name: '用电量',

          type: 'line',

          stack: 'Total',

          data: values,

          lineStyle: {

            width: 3,

            color: '#409eff'

          },

          itemStyle: {

            color: '#409eff',

            borderWidth: 2,

            borderColor: '#fff'

          },

          areaStyle: {

            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [

              {

                offset: 0,

                color: 'rgba(64, 158, 255, 0.5)'

              },

              {

                offset: 1,

                color: 'rgba(64, 158, 255, 0.1)'

              }

            ])

          },

          smooth: true

        }

      ]

    };

    

    trendChartInstance.value.setOption(option);

  } catch (error) {

    console.error('更新趋势图失败:', error);

    ElMessage.error('更新趋势图失败');

  } finally {

    trendChartLoading.value = false;

  }

};





const updateTypeChart = async () => {

  if (!typeChartInstance.value) return;

  

  try {

    typeChartLoading.value = true;

    const distributionData = await getAnomalyTypeDistribution();

    const legendData = distributionData.map(item => item.name);

    

    const option = {

      tooltip: {

        trigger: 'item',

        formatter: '{a} <br/>{b}: {c} ({d}%)',

        backgroundColor: 'rgba(50, 50, 50, 0.8)',

        textStyle: {

          color: '#fff',

          fontSize: 12

        }

      },

      graphic: (!distributionData || distributionData.length === 0) ? [{

        type: 'text',

        left: 'center',

        top: 'middle',

        style: { text: '暂无数据', fill: '#909399', fontSize: 14 }

      }] : undefined,

      legend: {

        orient: 'vertical',

        left: 10,

        top: 'center',

        data: legendData,

        textStyle: {

          fontSize: 12

        }

      },

      series: [

        {

          name: '异常类型',

          type: 'pie',

          radius: ['35%', '65%'],

          avoidLabelOverlap: false,

          itemStyle: {

            borderRadius: 10,

            borderColor: '#fff',

            borderWidth: 2

          },

          label: {

            show: false,

            position: 'center'

          },

          emphasis: {

            label: {

              show: true,

              fontSize: '18',

              fontWeight: 'bold'

            }

          },

          labelLine: {

            show: false

          },

          data: distributionData

        }

      ]

    };

    

    typeChartInstance.value.setOption(option);

  } catch (error) {

    console.error('更新类型分布图失败:', error);

    ElMessage.error('更新类型分布图失败');

  } finally {

    typeChartLoading.value = false;

  }

};









const viewAllRecords = () => {

  router.push('/anomaly-detection');

};





const viewDetail = (record: AnomalyRecord) => {

  router.push(`/anomaly-detection?recordId=${record.id}`);

};





const handleResize = () => {

  if (trendChartInstance.value) {

    trendChartInstance.value.resize();

  }

  if (typeChartInstance.value) {

    typeChartInstance.value.resize();

  }

};





onMounted(async () => {

  loading.value = true;

  

  try {

    

    await Promise.all([

      getSystemStatus(),

      getRecentRecords()

    ]);

  } catch (error) {

    console.error('初始化数据失败:', error);

  } finally {

    loading.value = false;

  }

  

  

  await nextTick();

  await Promise.all([

    initTrendChart(),

    initTypeChart()

  ]);

  

  window.addEventListener('resize', handleResize);

});





onUnmounted(() => {

  if (trendChartInstance.value) {

    trendChartInstance.value.dispose();

  }

  if (typeChartInstance.value) {

    typeChartInstance.value.dispose();

  }

  window.removeEventListener('resize', handleResize);

});

</script>



<style scoped>

.dashboard-container {

  padding: 20px;

  background-color: #f5f7fa;

  min-height: 100vh;

  width: 100%;

  max-width: none;

}



.dashboard-header {

  margin-bottom: 30px;

  text-align: center;

}



.dashboard-header h2 {

  font-size: 28px;

  color: #303133;

  margin-bottom: 10px;

}



.dashboard-header p {

  font-size: 16px;

  color: #606266;

}



.status-cards {

  display: grid;

  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));

  gap: 16px;

  margin-bottom: 30px;

}



.status-card {

  height: 140px;

}



.card-content {

  display: flex;

  align-items: center;

  height: 100%;

}



.card-icon {

  margin-right: 16px;

}



.card-data {

  flex: 1;

}



.card-value {

  font-size: 32px;

  font-weight: bold;

  color: #303133;

  margin-bottom: 8px;

}



.card-label {

  font-size: 14px;

  color: #606266;

}



.card-trend {

  text-align: right;

}



.trend-up {

  color: #f56c6c;

  font-weight: bold;

}



.trend-down {

  color: #67c23a;

  font-weight: bold;

}



.trend-text {

  font-size: 12px;

  color: #909399;

  display: block;

  margin-top: 4px;

}



.charts-container {

  display: grid;

  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));

  gap: 20px;

  margin-bottom: 30px;

}



.chart-card {

  height: 400px;

  display: flex;

  flex-direction: column;

}



.card-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

}



.time-select {

  width: 120px;

}



.chart {

  flex: 1;

  width: 100%;

  padding: 20px;

  box-sizing: border-box;

}



.recent-records {

  margin-bottom: 20px;

}



.records-card {

  height: 400px;

  display: flex;

  flex-direction: column;

}



.records-card .el-table {

  flex: 1;

}

</style>