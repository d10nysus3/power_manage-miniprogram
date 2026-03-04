<template>
  <div class="container">
    <h2>应急处理</h2>
    
    <div class="status-alert">
      <el-badge :value="emergencyStats.processingEvents || 0" type="danger" class="status-badge">
        <el-button type="text" class="status-button">有点紧急事件待处理</el-button>
      </el-badge>
    </div>

    <div class="emergency-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="应急响应" name="response" />
        <!-- 删除资源调配标签 -->
        <!-- <el-tab-pane label="资源调配" name="resources" /> -->
        <el-tab-pane label="通知联络" name="notification" />
        <el-tab-pane label="消息列表" name="messages" />
        <el-tab-pane label="事件记录" name="records" />
      </el-tabs>
    </div>

    <div class="emergency-process">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="启动应急预案" description="立即启动紧急处理流程" />
        <el-step title="联系相关人员" description="通知电力部门和维修人员" />
        <el-step title="整理处理要求" description="明确问题和处理优先级" />
      </el-steps>
    </div>

    <div class="search-filters">
      <el-select v-model="eventType" placeholder="事件类型" class="filter-select" clearable>
        <el-option
          v-for="option in eventTypeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <el-select v-model="emergencyLevel" placeholder="紧急程度" class="filter-select" clearable>
        <el-option
          v-for="option in emergencyLevelOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <el-select v-model="processingStatus" placeholder="处理状态" class="filter-select" clearable>
        <el-option
          v-for="option in processingStatusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-select v-model="handler" placeholder="负责人" class="filter-select">
        <el-option label="全部" value="" />
        <el-option 
          v-for="person in personnelList" 
          :key="person.id" 
          :label="person.name" 
          :value="person.name" 
        />
      </el-select>
      <el-button type="primary" @click="searchEvents">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    
    <div v-if="activeTab === 'response'" class="response-content">
      <div class="dashboard-container">
        <h3>应急处理控制面板</h3>
        <el-button type="primary" class="emergency-btn" @click="startEmergencyResponse">启动应急响应</el-button>
        <el-button class="contact-btn" @click="contactPersonnel">联系相关人员</el-button>
        <el-button class="report-btn" @click="generateReport">生成处理报告</el-button>
        <el-button class="settings-btn" @click="openSettings">应急设置</el-button>
      </div>

      <div class="emergency-dashboard">
        <el-card class="stat-card danger" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ emergencyStats.emergencyEvents }}</div>
            <div class="stat-label">紧急事件</div>
            <div class="stat-icon">
              <el-icon size="32" color="#f56c6c"><Warning /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card warning" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ emergencyStats.processingEvents }}</div>
            <div class="stat-label">处理中</div>
            <div class="stat-icon">
              <el-icon size="32" color="#e6a23c"><Clock /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card success" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ emergencyStats.completedEvents }}</div>
            <div class="stat-label">今日完成</div>
            <div class="stat-icon">
              <el-icon size="32" color="#67c23a"><Check /></el-icon>
            </div>
          </div>
        </el-card>
        
        <el-card class="stat-card info" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-number">{{ emergencyStats.avgResponseTime }}</div>
            <div class="stat-label">平均响应时间</div>
            <div class="stat-icon">
              <el-icon size="32" color="#409eff"><Timer /></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 通知联络 -->
    <div v-if="activeTab === 'notification'" class="notification-content">
      <el-card class="panel-card">
        <template #header>
          <div class="card-header">
            <span>通知联络</span>
          </div>
        </template>
        <div class="notify-form">
          <el-select v-model="selectedPersonnel" multiple placeholder="选择联系人" class="notify-select">
            <el-option
              v-for="p in personnelList"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
          <el-input
            v-model="notificationMessage"
            type="textarea"
            :rows="4"
            placeholder="输入要发送的通知内容"
          />
          <div style="text-align:right;margin-top:12px;">
            <el-button type="primary" @click="contactPersonnel">发送通知</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 消息列表 -->
    <div v-if="activeTab === 'messages'" class="messages-tab-content">
      <div class="messages-header">
        <el-select v-model="messageFilter" placeholder="筛选消息类型" style="width: 180px;">
          <el-option label="全部消息" value="all" />
          <el-option label="应急消息" value="emergency" />
          <el-option label="异常用电" value="anomaly" />
          <el-option label="系统通知" value="system" />
        </el-select>
      </div>
      
      <el-card class="messages-card">
        <el-table 
          v-loading="messagesLoading"
          :data="filteredMessages" 
          stripe 
          style="width: 100%"
          @row-click="handleMessageRowClick"
        >
          <el-table-column prop="id" label="消息ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column label="类型" width="100">
            <template #default="scope">
              <el-tag 
                size="small" 
                :type="getMessageTagType(scope.row.type)"
              >
                {{ getMessageTagText(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="300">
            <template #default="scope">
              <div class="message-content" :title="scope.row.content">
                {{ scope.row.content }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="timestamp" label="时间" width="180" show-overflow-tooltip>
            <template #default="scope">
              {{ formatMessageDateTime(scope.row.timestamp) }}
            </template>
          </el-table-column>
          <el-table-column prop="isRead" label="状态" width="80">
            <template #default="scope">
              <el-tag 
                size="small" 
                :type="scope.row.isRead ? 'info' : 'primary'"
              >
                {{ scope.row.isRead ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button 
                type="text" 
                size="small" 
                @click="markMessageAsRead(scope.row.id)"
                :disabled="scope.row.isRead"
              >
                标记已读
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="messageCurrentPage"
            v-model:page-size="messagePageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredMessages.length"
          />
        </div>
      </el-card>
      
      <!-- 消息详情对话框 -->
      <el-dialog 
        v-model="messageDialogVisible" 
        :title="selectedMessage?.title || '消息详情'" 
        width="600px"
        @open="onMessageDialogOpen"
      >
        <div class="message-detail">
          <div class="detail-info">
            <span class="detail-type">
              <el-tag :type="getMessageTagType(selectedMessage?.type)">
                {{ getMessageTagText(selectedMessage?.type) }}
              </el-tag>
            </span>
            <span class="detail-time">
              {{ formatMessageDateTime(selectedMessage?.timestamp) }}
            </span>
          </div>
          <div class="detail-content">
            {{ selectedMessage?.content }}
          </div>
          <div v-if="selectedMessage?.relatedData" class="detail-related">
            <h4>相关数据</h4>
            <pre>{{ JSON.stringify(selectedMessage.relatedData, null, 2) }}</pre>
          </div>
        </div>
      </el-dialog>
    </div>

    <!-- 事件记录 -->
    <div v-if="activeTab === 'records'" class="records-tab-content">
      <el-card class="panel-card">
        <template #header>
          <div class="card-header">
            <span>事件记录</span>
          </div>
        </template>
        <el-table :data="eventList" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="事件ID" width="90" />
          <el-table-column prop="alertType" label="事件类型" width="140" />
          <el-table-column prop="alertTime" label="发生时间" width="180" />
          <el-table-column prop="severityLevel" label="紧急程度" width="100">
            <template #default="scope">
              <el-tag
                :type="scope.row.severityLevel === 'high' ? 'danger' : (scope.row.severityLevel === 'medium' ? 'warning' : 'info')"
              >
                {{ scope.row.severityLevel === 'high' ? '一级' : (scope.row.severityLevel === 'medium' ? '二级' : '三级') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理状态" width="120">
            <template #default="scope">
              <el-tag
                :type="scope.row.isProcessed ? 'success' : 'info'"
              >
                {{ scope.row.isProcessed ? '已处理' : '未处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="processor" label="负责人" width="120" />
          <el-table-column prop="alertContent" label="事件描述" show-overflow-tooltip />
        </el-table>
      </el-card>
      <div class="pagination" v-if="activeTab === 'records'">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalEvents"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 事件列表：仅在“应急响应/资源调配”下显示 -->
    <div class="event-list" v-if="activeTab === 'response' || activeTab === 'resources'">
      <el-table :data="eventList" style="width: 100%" v-loading="loading">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="事件ID" width="100" />
        <el-table-column prop="alertType" label="事件类型" width="120" />
        <el-table-column prop="alertTime" label="发生时间" width="160" />
        <el-table-column prop="severityLevel" label="紧急程度" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.severityLevel === 'high' ? 'danger' : (scope.row.severityLevel === 'medium' ? 'warning' : 'info')"
            >
              {{ scope.row.severityLevel === 'high' ? '一级' : (scope.row.severityLevel === 'medium' ? '二级' : '三级') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="processor" label="负责人" width="100" />
        <el-table-column prop="isProcessed" label="处理状态" width="120">
          <template #default="scope">
            <el-tag
              :type="scope.row.isProcessed ? 'success' : 'info'"
            >
              {{ scope.row.isProcessed ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertContent" label="事件描述" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="viewEventDetails(scope.row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="processEvent(scope.row)">处理</el-button>
            <el-button link type="danger" size="small" @click="closeEvent(scope.row)">关闭</el-button>
            <el-button link type="success" size="small" @click="generateEventReport(scope.row)">生成报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination" v-if="activeTab === 'response' || activeTab === 'resources'">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalEvents"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 事件详情弹窗 -->
    <el-dialog v-model="eventDetailDialogVisible" title="事件详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="事件ID">{{ currentEventDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ currentEventDetail.eventType }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ currentEventDetail.occurTime }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ currentEventDetail.emergencyLevel }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentEventDetail.processor || currentEventDetail.handler }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ currentEventDetail.processingStatus }}</el-descriptions-item>
        <el-descriptions-item label="事件描述">{{ currentEventDetail.description }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="eventDetailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 联系相关人员对话框 -->
    <el-dialog v-model="contactDialogVisible" title="联系相关人员" width="500px">
      <el-form>
        <el-form-item label="选择人员">
          <el-select v-model="selectedPersonnel" multiple placeholder="请选择要通知的人员">
            <el-option v-for="person in personnelList" :key="person.id" :label="person.name" :value="person.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="notificationMessage" type="textarea" :rows="4" placeholder="请输入通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendNotification">发送</el-button>
      </template>
    </el-dialog>

    <!-- 应急设置对话框 -->
    <el-dialog v-model="settingsDialogVisible" title="应急设置" width="600px">
      <el-form :model="emergencySettings" label-width="120px">
        <el-form-item label="自动启动响应">
          <el-switch v-model="emergencySettings.autoStartResponse" />
        </el-form-item>
        <el-form-item label="默认通知人员">
          <el-select v-model="emergencySettings.defaultPersonnel" multiple placeholder="选择默认通知人员">
            <el-option v-for="person in personnelList" :key="person.id" :label="person.name" :value="person.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告模板">
          <el-select v-model="emergencySettings.reportTemplate" placeholder="选择报告模板">
            <el-option label="标准模板" value="standard" />
            <el-option label="详细模板" value="detailed" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import apiClient from '../utils/api'
import { useWebSocket } from '../hooks/useWebSocket'


interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
}


interface EmergencyEvent {
  id: number; 
  alertType: string; 
  alertTime: string; 
  severityLevel: string; 
  processor: string; 
  isProcessed: boolean; 
  alertContent?: string; 
  triggerReason?: string; 
  processTime?: string; 
}

interface EmergencyEventDetail {
  id: number;
  eventId: string;
  eventType: string;
  occurTime: string;
  emergencyLevel: string;
  handler: string;
  processor?: string;
  processingStatus: string;
  description: string;
}

interface EmergencyStats {
  emergencyEvents: number;
  processingEvents: number;
  completedEvents: number;
  avgResponseTime: string;
}

interface Message {
  id: string;
  title: string;
  type: 'emergency' | 'anomaly' | 'system';
  content: string;
  timestamp: string;
  isRead: boolean;
  relatedData?: any;
}


const emergencySettings = ref({
  autoStartResponse: false,
  defaultPersonnel: [],
  reportTemplate: 'standard'
});


const activeTab = ref('response');
const activeStep = ref(0);
const eventType = ref('');
const emergencyLevel = ref('');
const processingStatus = ref('');
const handler = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const totalEvents = ref(0);

const eventList = ref<EmergencyEvent[]>([]);
const emergencyStats = ref<EmergencyStats>({
  emergencyEvents: 0,
  processingEvents: 0,
  completedEvents: 0,
  avgResponseTime: '0min'
});
const loading = ref(false);
const statsLoading = ref(false);
const isUnmounted = ref(false);


const eventDetailDialogVisible = ref(false);
const currentEventDetail = ref<EmergencyEventDetail>({
  id: 0,
  eventId: '',
  eventType: '',
  occurTime: '',
  emergencyLevel: '',
  handler: '',
  processingStatus: '',
  description: ''
});


const contactDialogVisible = ref(false);
const personnelList = ref<Array<{id: number, name: string, username?: string}>>([]);
const selectedPersonnel = ref([]);
const notificationMessage = ref('请立即处理紧急电力事件！');


const eventTypeOptions = ref<Array<{value: string, label: string}>>([]);
const emergencyLevelOptions = ref<Array<{value: string, label: string}>>([]);
const processingStatusOptions = ref<Array<{value: string, label: string}>>([]);


const settingsDialogVisible = ref(false);


const messages = ref<Message[]>([]);
const messageFilter = ref('all');
const messageCurrentPage = ref(1);
const messagePageSize = ref(10);
const messageDialogVisible = ref(false);
const selectedMessage = ref<Message | null>(null);
const messagesLoading = ref(false);

const currentUsername = localStorage.getItem('username') || 'system';
const { connect, disconnect, onMessage } = useWebSocket({
  subscribeDestinations: [`/user/${currentUsername}/queue/user-alerts`]
});


const startEmergencyResponse = () => {
  ElMessageBox.confirm('确定要启动应急响应吗？这将触发一系列自动化流程。', '确认启动', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      
      
      const response = await apiClient.post('emergency/start-response', { 
        startTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
        status: 'active'
      }); 
      
      // 由于apiClient的响应拦截器已处理，直接检查response对象的success属性
      if (response && response.success) {
        ElMessage.success('应急响应已成功启动！');
        activeStep.value = 1;
      } else {
        ElMessage.error('启动应急响应失败');
      }
    } catch (error) {
      console.error('启动应急响应失败:', error);
      ElMessage.error('启动应急响应失败');
    }
  }).catch(() => {
    ElMessage.info('已取消启动');
  });
};

const contactPersonnel = () => {
  contactDialogVisible.value = true;
};

const sendNotification = async () => {
  try {
    const response = await apiClient.post('emergency/contact-personnel', {
      personnel: selectedPersonnel.value,
      message: notificationMessage.value,
      sendTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
    });
    
    // 由于apiClient的响应拦截器已处理，直接检查response对象的success属性
    if (response && response.success) {
      ElMessage.success('通知已发送！');
      contactDialogVisible.value = false;
      activeStep.value = 2;
    } else {
      ElMessage.error('发送通知失败');
    }
  } catch (error) {
    console.error('发送通知失败:', error);
    ElMessage.error('发送通知失败');
  }
};

const generateEventReport = async (event: EmergencyEvent) => {
  try {
    await ElMessageBox.confirm(
      `确定要为事件ID ${event.id} 生成报告吗？`,
      '确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    
    
    await generateReport(event.id.toString());
  } catch (error) {
    
    if (error !== 'cancel') {
      console.error('生成事件报告失败:', error);
      ElMessage.error('生成事件报告失败');
    }
  }
};

const generateReport = async (event?: Event | string) => {
  try {
    
    let eventIdStr = 'none';
    
    
    if (event && !(event instanceof PointerEvent) && typeof event === 'string') {
      eventIdStr = event;
    }
    
    const response = await apiClient.post('/emergency/generate-report', { eventId: eventIdStr });
    if (response && response.success) {
      ElMessage.success('报告已生成');
      
      const reportUrl = response.reportUrl;
      const reportId = response.reportId;
      
      if (reportUrl && reportId) {
        
        try {
          
          
          const token = localStorage.getItem('token');
          const headers: Record<string, string> = {};
          if (token) {
            headers['Authorization'] = `Bearer ${token}`;
          }
          
          
          const downloadResponse = await fetch(reportUrl.startsWith('http') ? reportUrl : `${window.location.origin}${reportUrl}`, { 
            method: 'GET',
            headers: headers
          });
          
          if (!downloadResponse.ok) {
            throw new Error(`HTTP error! status: ${downloadResponse.status}`);
          }
          
          const blob = await downloadResponse.blob();
          
          
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          
          const urlObj = new URL(reportUrl, window.location.origin);
          const searchParams = new URLSearchParams(urlObj.search);
          const reportIdParam = searchParams.get('reportId') || reportId;
          
          
          const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
          link.setAttribute('download', `EmergencyReport_${timestamp}.txt`);
          document.body.appendChild(link);
          link.click();
          link.remove();
          window.URL.revokeObjectURL(url);
        } catch (downloadError) {
          console.error('下载报告失败:', downloadError);
          ElMessage.error('下载报告失败');
        }
      } else {
        ElMessage.warning('报告生成成功，但无法获取下载链接');
      }
    } else {
      ElMessage.error('生成处理报告失败');
    }
  } catch (error) {
    console.error('生成处理报告失败:', error);
    ElMessage.error('生成处理报告失败');
  }
};


const openSettings = async () => {
  try {
    
    const response = await apiClient.get('/emergency/settings');
    if (response && response.data) {
      emergencySettings.value = {
        autoStartResponse: response.data.autoStartResponse || false,
        defaultPersonnel: response.data.defaultPersonnel || [],
        reportTemplate: response.data.reportTemplate || 'standard'
      };
    } else {
      
      emergencySettings.value = {
        autoStartResponse: response.autoStartResponse || false,
        defaultPersonnel: response.defaultPersonnel || [],
        reportTemplate: response.reportTemplate || 'standard'
      };
    }
  } catch (error) {
    console.error('加载设置失败:', error);
    
    emergencySettings.value = {
      autoStartResponse: false,
      defaultPersonnel: [],
      reportTemplate: 'standard'
    };
  } finally {
    settingsDialogVisible.value = true;
  }
};

const saveSettings = async () => {
  try {
    
    // 移除泛型参数，直接使用响应拦截器处理后的响应对象
    const response = await apiClient.post('/emergency/save-settings', emergencySettings.value);
    
    if (response && response.success) {
      ElMessage.success('设置已保存！');
      settingsDialogVisible.value = false;
    } else {
      ElMessage.error('保存设置失败');
    }
  } catch (error) {
    console.error('保存设置时出错:', error);
    ElMessage.error('保存设置失败');
  }
};






const getEmergencyStats = async () => {
  try {
    if (isUnmounted.value) return;
    statsLoading.value = true;
    
    const response = await apiClient.get('alert-stats/stats') as any;
    emergencyStats.value = {
      emergencyEvents: response.highRisk || 0,
      processingEvents: response.unprocessed || 0,
      completedEvents: response.processedToday || 0,
      avgResponseTime: response.avgResponseTimeMinutes ? `${response.avgResponseTimeMinutes}min` : '15min'
    };
  } catch (error) {
    console.error('获取应急统计数据失败:', error);
    ElMessage.error('获取应急统计数据失败');
  } finally {
    if (!isUnmounted.value) {
      statsLoading.value = false;
    }
  }
};






const getStatusColor = (statusName: string) => {
  const colorMap: Record<string, string> = {
    '已关闭': '#909399',
    '处理中': '#e6a23c',
    '已完成': '#67c23a',
    '未处理': '#409eff'
  };
  return colorMap[statusName] || '#909399';
};


const loadPersonnelList = async () => {
  try {
    const response = await apiClient.get('/system-users', {
      params: {
        activeOnly: true  
      }
    }) as any;
    if (response && Array.isArray(response)) {
      
      personnelList.value = response.map((user: any) => ({
        id: user.id,
        name: user.username, 
        username: user.username
      }));
    } else {
      console.error('获取管理员列表失败：响应格式错误', response);
      
      personnelList.value = [
        { id: 1, name: 'admin' },
        { id: 2, name: 'manager1' },
        { id: 3, name: 'manager2' }
      ];
    }
  } catch (error) {
    console.error('获取管理员列表失败:', error);
    ElMessage.error('获取管理员列表失败，使用默认列表');
    
    personnelList.value = [
      { id: 1, name: 'admin' },
      { id: 2, name: 'manager1' },
      { id: 3, name: 'manager2' }
    ];
  }
};


const loadEventTypeOptions = async () => {
  try {
    const response = await apiClient.get('/emergency/event-types') as any;
    if (response && Array.isArray(response)) {
      eventTypeOptions.value = response;
    } else {
      console.error('获取事件类型选项失败：响应格式错误', response);
      
      eventTypeOptions.value = [
        { value: 'power outage', label: '电力中断' },
        { value: 'equipment failure', label: '设备故障' },
        { value: 'line issue', label: '线路问题' },
        { value: 'other', label: '其他' }
      ];
    }
  } catch (error) {
    console.error('获取事件类型选项失败:', error);
    ElMessage.error('获取事件类型选项失败，使用默认列表');
    
    eventTypeOptions.value = [
      { value: 'power outage', label: '电力中断' },
      { value: 'equipment failure', label: '设备故障' },
      { value: 'line issue', label: '线路问题' },
      { value: 'other', label: '其他' }
    ];
  }
};


const loadEmergencyLevelOptions = async () => {
  try {
    const response = await apiClient.get('/emergency/emergency-levels') as any;
    if (response && Array.isArray(response)) {
      emergencyLevelOptions.value = response;
    } else {
      console.error('获取紧急程度选项失败：响应格式错误', response);
      
      emergencyLevelOptions.value = [
        { value: 'high', label: '一级' },
        { value: 'medium', label: '二级' },
        { value: 'low', label: '三级' }
      ];
    }
  } catch (error) {
    console.error('获取紧急程度选项失败:', error);
    ElMessage.error('获取紧急程度选项失败，使用默认列表');
    
    emergencyLevelOptions.value = [
      { value: 'high', label: '一级' },
      { value: 'medium', label: '二级' },
      { value: 'low', label: '三级' }
    ];
  }
};


const loadProcessingStatusOptions = async () => {
  try {
    const response = await apiClient.get('/emergency/processing-statuses') as any;
    if (response && Array.isArray(response)) {
      processingStatusOptions.value = response;
    } else {
      console.error('获取处理状态选项失败：响应格式错误', response);
      
      processingStatusOptions.value = [
        { value: 'pending', label: '未处理' },
        { value: 'processing', label: '处理中' },
        { value: 'completed', label: '已完成' },
        { value: 'closed', label: '已关闭' }
      ];
    }
  } catch (error) {
    console.error('获取处理状态选项失败:', error);
    ElMessage.error('获取处理状态选项失败，使用默认列表');
    
    processingStatusOptions.value = [
      { value: 'pending', label: '未处理' },
      { value: 'processing', label: '处理中' },
      { value: 'completed', label: '已完成' },
      { value: 'closed', label: '已关闭' }
    ];
  }
};


const handleTabChange = async (tabName: string) => {
  if (isUnmounted.value) return;
  activeTab.value = tabName;
  
  
  if (tabName === 'response') {
    await nextTick();
    await getEmergencyStats();
  }
};


const searchEvents = () => {
  if (isUnmounted.value) return;
  console.log('查询按钮被点击，当前筛选条件:', {
    eventType: eventType.value,
    emergencyLevel: emergencyLevel.value,
    processingStatus: processingStatus.value,
    handler: handler.value
  });
  currentPage.value = 1;
  loadEventData();
};


const resetFilters = () => {
  if (isUnmounted.value) return;
  console.log('重置筛选条件');
  eventType.value = '';
  emergencyLevel.value = '';
  processingStatus.value = '';
  handler.value = '';
  currentPage.value = 1;
  loadEventData();
};


const loadEventData = async () => {
  try {
    if (isUnmounted.value) return;
    loading.value = true;
    
    
    const params = {
      anomalyType: eventType.value === 'power outage' ? '电力中断' : 
                   eventType.value === 'equipment failure' ? '设备故障' :
                   eventType.value === 'line issue' ? '线路问题' :
                   eventType.value === 'other' ? '其他' : eventType.value,
      severityLevel: emergencyLevel.value,  
      processStatus: processingStatus.value,  
      page: currentPage.value,
      pageSize: pageSize.value
    };
    
    
    Object.keys(params).forEach(key => {
      if (!params[key]) {
        delete params[key];
      }
    });
    
    console.log('查询参数:', params);
    
    const response = await apiClient.get('emergency/events', { params }) as any;
    console.log('查询响应:', response);
    
    const items = response?.items || [];
    eventList.value = items.map((it: any) => ({
      id: it.id,
      alertType: it.eventType || it.alertType || '未知',
      alertTime: it.occurTime || it.alertTime || '',
      severityLevel: it.emergencyLevel || it.severityLevel || 'low',
      processor: it.handler || it.processor || it.userName || '',
      isProcessed: it.isProcessed || it.processingStatus === 'completed',
      alertContent: it.description || it.alertContent || ''
    }));
    totalEvents.value = response?.total || items.length || 0;
    
  } catch (error) {
    console.error('获取事件数据失败:', error);
    ElMessage.error('获取事件数据失败: ' + (error.message || '未知错误'));
    eventList.value = [];
    totalEvents.value = 0;
  } finally {
    if (!isUnmounted.value) {
      loading.value = false;
    }
  }
};


const handleSizeChange = (size: number) => {
  if (isUnmounted.value) return;
  pageSize.value = size;
  loadEventData();
};

const handleCurrentChange = (current: number) => {
  if (isUnmounted.value) return;
  currentPage.value = current;
  loadEventData();
};


const viewEventDetails = async (event: EmergencyEvent) => {
  try {
    if (isUnmounted.value) return;
    
    const response = await apiClient.get(`emergency/events/${event.id}`);
    
    if (response) {
      
      currentEventDetail.value = {
        ...response,
        processor: response.processor || response.handler || ''
      };
      eventDetailDialogVisible.value = true;
    }
  } catch (error) {
    console.error('获取事件详情失败:', error);
    ElMessage.error('获取事件详情失败');
  }
};


const processEvent = async (event: EmergencyEvent) => {
  try {
    if (isUnmounted.value) return;
    
    
    if (event.isProcessed) {
      ElMessage.warning(`事件${event.id}已经处理过了`);
      return;
    }
    
    
    const currentUsername = localStorage.getItem('username') || '系统管理员';
    
    const processRecord = {
      processor: currentUsername,
      result: '开始处理此事件'
    };
    
    await apiClient.post(`emergency/events/${event.id}/process`, processRecord);
    await loadEventData();
    ElMessage.success(`开始处理事件${event.id}`);
  } catch (error: any) {
    if (error.response && error.response.status === 500 && error.response.data && error.response.data.error) {
      
      ElMessage.error(`处理事件失败: ${error.response.data.error}`);
    } else {
      ElMessage.error('处理事件失败');
    }
  }
};


const closeEvent = async (event: EmergencyEvent) => {
  try {
    if (isUnmounted.value) return;
    await ElMessageBox.confirm('确定要关闭这个事件吗？', '确认关闭', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    
    if (event.isProcessed) {
      ElMessage.warning(`事件${event.id}已经处理过了`);
      return;
    }
    
    
    const currentUsername = localStorage.getItem('username') || '系统管理员';
    
    const closeRecord = {
      processor: currentUsername,
      result: '事件已处理完成，关闭告警'
    };
    
    await apiClient.post(`emergency/events/${event.id}/close`, closeRecord);
    await loadEventData();
    ElMessage.success(`事件${event.id}已关闭`);
  } catch (error: any) {
    if (error === 'cancel') {
      ElMessage.info('已取消关闭');
    } else if (error.response && error.response.status === 500 && error.response.data && error.response.data.error) {
      
      ElMessage.error(`关闭事件失败: ${error.response.data.error}`);
    } else {
      ElMessage.error('关闭事件失败');
    }
  }
};





const filteredMessages = computed(() => {
  let result = [...messages.value];
  
  if (messageFilter.value !== 'all') {
    result = result.filter(msg => msg.type === messageFilter.value);
  }
  
  result.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  
  return result;
});

const formatMessageDateTime = (dateString?: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date);
};

const getMessageTagType = (type?: string) => {
  switch (type) {
    case 'emergency':
      return 'danger';
    case 'anomaly':
      return 'warning';
    case 'system':
      return 'info';
    default:
      return '';
  }
};

const getMessageTagText = (type?: string) => {
  switch (type) {
    case 'emergency':
      return '应急消息';
    case 'anomaly':
      return '异常用电';
    case 'system':
      return '系统通知';
    default:
      return '其他';
  }
};

const markMessageAsRead = async (id: string) => {
  const message = messages.value.find(msg => msg.id === id);
  if (!message) return;
  try {
    if (message.type === 'system') {
      await apiClient.post(`notifications/${Number(id)}/read`);
    } else if (message.type === 'anomaly') {
      await apiClient.post(`alerts/${Number(id)}/read`);
    } else if (message.type === 'emergency') {
      await apiClient.post(`alerts/${Number(id)}/read`);
    }
    message.isRead = true;
    ElMessage.success('已标记为已读');
  } catch (error: any) {
    console.error('标记已读失败:', error);
    ElMessage.error('标记已读失败');
  }
};

const handleMessageRowClick = (row: Message) => {
  selectedMessage.value = { ...row };
  messageDialogVisible.value = true;
  
  if (!row.isRead) {
    markMessageAsRead(row.id);
  }
};

const onMessageDialogOpen = () => {
  
};

const fetchMessages = async () => {
  messagesLoading.value = true;
  try {
    const currentUsername = localStorage.getItem('username');
    
    const response = await apiClient.get('alerts', { params: { username: currentUsername } });
    const alerts = Array.isArray(response) ? response : (response?.content || []);
    const mapped: Message[] = (alerts || []).map((a: any) => {
      const id = (a.id ?? a.alertId ?? '').toString();
      const alertType = (a.alertType ?? a.anomalyType ?? 'system').toString().toLowerCase();
      const severity = (a.severityLevel ?? '').toString();
      const ts = a.alertTime ?? a.timestamp ?? a.detectionTime ?? new Date().toISOString();
      const userName = a.user?.name ?? a.username ?? '';
      const title = userName ? `${userName} - ${alertType}` : `${alertType}`;
      const content = a.description || a.anomalyDetails || `严重程度: ${severity}`;
      const isRead = Boolean(a.isRead ?? false);
      const type: Message['type'] = (severity === 'CRITICAL' || severity === 'HIGH') ? 'emergency'
        : (alertType.includes('system') ? 'system' : 'anomaly');
      return {
        id,
        title,
        type,
        content,
        timestamp: typeof ts === 'string' ? ts : new Date(ts).toISOString(),
        isRead,
        relatedData: a
      };
    });
    
    let notifMapped: Message[] = [];
    try {
      const page = await apiClient.get('notifications', { 
        params: { 
          page: 0, 
          size: 50,
          recipient: currentUsername 
        } 
      }) as any;
      const notifs = Array.isArray(page?.content) ? page.content : [];
      notifMapped = notifs.map((n: any) => ({
        id: (n.id ?? '').toString(),
        title: n.title || '系统通知',
        type: 'system',
        content: n.content || '',
        timestamp: n.createdAt || new Date().toISOString(),
        isRead: Boolean(n.isRead)
      }));
    } catch (notificationError: any) {
      console.error('获取系统通知失败:', notificationError);
      const isForbidden = notificationError?.response?.status === 403;
      if (isForbidden) {
        ElMessage.warning('您的权限级别无法查看系统通知');
      } else {
        ElMessage.error('获取系统通知失败');
      }
    }

    messages.value = [...notifMapped, ...mapped];
    
    if (messages.value.length === 0) {
      ElMessage.warning('未获取到任何消息数据');
    }
  } catch (error) {
    console.error('获取消息列表失败:', error);
    ElMessage.error('获取消息列表失败，请检查网络连接或联系管理员');
    messages.value = []; 
  } finally {
    messagesLoading.value = false;
  }
};


onMounted(async () => {
  
  await Promise.all([
    loadEventData(),
    getEmergencyStats(),
    loadPersonnelList(), 
    loadEventTypeOptions(), 
    loadEmergencyLevelOptions(), 
    loadProcessingStatusOptions(),
    fetchMessages()
  ]);
  

  
  
  
  onMessage((data) => {
    console.log('应急处理页面收到WebSocket消息:', data);
    
    
    try {
      let newMessage: Message;
      
      if (data.type === 'anomaly_alert') {
        newMessage = {
          id: data.id || data.alertId || Date.now().toString(),
          title: `${data.sender || '异常检测系统'} - 异常用电`,
          type: 'anomaly',
          content: data.message || '收到异常用电告警消息',
          timestamp: data.timestamp || new Date().toISOString(),
          isRead: false,
          relatedData: data
        };
      } else if (data.type === 'urgent_alert') {
        newMessage = {
          id: data.alertId || data.id || Date.now().toString(),
          title: `${data.sender || '应急处理系统'} - 紧急告警`,
          type: 'emergency',
          content: data.message || '收到紧急告警消息',
          timestamp: data.timestamp || new Date().toISOString(),
          isRead: false,
          relatedData: data
        };
      } else if (data.type === 'system_notification') {
        newMessage = {
          id: Date.now().toString(),
          title: '系统通知',
          type: 'system',
          content: data.message || data.content || '收到系统通知',
          timestamp: data.timestamp || new Date().toISOString(),
          isRead: false,
          relatedData: data
        };
      } else if (data.type === 'alert_status_update') {
        console.log('收到告警状态更新:', data);
        return; 
      } else {
        newMessage = {
          id: data.id || data.alertId || data.notificationId || Date.now().toString(),
          title: data.title || data.message || '新消息',
          type: (data.severityLevel === 'CRITICAL' || data.severityLevel === 'HIGH') ? 'emergency' 
                : (data.type === 'system' || data.notificationType === 'system') ? 'system' : 'anomaly',
          content: data.content || data.message || data.description || '收到新消息',
          timestamp: data.timestamp || data.alertTime || data.detectionTime || data.createdAt || new Date().toISOString(),
          isRead: false,
          relatedData: data
        };
      }
      
      messages.value.unshift(newMessage);
      ElMessage.success(`收到新消息: ${newMessage.title}`);
    } catch (error) {
      console.error('Error processing WebSocket message:', error);
    }
    
    
    if (data.type === 'anomaly_alert' || data.type === 'urgent_alert') {
      ElMessage.warning({
        title: '新的紧急事件',
        message: data.message || '检测到新的紧急事件，请及时处理',
        duration: 0, 
        showClose: true
      });
      
      loadEventData();
      getEmergencyStats();
    }
  });
  
  connect();
  

});


onUnmounted(() => {
  isUnmounted.value = true;
  disconnect();
});
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
  color: #f56c6c;
}

.emergency-tabs {
  margin-bottom: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 16px;
}

.emergency-process {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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

.filter-select {
  width: 150px;
}

.response-content {
  margin-bottom: 20px;
}

.dashboard-container {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-container h3 {
  margin: 0;
  margin-right: 20px;
  color: #303133;
}

.emergency-btn {
  margin-right: 10px;
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.contact-btn {
  margin-right: 10px;
}

.report-btn {
  margin-right: 10px;
}

.emergency-dashboard {
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

.stat-card.success {
  border-left: 4px solid #67c23a;
}

.stat-card.info {
  border-left: 4px solid #409eff;
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

.status-chart {
  margin-bottom: 20px;
}

.chart-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chart {
  width: 100%;
}

.event-list {
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
</style>