<template>

  <div class="container">

    <h2>用电分析</h2>

    

    <div class="search-filters">

      <el-select v-model="selectedUserId" placeholder="选择用户" class="filter-select">

        <el-option label="全部用户" value="" />

        <el-option v-for="user in userList" :key="user.id" :label="user.name" :value="user.id.toString()" />

      </el-select>

      

      <div v-if="activeTab === 'trend' || activeTab === 'timeSlot'" class="time-range-selector">

        <el-date-picker

          v-model="trendDateRange"

          type="daterange"

          range-separator="至"

          start-placeholder="开始日期"

          end-placeholder="结束日期"

          format="YYYY-MM-DD"

          value-format="YYYY-MM-DD"

          @change="onTrendDateRangeChange"

          class="date-range-picker"

        />

        <el-button @click="setPresetDateRange('week')" size="small">最近7天</el-button>

        <el-button @click="setPresetDateRange('month')" size="small">最近30天</el-button>

        <el-button @click="setPresetDateRange('quarter')" size="small">最近90天</el-button>

      </div>

      <el-button type="primary" @click="analyzePatterns">分析</el-button>

    </div>



    <div class="analysis-tabs">

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">

        <el-tab-pane label="用电趋势分析" name="trend" />

        <el-tab-pane label="用电时段分析" name="timeSlot" />

        <el-tab-pane label="用电预测" name="prediction" />

      </el-tabs>

    </div>



    <div class="chart-container">

      <el-card class="chart-card">

        <template #header>

          <div class="card-header">

            <span>{{ chartTitle }}</span>

            <div class="chart-actions">

              <el-button size="small" @click="refreshChart">刷新</el-button>

              <el-button size="small" @click="exportChart">导出</el-button>

            </div>

          </div>

        </template>

        <div ref="chartRef" class="chart" style="height: 400px;" />

      </el-card>

    </div>



    <div v-if="activeTab === 'trend'" class="analysis-results">

      <el-card class="results-card">

        <template #header>

          <div class="card-header">

            <span>用电分析结果</span>

          </div>

        </template>

        <el-table :data="analysisResults" style="width: 100%">

          <el-table-column prop="userId" label="用户ID" width="100" />

          <el-table-column prop="username" label="用户名" width="120" />

          <el-table-column prop="averageDailyUsage" label="平均日用电量(kWh)" width="150" />

          <el-table-column prop="peakHour" label="用电高峰时段" width="150" />

          <el-table-column prop="patternType" label="用电规律类型" width="120" />

          <el-table-column prop="predictedMonthlyUsage" label="预测下月用电量(kWh)" width="150" />

          <el-table-column prop="analysisDate" label="分析日期" width="120" />

          <el-table-column label="操作" width="150" fixed="right">

            <template #default="scope">

              <el-button link type="primary" size="small" @click="viewDetails(scope.row)">查看详情</el-button>

              <el-button link type="primary" size="small" @click="exportReport(scope.row)">导出报告</el-button>

            </template>

          </el-table-column>

        </el-table>

      </el-card>

    </div>



    <div class="actions-bottom">

      <el-button type="primary" @click="generateAnalysisReport">生成分析报告</el-button>

      <el-button @click="exportAnalysisData">导出分析数据</el-button>

    </div>



    <!-- 详情弹窗 -->

    <el-dialog

      v-model="dialogVisible"

      title="用电规律详情"

      width="70%"

    >

      <div v-if="selectedDetail" class="detail-content">

        <el-descriptions :column="2" border>

          <el-descriptions-item label="用户ID">{{ selectedDetail.userId }}</el-descriptions-item>

          <el-descriptions-item label="用户名">{{ selectedDetail.username }}</el-descriptions-item>

          <el-descriptions-item label="平均日用电量">{{ selectedDetail.averageDailyUsage }} kWh</el-descriptions-item>

          <el-descriptions-item label="预测下月用电量">{{ selectedDetail.predictedMonthlyUsage }} kWh</el-descriptions-item>

          <el-descriptions-item label="用电高峰时段">{{ selectedDetail.peakHour }}</el-descriptions-item>

          <el-descriptions-item label="用电规律类型">{{ selectedDetail.patternType }}</el-descriptions-item>

          <el-descriptions-item label="分析日期">{{ selectedDetail.analysisDate }}</el-descriptions-item>

          <el-descriptions-item label="用电量环比" :span="selectedDetail.monthOverMonthChange ? 1 : 2">

            {{ selectedDetail.monthOverMonthChange || '暂无数据' }}

          </el-descriptions-item>

          <el-descriptions-item v-if="selectedDetail.yearOverYearChange" label="用电量同比">

            {{ selectedDetail.yearOverYearChange }}

          </el-descriptions-item>

          <el-descriptions-item v-if="selectedDetail.stability" label="用电稳定性" :span="2">

            {{ selectedDetail.stability }}

          </el-descriptions-item>

        </el-descriptions>

        

        <!-- 详细分析部分 -->

        <div v-if="selectedDetail.analysisNotes" class="analysis-notes">

          <h4>详细分析说明</h4>

          <p>{{ selectedDetail.analysisNotes }}</p>

        </div>

      </div>

      <div v-else class="no-data">

        加载中...

      </div>

      <template #footer>

        <span class="dialog-footer">

          <el-button @click="dialogVisible = false">关闭</el-button>

        </span>

      </template>

    </el-dialog>

  </div>

</template>



<script setup lang="ts">

import { ref, onMounted, onUnmounted, nextTick } from 'vue';

import * as echarts from 'echarts';

import type { ECharts } from 'echarts';

import { ElMessage, ElMessageBox } from 'element-plus';

import * as XLSX from 'xlsx';

import axios from 'axios';

import apiClient from '../utils/api';





interface User {

  id: number; 

  name: string; 

}



interface AnalysisResult {

  userId: number; 

  username: string;

  averageDailyUsage: number;

  peakHour: string;

  patternType: string;

  predictedMonthlyUsage: number;

  analysisDate: string;

}





const trendDateRange = ref<[string, string] | null>(null);

const activeTab = ref('trend');

const analysisPeriod = ref('7d');

const selectedUserId = ref('');

const analysisResults = ref<AnalysisResult[]>([]);

const chartRef = ref<HTMLDivElement | null>(null);

const chartInstance = ref<ECharts | null>(null);

const chartTitle = ref('用电趋势分析');

const analysisResult = ref(null);

const loading = ref(false);

const isUnmounted = ref(false);

let handleResize: (() => void) | null = null;

const chartData = ref({

  months: [],

  usageData: [],

  timeSlotData: [],

  historicalData: [],

  predictedData: []

});




const userList = ref<User[]>([]);




const setPresetDateRange = (preset: string) => {

  const end = new Date();

  const start = new Date();

  

  switch (preset) {

    case 'week':

      start.setDate(end.getDate() - 7);

      break;

    case 'month':

      start.setDate(end.getDate() - 30);

      break;

    case 'quarter':

      start.setDate(end.getDate() - 90);

      break;

  }

  

  trendDateRange.value = [

    start.toISOString().split('T')[0],

    end.toISOString().split('T')[0]

  ];

  

  
  onTrendDateRangeChange();

};




const onTrendDateRangeChange = () => {

  if (trendDateRange.value && (activeTab.value === 'trend' || activeTab.value === 'timeSlot')) {

    analyzePatterns();

  }

};



const resultsLoading = ref(false);





const generateChartData = async () => {

  try {

    

    let endpoint = '';

    const params: any = {};

    

    if (selectedUserId.value) {

      params.userId = selectedUserId.value;

    }

    

    

    if (activeTab.value === 'prediction') {

      
      params.days = 30;

    } else {

      
      params.period = '30d'; // 默认使用30天周期

    }

    

    

    switch (activeTab.value) {

      case 'statistics':
        endpoint = '/api/electricity-statistics/statistics';
        chartTitle.value = '用电量统计';
        break;
      case 'trend':
        endpoint = '/api/electricity-statistics/trend';
        chartTitle.value = '用电趋势分析';
        break;
      case 'timeSlot':
        endpoint = '/api/electricity-statistics/time-slot';
        chartTitle.value = '用电时段分析';
        break;
      case 'prediction':
        endpoint = '/api/electricity-statistics/prediction';
        chartTitle.value = '用电预测';
        break;
      default:
        endpoint = '/api/electricity-statistics/trend';
        chartTitle.value = '用电趋势分析';

    }

    

    

    console.log('请求API端点:', endpoint, '参数:', params);

    const response = await apiClient.get(endpoint, { params }) as any;

    console.log('API返回数据:', response);

    

    

    if (!response) {

      console.log('后端API返回空数据，使用模拟数据');

      throw new Error('后端API返回空数据');

    }

    

    

    if (activeTab.value === 'prediction') {

      

      if (response.months && response.historicalData && response.predictedData) {

        console.log('使用后端预测数据:', response);

        return response;

      } else {

        console.error('后端预测数据格式不正确:', response);

        throw new Error('后端预测数据格式不正确');

      }

    }

    

    

    if (response.months && response.usageData && Array.isArray(response.months) && Array.isArray(response.usageData)) {

      

      console.log('使用后端API数据:', response);

      return response;

    } else if (response.timeSlotData && Array.isArray(response.timeSlotData)) {

      

      console.log('使用时段分析数据:', response);

      return { timeSlotData: response.timeSlotData };

    } else if (response.historicalData && response.predictedData) {

      

      console.log('使用预测数据:', response);

      return response;

    } else if (Array.isArray(response) && response.length > 0 && response[0].date && response[0].value !== undefined) {

      

      console.log('转换daily-stats数据格式');

      const months = response.map((item: any) => item.date);

      const usageData = response.map((item: any) => item.value);

      return { months, usageData };

    } else if (typeof response === 'object' && (response.totalConsumption || response.averageConsumption)) {

      

      console.log('转换统计数据为图表数据');

      const months = ['统计数据'];

      const usageData = [response.totalConsumption || response.averageConsumption || 0];

      return { months, usageData };

    } else {

      

      if (Array.isArray(response)) {

        const items = response as any[];

        if (items.length > 0 && (items[0].recordTime || items[0].date)) {

          const map = new Map<string, number>();

          items.forEach((it: any) => {

            const dtStr = (it.recordTime || it.date || '').toString();

            if (!dtStr) return;

            const day = dtStr.slice(0, 10);

            const val = Number(it.electricityConsumption ?? it.value ?? 0) || 0;

            map.set(day, (map.get(day) || 0) + val);

          });

          const months = Array.from(map.keys()).sort();

          const usageData = months.map(d => map.get(d) || 0);

          return { months, usageData };

        }

      }

      console.log('其他数据格式，直接使用:', response);

      return response;

    }

  } catch (error) {

    console.error('获取图表数据失败:', error);

    ElMessage.error('获取图表数据失败，请稍后重试');

    

    

    if (activeTab.value === 'prediction') {

      

      console.log('预测数据获取失败，使用默认数据结构');

      return {

        months: [],

        historicalData: [],

        predictedData: []

      };

    }

    

    return {

      months: [],

      usageData: [],

      timeSlotData: [],

      historicalData: [],

      predictedData: []

    };

  }

};





const getElectricityPatterns = async (userId?: string, period?: string) => {

  try {

    loading.value = true;

    resultsLoading.value = true;

    

    
    const params: any = {};

    if (userId || selectedUserId.value) {

      params.userId = userId || selectedUserId.value;

    }

    

    
    if (activeTab.value === 'trend' || activeTab.value === 'timeSlot') {
      
      if (trendDateRange.value) {
        params.startTime = trendDateRange.value[0];
        params.endTime = trendDateRange.value[1];
      } else {
        params.period = period || '30d'; // 默认使用30天周期
      }
    } else if (activeTab.value === 'prediction') {
      
      params.days = 30;
    } else {
      
      params.period = '30d';
    }

    

    
    let endpoint = '';

    switch (activeTab.value) {

      case 'trend':
        endpoint = '/api/electricity-statistics/trend';
        break;
      case 'timeSlot':
        endpoint = '/api/electricity-statistics/time-slot';
        break;
      case 'prediction':
        endpoint = '/api/electricity-statistics/prediction';
        break;
      default:
        endpoint = '/api/electricity-statistics/trend';

    }

    

    console.log('请求API端点:', endpoint, '参数:', params);

    const response = await apiClient.get(endpoint, { params }) as any;

    console.log('API返回数据:', response);

    

    
    if (!response) {

      console.log('后端API返回空数据，使用模拟数据');

      throw new Error('后端API返回空数据');

    }

    

    
    if (activeTab.value === 'prediction') {

      
      if (response.months && response.historicalData && response.predictedData) {

        console.log('使用后端预测数据:', response);

        chartData.value = {

          months: response.months,

          usageData: [],

          timeSlotData: [],

          historicalData: response.historicalData,

          predictedData: response.predictedData

        };

        

        
        const selectedUser = userList.value.find(u => u.id.toString() === selectedUserId.value);

        if (selectedUser || !selectedUserId.value) { 
          
          const avgHistoricalUsage = response.historicalData.reduce((sum: number, val: number) => sum + val, 0) / response.historicalData.length;

          

          
          const predictedMonthlyUsage = Math.round(response.predictedData.reduce((sum: number, val: number) => sum + val, 0));

          

          
          const maxHistoricalUsage = Math.max(...response.historicalData);

          const minHistoricalUsage = Math.min(...response.historicalData);

          let patternType = '平稳型';

          if (maxHistoricalUsage / minHistoricalUsage > 3) patternType = '波动型';

          else if (maxHistoricalUsage / minHistoricalUsage > 1.5) patternType = '高峰型';

          

          
          const peakHour = '18:00-22:00'; 
          

          
          const result: AnalysisResult = {

            userId: selectedUser ? selectedUser.id : 0, 
            username: selectedUser ? selectedUser.name : '全部用户', 
            averageDailyUsage: Math.round(avgHistoricalUsage * 100) / 100,

            peakHour,

            patternType,

            predictedMonthlyUsage,

            analysisDate: new Date().toISOString().split('T')[0]

          };

          

          
          analysisResults.value = [result];

          console.log('已更新预测分析结果:', analysisResults.value);

        }

      } else {

        console.error('后端预测数据格式不正确:', response);

        throw new Error('后端预测数据格式不正确');

      }

    } else if (activeTab.value === 'timeSlot') {

      
      if (response.timeSlotData && Array.isArray(response.timeSlotData)) {

        console.log('使用时段分析数据:', response);

        chartData.value = {

          months: [],

          usageData: [],

          timeSlotData: response.timeSlotData,

          historicalData: [],

          predictedData: []

        };

      } else {

        console.warn('未获取到时段分析数据');

        chartData.value = {

          months: [],

          usageData: [],

          timeSlotData: [],

          historicalData: [],

          predictedData: []

        };

      }

    } else {

      
      if (response.months && response.usageData && Array.isArray(response.months) && Array.isArray(response.usageData)) {

        console.log('使用后端API数据:', response);

        chartData.value = {

          months: response.months,

          usageData: response.usageData,

          timeSlotData: [],

          historicalData: [],

          predictedData: []

        };

      } else if (Array.isArray(response) && response.length > 0 && response[0].date && response[0].value !== undefined) {

        
        console.log('转换daily-stats数据格式');

        const months = response.map((item: any) => item.date);

        const usageData = response.map((item: any) => item.value);

        chartData.value = {

          months,

          usageData,

          timeSlotData: [],

          historicalData: [],

          predictedData: []

        };

      } else {

        console.log('其他数据格式，直接使用:', response);

        chartData.value = {

          months: response.months || [],

          usageData: response.usageData || [],

          timeSlotData: [],

          historicalData: [],

          predictedData: []

        };

      }

    }

    

    
    updateChart();

    

    
    if (activeTab.value === 'trend' && chartData.value.usageData.length > 0) {

      const totalUsage = chartData.value.usageData.reduce((sum, val) => sum + val, 0);

      const avgUsage = totalUsage / chartData.value.usageData.length;

      const maxUsage = Math.max(...chartData.value.usageData);

      const minUsage = Math.min(...chartData.value.usageData);

      

      analysisResult.value = {

        totalUsage: totalUsage.toFixed(2),

        avgUsage: avgUsage.toFixed(2),

        maxUsage: maxUsage.toFixed(2),

        minUsage: minUsage.toFixed(2),

        trendDirection: totalUsage > 0 ? '上升' : '稳定',

        dataPoints: chartData.value.usageData.length

      };

      

      
      const selectedUser = userList.value.find(u => u.id.toString() === selectedUserId.value);

      if (selectedUser || !selectedUserId.value) { 
        
        let peakHour = '暂无数据';
        
        // 尝试从时段分析数据中获取真实的高峰时段
        try {
            const timeSlotParams: any = {};
            if (trendDateRange.value) {
                timeSlotParams.startDate = trendDateRange.value[0];
                timeSlotParams.endDate = trendDateRange.value[1];
            } else {
                timeSlotParams.period = analysisPeriod.value;
            }
            if (selectedUserId.value) {
                timeSlotParams.userId = selectedUserId.value;
            }
            
            const timeSlotResponse = await apiClient.get('/api/electricity-statistics/time-slot', { params: timeSlotParams }) as any;
            
            if (timeSlotResponse && timeSlotResponse.timeSlotData && Array.isArray(timeSlotResponse.timeSlotData) && timeSlotResponse.timeSlotData.length > 0) {
                const maxSlot = timeSlotResponse.timeSlotData.reduce((max: any, current: any) => 
                    (current.value > max.value) ? current : max
                , timeSlotResponse.timeSlotData[0]);
                
                if (maxSlot && maxSlot.name) {
                    peakHour = maxSlot.name;
                }
            }
        } catch (e) {
            console.warn('获取高峰时段失败', e);
        }

        
        let patternType = '平稳型';

        if (minUsage > 0) {
            if (maxUsage / minUsage > 3) patternType = '波动型';
            else if (maxUsage / minUsage > 1.5) patternType = '高峰型';
        }

        

        
        let predictedMonthlyUsage = 0;

        try {

          
          const predictionParams: any = {};

          

          
          if (trendDateRange.value) {
            predictionParams.startTime = trendDateRange.value[0];
            predictionParams.endTime = trendDateRange.value[1];
          } else {
            predictionParams.days = 30; 
          }

          

          
          if (selectedUser) {

            predictionParams.userId = selectedUser.id;

          }

          

          const predictionResponse = await apiClient.get('electricity-statistics/prediction', {

            params: predictionParams

          }) as any;

          

          
          if (predictionResponse && predictionResponse.predictedMonthlyUsage) {

            
            predictedMonthlyUsage = Math.round(predictionResponse.predictedMonthlyUsage);

          } else if (predictionResponse && predictionResponse.predictedData && Array.isArray(predictionResponse.predictedData) && predictionResponse.predictedData.length > 0) {

            
            // 计算预测数据的总和
            const sumPredicted = predictionResponse.predictedData
                .filter((val: any) => typeof val === 'number')
                .reduce((a: number, b: number) => a + b, 0);
            
            // 如果预测数据不足30天，按比例估算月用电量
            const predictedDays = predictionResponse.predictedData.filter((val: any) => typeof val === 'number').length;
            if (predictedDays > 0) {
                predictedMonthlyUsage = Math.round((sumPredicted / predictedDays) * 30);
            } else {
                predictedMonthlyUsage = 0;
            }

          } else if (predictionResponse && predictionResponse.predictedTotalUsage) {

            predictedMonthlyUsage = Math.round(predictionResponse.predictedTotalUsage);

          } else {

            
            predictedMonthlyUsage = 0;

          }

        } catch (error) {

          console.error('获取预测数据失败:', error);

          
          predictedMonthlyUsage = 0;

        }

        

        
        const result: AnalysisResult = {

          userId: selectedUser ? selectedUser.id : 0, 
          username: selectedUser ? selectedUser.name : '全部用户', 
          averageDailyUsage: Math.round(avgUsage * 100) / 100,

          peakHour,

          patternType,

          predictedMonthlyUsage,

          analysisDate: new Date().toISOString().split('T')[0]

        };

        

        
        analysisResults.value = [result];

        console.log('已更新分析结果:', analysisResults.value);

      }

    } else {
        // 如果没有数据，清空分析结果
        analysisResults.value = [];
    }

    if (activeTab.value === 'timeSlot' && chartData.value.timeSlotData.length > 0) {

      
      const selectedUser = userList.value.find(u => u.id.toString() === selectedUserId.value);

      if (selectedUser || !selectedUserId.value) { 
        
        const maxTimeSlot = chartData.value.timeSlotData.reduce((max, slot) => 

          slot.value > max.value ? slot : max, chartData.value.timeSlotData[0]);

        

        
        const totalUsage = chartData.value.timeSlotData.reduce((sum, slot) => sum + slot.value, 0);

        

        
        let days = 30;

        if (trendDateRange.value) {

          
          const start = new Date(trendDateRange.value[0]);

          const end = new Date(trendDateRange.value[1]);

          days = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;

        } else {

          
          days = parseInt(analysisPeriod.value) || 30;

        }

        const avgDailyUsage = totalUsage / days;

        

        
        const maxSlotValue = maxTimeSlot.value;

        const minSlotValue = Math.min(...chartData.value.timeSlotData.map(slot => slot.value));

        let patternType = '平稳型';

        if (minSlotValue > 0) {
            if (maxSlotValue / minSlotValue > 3) patternType = '波动型';
            else if (maxSlotValue / minSlotValue > 1.5) patternType = '高峰型';
        }

        

        
        let predictedMonthlyUsage = 0;

        try {

          
          const predictionParams: any = {};

          

          
          if (trendDateRange.value) {

            predictionParams.startDate = trendDateRange.value[0];

            predictionParams.endDate = trendDateRange.value[1];

            predictionParams.period = 'custom';

          } else {

            predictionParams.period = 30; 
          }

          

          
          if (selectedUser) {

            predictionParams.userId = selectedUser.id;

          }

          

          const predictionResponse = await apiClient.get('electricity-statistics/prediction', {

            params: predictionParams

          }) as any;

          

          
          if (predictionResponse && predictionResponse.predictedMonthlyUsage) {

            
            predictedMonthlyUsage = Math.round(predictionResponse.predictedMonthlyUsage);

          } else if (predictionResponse && predictionResponse.predictedData && Array.isArray(predictionResponse.predictedData) && predictionResponse.predictedData.length > 0) {

            
            // 计算预测数据的总和
            const sumPredicted = predictionResponse.predictedData
                .filter((val: any) => typeof val === 'number')
                .reduce((a: number, b: number) => a + b, 0);
            
            // 如果预测数据不足30天，按比例估算月用电量
            const predictedDays = predictionResponse.predictedData.filter((val: any) => typeof val === 'number').length;
            if (predictedDays > 0) {
                predictedMonthlyUsage = Math.round((sumPredicted / predictedDays) * 30);
            } else {
                predictedMonthlyUsage = 0;
            }

          } else if (predictionResponse && predictionResponse.predictedTotalUsage) {

            predictedMonthlyUsage = Math.round(predictionResponse.predictedTotalUsage);

          } else {

            
            predictedMonthlyUsage = 0;

          }

        } catch (error) {

          console.error('获取预测数据失败:', error);

          
          predictedMonthlyUsage = 0;

        }

        

        
        const result: AnalysisResult = {

          userId: selectedUser ? selectedUser.id : 0, 
          username: selectedUser ? selectedUser.name : '全部用户', 
          averageDailyUsage: Math.round(avgDailyUsage * 100) / 100,

          peakHour: maxTimeSlot.name,

          patternType,

          predictedMonthlyUsage,

          analysisDate: new Date().toISOString().split('T')[0]

        };

        

        
        analysisResults.value = [result];

        console.log('已更新时段分析结果:', analysisResults.value);

      }

    } else {
        // 如果没有数据，清空分析结果
        analysisResults.value = [];
    }

    

  } catch (error) {

    console.error('获取用电数据失败:', error);

    ElMessage.error('获取用电数据失败，请稍后重试');

    

    
    if (activeTab.value === 'prediction') {

      chartData.value = {

        months: [],

        historicalData: [],

        predictedData: [],

        usageData: [],

        timeSlotData: []

      };

    } else {

      chartData.value = {

        months: [],

        usageData: [],

        timeSlotData: [],

        historicalData: [],

        predictedData: []

      };

    }

    

    
    updateChart();

  } finally {

    loading.value = false;

    resultsLoading.value = false;

  }

};



const analyzePatterns = async () => {

  try {

    
    analysisResults.value = [];

    

    
    await getElectricityPatterns(selectedUserId.value, analysisPeriod.value);

    await updateChart();

    

    
    if (analysisResults.value.length === 0) {

      ElMessage.warning('未能获取到用电分析结果，请检查数据或稍后重试');

    }

  } catch (error) {

    console.error('分析用电规律失败:', error);

    ElMessage.error('分析用电规律失败，请稍后重试');

  }

};





const handleTabChange = async (tabName: string) => {

  activeTab.value = tabName;

  

  
  await getElectricityPatterns();

  

  
  await updateChart();

};





const refreshChart = async () => {

  await updateChart();

};





const exportChart = () => {

  if (chartInstance.value) {

    const url = chartInstance.value.getDataURL({

      type: 'png',

      pixelRatio: 2,

      backgroundColor: '#fff'

    });

    const link = document.createElement('a');

    link.download = `${chartTitle.value}.png`;

    link.href = url;

    link.click();

  }

};






const dialogVisible = ref(false);

const selectedDetail = ref<AnalysisResult | null>(null);



const viewDetails = async (row: AnalysisResult) => {

  console.log('查看详情:', row);

  selectedDetail.value = row;

  

  try {

    
    const response = await apiClient.get('electricity-statistics/detailed-stats', {

      params: {

        userId: row.userId || undefined,

        period: analysisPeriod.value

      }

    }) as any;

    

    
    if (response) {

      selectedDetail.value = { ...row, ...response };

    }

  } catch (error) {

    console.warn('获取详细数据失败，使用基础数据:', error);

  }

  

  
  dialogVisible.value = true;

};







const exportReport = async (row: AnalysisResult) => {

  try {

    console.log('开始导出TXT报告', { userId: row.userId, period: analysisPeriod.value });

    

    

    const endDate = new Date();

    let startDate = new Date();

    const days = parseInt(analysisPeriod.value) || 30;

    startDate.setDate(endDate.getDate() - days);

    

    

    const formatDate = (date: Date) => {

      return date.toLocaleDateString('zh-CN');

    };

    

    

    let detailedData: any = {};

    try {

      

      const response = await apiClient.get('electricity-statistics/detailed-stats', {

        params: {

          userId: row.userId || undefined,

          period: '30d'

        }

      }) as any;

      detailedData = response || {};

    } catch (error) {

      console.warn('获取详细数据失败，使用基础数据:', error);

    }

    

    

    const estimatedCost = (row.predictedMonthlyUsage * 0.6).toFixed(2);

    

    

    const monthOverMonthChange = detailedData.monthOverMonthChange || '暂无数据';

    const yearOverYearChange = detailedData.yearOverYearChange || '暂无数据';

    const stability = detailedData.stability || '暂无数据';

    

    

    let alerts = [];

    try {

      const alertsResponse = await apiClient.get('electricity-statistics/alerts', {

        params: {

          userId: row.userId || undefined,

          period: '30d'

        }

      }) as any;

      alerts = alertsResponse || [];

    } catch (error) {

      console.warn('获取告警数据失败:', error);

    }

    

    

    const powerOverloadCount = alerts.filter((a: any) => a.type === '功率超限').length;

    const lineFaultCount = alerts.filter((a: any) => a.type === '线路故障').length;

    const totalAlerts = alerts.length;

    

    

    let suggestions = [];

    try {

      const suggestionsResponse = await apiClient.get('electricity-statistics/suggestions', {

        params: {

          userId: row.userId || undefined,

          period: '30d'

        }

      }) as any;

      suggestions = suggestionsResponse || [];

    } catch (error) {

      console.warn('获取建议数据失败:', error);

    }

    

    

    const lines = [

      '========================================',

      '         用电分析报告',

      '========================================',

      '',

      `报告对象: ${row.username}`,

      `报告周期: ${formatDate(startDate)} 至 ${formatDate(endDate)} (${days}天)`,

      `生成日期: ${formatDate(endDate)}`,

      '',

      '--- 核心指标 ---',

      `平均日用电量: ${row.averageDailyUsage} kWh`,

      `预测月用电量: ${row.predictedMonthlyUsage} kWh`,

      `预估总电费: ¥${estimatedCost} (按0.6元/度估算)`,

      '',

      '--- 趋势分析 ---',

      `用电量环比: ${monthOverMonthChange}`,

      `用电量同比: ${yearOverYearChange}`,

      `用电稳定性: ${stability}`,

      '',

      '--- 规律分析 ---',

      `主要用电模式: ${row.patternType}`,

      `主要高峰时段: ${row.peakHour}`,

      '',

      '--- 风险提示 ---',

      `本周期异常告警: 共${totalAlerts}次`,

      `  - 功率超限: ${powerOverloadCount}次`,

      `  - 线路故障: ${lineFaultCount}次`,

      ''

    ];

    

    

    lines.push('--- 系统建议 ---');

    if (suggestions.length > 0) {

      suggestions.forEach((suggestion: any) => {

        lines.push(`> ${suggestion.content}`);

      });

    } else {

      

      if (row.averageDailyUsage > 150) {

        lines.push('> 用电量较高，建议优化用电设备使用时间，避免高峰时段集中用电。');

      }

      if (powerOverloadCount > 1) {

        lines.push('> 功率超限告警较多，建议检查用电设备容量是否匹配。');

      }

      if (row.peakHour.includes('19:00') || row.peakHour.includes('20:00')) {

        lines.push('> 晚间高峰负荷较为集中，建议关注电网容量。');

      }

    }

    

    

    const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' });

    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');

    link.href = url;

    link.download = `用电分析报告_${row.username}_${analysisPeriod.value}.txt`;

    document.body.appendChild(link); 

    link.click();

    

    

    setTimeout(() => {

      document.body.removeChild(link);

      URL.revokeObjectURL(url);

    }, 100);

    

    ElMessage.success(`已成功导出${row.username}的TXT报告`);

  } catch (error) {

    console.error('导出报告失败:', error);

    ElMessage.error('导出报告失败');

  }

};





const generateAnalysisReport = async () => {

  try {

    console.log('开始生成TXT分析报告', { userId: selectedUserId.value, period: analysisPeriod.value });

    

    

    if (analysisResults.value.length === 0) {

      await getElectricityPatterns(selectedUserId.value, analysisPeriod.value);

    }

    

    

    const row = analysisResults.value[0];

    if (!row) {

      ElMessage.warning('没有可导出的分析数据，请先进行用电分析');

      return;

    }

    

    

    const endDate = new Date();

    let startDate = new Date();

    const days = parseInt(analysisPeriod.value) || 30;

    startDate.setDate(endDate.getDate() - days);

    

    

    const formatDate = (date: Date) => {

      return date.toLocaleDateString('zh-CN');

    };

    

    

    let detailedData: any = {};

    try {

      

      const response = await apiClient.get('electricity-statistics/detailed-stats', {

        params: {

          userId: selectedUserId.value || undefined,

          period: '30d'

        }

      }) as any;

      detailedData = response || {};

    } catch (error) {

      console.warn('获取详细数据失败，使用基础数据:', error);

    }

    

    

    const estimatedCost = (row.predictedMonthlyUsage * 0.6).toFixed(2);

    

    

    const monthOverMonthChange = detailedData.monthOverMonthChange || '暂无数据';

    const yearOverYearChange = detailedData.yearOverYearChange || '暂无数据';

    const stability = detailedData.stability || '暂无数据';

    

    

    let alerts = [];

    try {

      const alertsResponse = await apiClient.get('electricity-statistics/alerts', {

        params: {

          userId: selectedUserId.value || undefined,

          period: analysisPeriod.value

        }

      }) as any;

      alerts = alertsResponse || [];

    } catch (error) {

      console.warn('获取告警数据失败:', error);

    }

    

    

    const powerOverloadCount = alerts.filter((a: any) => a.type === '功率超限').length;

    const lineFaultCount = alerts.filter((a: any) => a.type === '线路故障').length;

    const totalAlerts = alerts.length;

    

    

    let suggestions = [];

    try {

      const suggestionsResponse = await apiClient.get('electricity-statistics/suggestions', {

        params: {

          userId: selectedUserId.value || undefined,

          period: analysisPeriod.value

        }

      }) as any;

      suggestions = suggestionsResponse || [];

    } catch (error) {

      console.warn('获取建议数据失败:', error);

    }

    

    

    const lines = [

      '========================================',

      '         用电分析报告',

      '========================================',

      '',

      `报告对象: ${selectedUserId.value ? row.username : '全部用户'}`,

      `报告周期: ${formatDate(startDate)} 至 ${formatDate(endDate)} (${days}天)`,

      `生成日期: ${formatDate(endDate)}`,

      '',

      '--- 核心指标 ---',

      `平均日用电量: ${row.averageDailyUsage} kWh`,

      `预测月用电量: ${row.predictedMonthlyUsage} kWh`,

      `预估总电费: ¥${estimatedCost} (按0.6元/度估算)`,

      '',

      '--- 趋势分析 ---',

      `用电量环比: ${monthOverMonthChange}`,

      `用电量同比: ${yearOverYearChange}`,

      `用电稳定性: ${stability}`,

      '',

      '--- 规律分析 ---',

      `主要用电模式: ${row.patternType}`,

      `主要高峰时段: ${row.peakHour}`,

      '',

      '--- 风险提示 ---',

      `本周期异常告警: 共${totalAlerts}次`,

      `  - 功率超限: ${powerOverloadCount}次`,

      `  - 线路故障: ${lineFaultCount}次`,

      ''

    ];

    

    

    lines.push('--- 系统建议 ---');

    if (suggestions.length > 0) {

      suggestions.forEach((suggestion: any) => {

        lines.push(`> ${suggestion.content}`);

      });

    } else {

      

      if (row.averageDailyUsage > 150) {

        lines.push('> 用电量较高，建议优化用电设备使用时间，避免高峰时段集中用电。');

      }

      if (powerOverloadCount > 1) {

        lines.push('> 功率超限告警较多，建议检查用电设备容量是否匹配。');

      }

      if (row.peakHour.includes('19:00') || row.peakHour.includes('20:00')) {

        lines.push('> 晚间高峰负荷较为集中，建议关注电网容量。');

      }

    }

    

    

    const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' });

    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');

    link.href = url;

    link.download = `用电分析报告_${selectedUserId.value ? row.username : '全部用户'}_${analysisPeriod.value}.txt`;

    document.body.appendChild(link); 

    link.click();

    

    

    setTimeout(() => {

      document.body.removeChild(link);

      URL.revokeObjectURL(url);

    }, 100);

    

    ElMessage.success('报告生成并下载成功');

  } catch (error) {

    console.error('生成分析报告失败:', error);

    ElMessage.error('生成分析报告失败');

  }

};





const exportAnalysisData = async () => {

  try {

    

    if (analysisResults.value.length === 0) {

      await getElectricityPatterns(selectedUserId.value, analysisPeriod.value);

    }

    

    

    const detailedDataResponse = await apiClient.get('electricity-statistics/detailed-stats', {

      params: {

        userId: selectedUserId.value || undefined,

        period: analysisPeriod.value

      }

    });

    

    

    const alertsResponse = await apiClient.get('electricity-statistics/alerts', {

      params: {

        userId: selectedUserId.value || undefined,

        period: analysisPeriod.value

      }

    });

    

    

    const workbook = XLSX.utils.book_new();

    

    

    

    const summaryData = [

      ['用户ID', '用户名', '分析周期', '平均日用电量(kWh)', '用电高峰时段', '用电规律类型', '预测月用电量(kWh)', '分析日期'],

      [

        selectedUserId.value || '全部用户',

        selectedUserId.value ? analysisResults.value[0]?.username || '未知用户' : '全部用户',

        '30d',

        analysisResults.value[0]?.averageDailyUsage || 0,

        analysisResults.value[0]?.peakHour || '未知',

        analysisResults.value[0]?.patternType || '未知',

        analysisResults.value[0]?.predictedMonthlyUsage || 0,

        analysisResults.value[0]?.analysisDate || new Date().toISOString().split('T')[0]

      ]

    ];

    

    const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);

    XLSX.utils.book_append_sheet(workbook, summarySheet, '总体摘要');

    

    

    const dailyData = [];

    const dailyHeaders = ['日期', '当日总用电量(kWh)', '当日高峰时段用电量(kWh)', '当日谷时段用电量(kWh)', '当日告警次数'];

    dailyData.push(dailyHeaders);

    

    

    if (detailedDataResponse.data && detailedDataResponse.data.dailyData) {

      detailedDataResponse.data.dailyData.forEach((dayData: any) => {

        

        const dayAlerts = alertsResponse.data && alertsResponse.data.alerts 

          ? alertsResponse.data.alerts.filter((alert: any) => {

              const alertDate = new Date(alert.timestamp).toISOString().split('T')[0];

              const dataDate = new Date(dayData.date).toISOString().split('T')[0];

              return alertDate === dataDate;

            }).length

          : 0;

        

        dailyData.push([

          dayData.date,

          dayData.totalUsage || 0,

          dayData.peakUsage || 0,

          dayData.valleyUsage || 0,

          dayAlerts

        ]);

      });

    } else {

      
      // 数据不足时，导出空数据或仅表头
      // 移除原有的伪造数据逻辑

    }

    

    const dailySheet = XLSX.utils.aoa_to_sheet(dailyData);

    XLSX.utils.book_append_sheet(workbook, dailySheet, '每日数据');

    

    

    XLSX.writeFile(workbook, `用电分析数据_${selectedUserId.value || '全部用户'}_${analysisPeriod.value}.xlsx`);

    

    ElMessage.success('已导出分析数据');

  } catch (error) {

    console.error('导出分析数据失败:', error);

    

    try {

      

      if (analysisResults.value.length === 0) {

        await getElectricityPatterns(selectedUserId.value, analysisPeriod.value);

      }

      

      

      const workbook = XLSX.utils.book_new();

      

      

      

      const summaryData = [

        ['用户ID', '用户名', '分析周期', '平均日用电量(kWh)', '用电高峰时段', '用电规律类型', '预测月用电量(kWh)', '分析日期'],

        [

          selectedUserId.value || '全部用户',

          selectedUserId.value ? analysisResults.value[0]?.username || '未知用户' : '全部用户',

          analysisPeriod.value,

          analysisResults.value[0]?.averageDailyUsage || 0,

          analysisResults.value[0]?.peakHour || '未知',

          analysisResults.value[0]?.patternType || '未知',

          analysisResults.value[0]?.predictedMonthlyUsage || 0,

          analysisResults.value[0]?.analysisDate || new Date().toISOString().split('T')[0]

        ]

      ];

      

      const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);

      XLSX.utils.book_append_sheet(workbook, summarySheet, '总体摘要');

      

      

      const dailyData = [];

      const dailyHeaders = ['日期', '当日总用电量(kWh)', '当日高峰时段用电量(kWh)', '当日谷时段用电量(kWh)', '当日告警次数'];

      dailyData.push(dailyHeaders);

      

      

      const today = new Date();

      // 移除原有的伪造数据逻辑，仅导出表头或空行
      /*
      for (let i = 29; i >= 0; i--) {

        const date = new Date(today);

        date.setDate(today.getDate() - i);

        const dateStr = date.toISOString().split('T')[0];

        

        

        const avgUsage = analysisResults.value[0]?.averageDailyUsage || 120;

        const totalUsage = Math.round((avgUsage * (0.8 + Math.random() * 0.4)) * 100) / 100;

        const peakUsage = Math.round(totalUsage * 0.4 * 100) / 100;

        const valleyUsage = Math.round(totalUsage * 0.2 * 100) / 100;

        const alerts = Math.floor(Math.random() * 3);

        

        dailyData.push([dateStr, totalUsage, peakUsage, valleyUsage, alerts]);

      }
      */

      

      const dailySheet = XLSX.utils.aoa_to_sheet(dailyData);

      XLSX.utils.book_append_sheet(workbook, dailySheet, '每日数据');

      

      

      XLSX.writeFile(workbook, `用电分析数据_${selectedUserId.value || '全部用户'}_${analysisPeriod.value}.xlsx`);

      

      ElMessage.success('已导出分析数据（部分数据为模拟数据）');

    } catch (e) {

      console.error('导出分析数据完全失败:', e);

      ElMessage.error('导出分析数据失败，请稍后重试');

    }

  }

};





const initChart = async () => {

  if (isUnmounted.value) return; 

  

  await nextTick();

  

  if (!chartRef.value || isUnmounted.value) return;

  

  try {

    
    if (chartInstance.value) {

      chartInstance.value.dispose();

    }

    

    
    chartInstance.value = echarts.init(chartRef.value);

    

    
    const emptyOption = {

      title: {

        text: '正在加载数据...',

        left: 'center',

        top: 'center',

        textStyle: {

          color: '#999'

        }

      }

    };

    chartInstance.value.setOption(emptyOption);

    

    
    setTimeout(() => {

      if (!isUnmounted.value && chartInstance.value) {

        updateChart();

      }

    }, 100);

    

    
    handleResize = () => {

      if (chartInstance.value && !isUnmounted.value) {

        chartInstance.value.resize();

      }

    };

    

    window.addEventListener('resize', handleResize);

  } catch (error) {

    console.error('初始化图表失败:', error);

  }

};






const updateChart = async () => {

  
  if (!chartInstance.value) {

    console.warn('图表实例不存在，尝试重新初始化');

    await initChart();

  }

  

  
  if (!chartInstance.value) {

    console.error('图表实例初始化失败');

    return;

  }

  

  try {

    
    chartInstance.value.showLoading();

    

    let option = {};

    

    switch (activeTab.value) {

      case 'trend':

        
        let sortedMonths = chartData.value.months || [];

        let sortedUsageData = chartData.value.usageData || [];

        

        
        if (sortedMonths.length === 0 || sortedUsageData.length === 0) {

          option = {

            title: {

              text: '暂无趋势分析数据，请点击"分析"按钮获取数据',

              left: 'center',

              top: 'middle',

              textStyle: {

                color: '#999'

              }

            },

            xAxis: { show: false },

            yAxis: { show: false },

            series: []

          };

          break;

        }

        

        
        if (sortedMonths.length > 0 && /^\d{4}-\d{2}-\d{2}$/.test(sortedMonths[0])) {

          interface DateUsagePair {

            date: Date;

            usage: number;

          }

          

          const dateDataMap = sortedMonths.map((date: string, index: number) => ({

            date: new Date(date),

            usage: sortedUsageData[index]

          }));

          

          dateDataMap.sort((a: DateUsagePair, b: DateUsagePair) => a.date.getTime() - b.date.getTime());

          

          sortedMonths = dateDataMap.map((item: DateUsagePair) => item.date.toISOString().split('T')[0]);

          sortedUsageData = dateDataMap.map((item: DateUsagePair) => item.usage);

        }

        

        option = {

          tooltip: {

            trigger: 'axis',

            backgroundColor: 'rgba(50, 50, 50, 0.8)',

            borderColor: '#409eff',

            borderWidth: 1,

            textStyle: {

              color: '#fff',

              fontSize: 14

            },

            formatter: function(params: any) {

              const data = params[0];

              const date = new Date(data.axisValue);

              const formattedDate = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;

              return `

                <div style="font-weight: bold; margin-bottom: 5px;">${formattedDate}</div>

                <div style="display: flex; align-items: center;">

                  <span style="display: inline-block; width: 10px; height: 10px; background-color: ${data.color}; border-radius: 50%; margin-right: 8px;"></span>

                  用电量: <span style="font-weight: bold; margin-left: 5px;">${data.value} kWh</span>

                </div>

              `;

            }

          },

          legend: {

            data: ['用电量'],

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

            data: sortedMonths

          },

          yAxis: {

            type: 'value',

            name: '用电量(kWh)'

          },

          series: [

            {

              name: '用电量',

              type: 'line',

              data: sortedUsageData,

              smooth: true,

              lineStyle: {

                color: '#3b82f6'

              },

              areaStyle: {

                color: {

                  type: 'linear',

                  x: 0,

                  y: 0,

                  x2: 0,

                  y2: 1,

                  colorStops: [

                    {

                      offset: 0,

                      color: 'rgba(59, 130, 246, 0.5)'

                    },

                    {

                      offset: 1,

                      color: 'rgba(59, 130, 246, 0.1)'

                    }

                  ]

                }

              }

            }

          ]

        };

        break;

        

      // 更新时段分析图表逻辑
      case 'timeSlot':

        
        const hourlyData = chartData.value.timeSlotData || [];

        
        if (hourlyData.length === 0) {

          option = {

            title: {

              text: '暂无时段分析数据',

              left: 'center',

              top: 'middle',

              textStyle: {

                color: '#999'

              }

            },

            xAxis: { show: false },

            yAxis: { show: false },

            series: []

          };

          break;

        }

        
        // 构造 0-23 小时的数据
        let hours = Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`);
        let values = new Array(24).fill(0);

        
        if (hourlyData.length > 0) {
            hourlyData.forEach((item: any) => {
                // 后端返回的 item 应该包含 hour 字段 (0-23)
                if (typeof item.hour === 'number' && item.hour >= 0 && item.hour < 24) {
                    values[item.hour] = item.value || 0;
                } else if (item.name && item.name.includes(':')) {
                    // 兼容处理：尝试从 name "05:00" 解析 hour
                    const hourStr = item.name.split(':')[0];
                    const h = parseInt(hourStr, 10);
                    if (!isNaN(h) && h >= 0 && h < 24) {
                        values[h] = item.value || 0;
                    }
                }
            });
        }

        
        option = {

          tooltip: {

            trigger: 'axis',

            axisPointer: {

              type: 'shadow'

            },

            backgroundColor: 'rgba(50, 50, 50, 0.8)',

            borderColor: '#409eff',

            borderWidth: 1,

            textStyle: {

              color: '#fff',

              fontSize: 14

            },

            formatter: function(params: any) {

              const data = params[0];

              return `

                <div style="font-weight: bold; margin-bottom: 5px;">时段: ${data.axisValue}</div>

                <div style="display: flex; align-items: center;">

                  <span style="display: inline-block; width: 10px; height: 10px; background-color: ${data.color}; border-radius: 50%; margin-right: 8px;"></span>

                  用电量: <span style="font-weight: bold; margin-left: 5px;">${data.value.toFixed(2)} kWh</span>

                </div>

                <div style="font-size: 12px; color: #ccc; margin-top: 5px;">

                  占全天用电量: ${((data.value / values.reduce((a: number, b: number) => a + b, 0)) * 100).toFixed(1)}%

                </div>

              `;

            }

          },

          grid: {

            left: '3%',

            right: '4%',

            bottom: '3%',

            containLabel: true

          },

          xAxis: {

            type: 'category',

            data: hours,

            axisLabel: {

              rotate: 0, // 0-23 小时可以不用旋转，或者根据空间调整
              interval: 2 // 每隔2小时显示一个标签，避免拥挤

            }

          },

          yAxis: {

            type: 'value',

            name: '用电量(kWh)'

          },

          series: [

            {

              name: '用电量',

              type: 'bar',

              data: values,

              itemStyle: {

                color: '#3b82f6'

              },
              // 增加柱状图圆角美化
              barMaxWidth: 30,
              itemStyle: {
                  borderRadius: [4, 4, 0, 0],
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                      { offset: 0, color: '#83bff6' },
                      { offset: 0.5, color: '#188df0' },
                      { offset: 1, color: '#188df0' }
                  ])
              },
              emphasis: {
                  itemStyle: {
                      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                          { offset: 0, color: '#2378f7' },
                          { offset: 0.7, color: '#2378f7' },
                          { offset: 1, color: '#83bff6' }
                      ])
                  }
              }

            }

          ]

        };

        break;

        

      case 'prediction':

        
        const historicalData = chartData.value.historicalData || [];

        const predictedData = chartData.value.predictedData || [];

        const months = chartData.value.months || [];

        

        
        if (historicalData.length === 0 || predictedData.length === 0 || months.length === 0) {

          option = {

            title: {

              text: '暂无预测数据，请点击"分析"按钮获取数据',

              left: 'center',

              top: 'middle',

              textStyle: {

                color: '#999'

              }

            },

            xAxis: { show: false },

            yAxis: { show: false },

            series: []

          };

          break;

        }

        

        
        const fullMonths = [...months];

        
        const lastDate = new Date(months[months.length - 1]);

        for (let i = 1; i <= predictedData.length; i++) {

          const nextDate = new Date(lastDate);

          nextDate.setDate(lastDate.getDate() + i);

          fullMonths.push(nextDate.toISOString().split('T')[0]);

        }

        

        
        const fullPredictedData = new Array(historicalData.length).fill(null);

        fullPredictedData.push(...predictedData);

        

        option = {

          tooltip: {

            trigger: 'axis',

            backgroundColor: 'rgba(50, 50, 50, 0.8)',

            borderColor: '#409eff',

            borderWidth: 1,

            textStyle: {

              color: '#fff',

              fontSize: 14

            },

            formatter: function(params: any) {

              let result = `<div style="font-weight: bold; margin-bottom: 8px;">${params[0].axisValue}</div>`;

              params.forEach((param: any) => {

                if (param.value !== null && param.value !== undefined) {

                  const dataType = param.seriesName === '历史数据' ? '历史用电量' : '预测用电量';

                  const valueColor = param.seriesName === '历史数据' ? '#3b82f6' : '#ef4444';

                  result += `

                    <div style="display: flex; align-items: center; margin-bottom: 5px;">

                      <span style="display: inline-block; width: 10px; height: 10px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>

                      ${dataType}: <span style="font-weight: bold; margin-left: 5px; color: ${valueColor};">${param.value} kWh</span>

                    </div>

                  `;

                }

              });

              

              
              const historicalIndex = params.findIndex((p: any) => p.seriesName === '历史数据' && p.value !== null);

              const predictedIndex = params.findIndex((p: any) => p.seriesName === '预测数据' && p.value !== null);

              

              if (predictedIndex >= 0 && historicalIndex >= 0) {

                const change = ((params[predictedIndex].value - params[historicalIndex].value) / params[historicalIndex].value * 100).toFixed(1);

                const changeColor = change > 0 ? '#f56c6c' : '#67c23a';

                const changeSymbol = change > 0 ? '↑' : '↓';

                result += `

                  <div style="font-size: 12px; color: #ccc; margin-top: 5px; padding-top: 5px; border-top: 1px solid #555;">

                    预测变化: <span style="color: ${changeColor}; font-weight: bold;">${changeSymbol} ${Math.abs(Number(change))}%</span>

                  </div>

                `;

              }

              

              return result;

            }

          },

          legend: {

            data: ['历史数据', '预测数据'],

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

            data: fullMonths

          },

          yAxis: {

            type: 'value',

            name: '用电量(kWh)'

          },

          series: [

            {

              name: '历史数据',

              type: 'line',

              data: historicalData, 

              smooth: true,

              lineStyle: {

                color: '#3b82f6',

                width: 3

              },

              itemStyle: {

                color: '#3b82f6'

              },

              areaStyle: {

                color: {

                  type: 'linear',

                  x: 0,

                  y: 0,

                  x2: 0,

                  y2: 1,

                  colorStops: [

                    {

                      offset: 0,

                      color: 'rgba(59, 130, 246, 0.3)'

                    },

                    {

                      offset: 1,

                      color: 'rgba(59, 130, 246, 0.05)'

                    }

                  ]

                }

              }

            },

            {

              name: '预测数据',

              type: 'line',

              data: fullPredictedData, 

              smooth: true,

              lineStyle: {

                color: '#ef4444',

                type: 'dashed',

                width: 3

              },

              itemStyle: {

                color: '#ef4444'

              },

              markLine: {

                silent: true,

                lineStyle: {

                  color: '#999',

                  type: 'dotted'

                },

                data: [

                  {

                    xAxis: historicalData.length - 1,

                    label: {

                      formatter: '预测起点',

                      position: 'end'

                    }

                  }

                ]

              }

            }

          ]

        };

        break;

    }

    

    
    chartInstance.value.setOption(option, true);

  } catch (error) {

    console.error('更新图表失败:', error);

    ElMessage.error('更新图表失败');

  } finally {

    if (chartInstance.value) {

      chartInstance.value.hideLoading();

    }

  }

};



const initUserList = async () => {
  try {
    console.log('开始获取用户列表...');
    const response = await apiClient.get('users', { params: { pageSize: 1000 } });
    console.log('用户列表响应:', response);
    
    // 检查是否是分页格式响应
    if (response && typeof response === 'object') {
      if (response.records && Array.isArray(response.records)) {
        // 从分页响应中提取用户列表
        userList.value = response.records;
      } else if (Array.isArray(response)) {
        // 兼容直接返回数组的情况
        userList.value = response;
      } else {
        console.warn('用户列表数据格式不正确，不是分页格式或数组:', response);
        userList.value = [];
      }
      
      // 如果有用户数据且未选择用户，自动选择第一个
      if (!selectedUserId.value && userList.value.length > 0) {
        selectedUserId.value = userList.value[0].id.toString();
        console.log('自动选择第一个用户:', selectedUserId.value);
      }
    } else {
      console.warn('用户列表数据格式不正确:', response);
      userList.value = [];
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    userList.value = [];
  }
};





onMounted(async () => {

  try {

    
    await initUserList();

    

    
    await initChart();

    

    
    if (selectedUserId.value) {

      await getElectricityPatterns(selectedUserId.value, analysisPeriod.value);

      await updateChart();

    } else {

      
      await updateChart();

    }

  } catch (error) {

    console.error('组件初始化失败:', error);

  }

});





onUnmounted(() => {

  isUnmounted.value = true;

  if (handleResize) {

    window.removeEventListener('resize', handleResize);

    handleResize = null;

  }

  if (chartInstance.value) {

    chartInstance.value.dispose();

    chartInstance.value = null;

  }

});

</script>



<style scoped>

.detail-content {

  padding: 10px 0;

}



.analysis-notes {

  margin-top: 20px;

  padding: 15px;

  background-color: #f5f7fa;

  border-radius: 4px;

  border-left: 4px solid #409eff;

}



.analysis-notes h4 {

  margin-top: 0;

  margin-bottom: 10px;

  color: #303133;

}



.analysis-notes p {

  color: #606266;

  line-height: 1.6;

  margin: 0;

}



.no-data {

  text-align: center;

  padding: 40px 0;

  color: #909399;

}



.container {

  padding: 20px;

  background-color: #f5f7fa;

  min-height: 100vh;

  width: 100%;

  max-width: none;

}



@media (max-width: 768px) {

  .container {

    padding: 10px;

  }

}



h2 {

  margin-bottom: 20px;

  color: #303133;

  font-size: 1.5rem;

}



@media (max-width: 768px) {

  h2 {

    font-size: 1.2rem;

    margin-bottom: 15px;

  }

}



.search-filters {

  display: flex;

  flex-wrap: wrap;

  gap: 12px;

  margin-bottom: 20px;

  padding: 16px;

  background-color: #fff;

  border-radius: 8px;

  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

}



@media (max-width: 768px) {

  .search-filters {

    flex-direction: column;

    gap: 8px;

    padding: 12px;

  }

}



.filter-select {

  width: 180px;

  min-width: 140px;

}



@media (max-width: 768px) {

  .filter-select {

    width: 100%;

  }

}



.analysis-tabs {

  margin-bottom: 20px;

  background-color: #fff;

  border-radius: 8px;

  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  padding: 0 16px;

}



@media (max-width: 768px) {

  .analysis-tabs {

    padding: 0 8px;

    margin-bottom: 15px;

  }

}



.chart-container {

  margin-bottom: 20px;

}



.chart-card {

  background-color: #fff;

  border-radius: 8px;

  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  overflow-x: auto;

}



.card-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  flex-wrap: wrap;

  gap: 10px;

}



@media (max-width: 768px) {

  .card-header {

    flex-direction: column;

    align-items: flex-start;

  }

}



.chart-actions {

  display: flex;

  gap: 8px;

  flex-wrap: wrap;

}



@media (max-width: 768px) {

  .chart-actions {

    width: 100%;

    justify-content: flex-end;

  }

}



.chart {

  width: 100%;

  height: 400px;

  min-height: 300px;

}



@media (max-width: 768px) {

  .chart {

    height: 300px;

    min-height: 250px;

  }

}



@media (max-width: 480px) {

  .chart {

    height: 250px;

    min-height: 200px;

  }

}



.analysis-results {

  margin-bottom: 20px;

}



.results-card {

  background-color: #fff;

  border-radius: 8px;

  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  overflow-x: auto;

}



@media (max-width: 768px) {

  .results-card {

    margin-bottom: 15px;

  }

}



.actions-bottom {

  display: flex;

  gap: 12px;

  justify-content: flex-end;

  padding: 16px;

  background-color: #fff;

  border-radius: 8px;

  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  flex-wrap: wrap;

}



@media (max-width: 768px) {

  .actions-bottom {

    justify-content: center;

    padding: 12px;

    gap: 8px;

  }

}





:deep(.el-table) {

  font-size: 14px;

}



@media (max-width: 768px) {

  :deep(.el-table) {

    font-size: 12px;

  }

  

  :deep(.el-table .el-table__header-wrapper) {

    overflow-x: auto;

  }

  

  :deep(.el-table .el-table__body-wrapper) {

    overflow-x: auto;

  }

}





@media (max-width: 480px) {

  :deep(.el-button) {

    font-size: 12px;

    padding: 6px 12px;

  }

  

  :deep(.el-button--small) {

    font-size: 11px;

    padding: 4px 8px;

  }

}





@media (max-width: 768px) {

  :deep(.el-select) {

    width: 100%;

  }

}





@media (max-width: 768px) {

  :deep(.el-tabs__header) {

    margin: 0 0 10px;

  }

  

  :deep(.el-tabs__nav-wrap) {

    overflow-x: auto;

  }

  

  :deep(.el-tabs__item) {

    font-size: 12px;

    padding: 0 12px;

  }

}





:deep(.chart) {

  width: 100% !important;

  max-width: 100%;

}

</style>