import { ref, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElNotification } from 'element-plus';
import { wsClient, WS_STATE, type AlertMessage, type MonitoringMessage, type ConnectionMessage } from '../utils/websocket';

export interface UseWebSocketOptions {
  userId: string;
  autoConnect?: boolean;
  showNotifications?: boolean;
  onAlert?: (message: AlertMessage) => void;
  onMonitoring?: (message: MonitoringMessage) => void;
  onConnection?: (message: ConnectionMessage) => void;
}

export function useWebSocket(options: UseWebSocketOptions) {
  const {
    userId,
    autoConnect = true,
    showNotifications = true,
    onAlert: customOnAlert,
    onMonitoring: customOnMonitoring,
    onConnection: customOnConnection
  } = options;

  
  const isConnected = ref(false);
  const connectionState = ref(wsClient.getConnectionState());
  const onlineUserCount = ref(0);
  const lastAlert = ref<AlertMessage | null>(null);
  const lastMonitoringData = ref<MonitoringMessage | null>(null);

  
  const connect = async () => {
    try {
      await wsClient.connect(userId);
      isConnected.value = true;
      connectionState.value = wsClient.getConnectionState();
    } catch (error) {
      console.error('WebSocket连接失败:', error);
      ElMessage.error('实时连接失败，请检查网络连接');
    }
  };

  
  const disconnect = () => {
    wsClient.disconnect();
    isConnected.value = false;
    connectionState.value = wsClient.getConnectionState();
  };

  
  const handleAlert = (message: AlertMessage) => {
    lastAlert.value = message;
    
    
    if (showNotifications) {
      const notificationType = message.type === 'urgent_alert' ? 'error' : 
                              message.type === 'anomaly_alert' ? 'warning' : 'info';
      
      ElNotification({
        title: getAlertTitle(message),
        message: getAlertMessage(message),
        type: notificationType,
        duration: message.type === 'urgent_alert' ? 0 : 5000, 
        position: 'top-right',
        showClose: true,
        dangerouslyUseHTMLString: true
      });
    }

    
    if (customOnAlert) {
      customOnAlert(message);
    }
  };

  
  const handleMonitoring = (message: MonitoringMessage) => {
    lastMonitoringData.value = message;
    onlineUserCount.value = message.data.onlineUserCount || 0;

    
    if (customOnMonitoring) {
      customOnMonitoring(message);
    }
  };

  
  const handleConnection = (message: ConnectionMessage) => {
    if (message.type === 'connection_established') {
      isConnected.value = true;
      onlineUserCount.value = message.data.onlineUserCount || 0;
      ElMessage.success('实时连接已建立');
    } else if (message.type === 'connection_closed') {
      isConnected.value = false;
      onlineUserCount.value = message.data.onlineUserCount || 0;
      ElMessage.info('实时连接已断开');
    } else if (message.type === 'connection_error') {
      isConnected.value = false;
      ElMessage.error('实时连接错误: ' + (message.data.error || '未知错误'));
    }

    
    if (customOnConnection) {
      customOnConnection(message);
    }
  };

  
  const handleError = (error: Event) => {
    console.error('WebSocket错误:', error);
    isConnected.value = false;
    connectionState.value = wsClient.getConnectionState();
  };

  
  const getAlertTitle = (message: AlertMessage): string => {
    switch (message.type) {
      case 'urgent_alert':
        return '🚨 紧急告警';
      case 'anomaly_alert':
        return '⚠️ 异常告警';
      case 'alert_status_update':
        return '📋 告警状态更新';
      case 'system_notification':
        return '📢 系统通知';
      default:
        return '📨 新消息';
    }
  };

  
  const getAlertMessage = (message: AlertMessage): string => {
    const data = message.data;
    
    switch (message.type) {
      case 'urgent_alert':
      case 'anomaly_alert':
        return `
          <div style="line-height: 1.5;">
            <p><strong>用户:</strong> ${data.userName || '未知用户'}</p>
            <p><strong>类型:</strong> ${data.alertType || '未知类型'}</p>
            <p><strong>严重程度:</strong> ${getSeverityText(data.severityLevel)}</p>
            ${data.notes ? `<p><strong>详情:</strong> ${data.notes}</p>` : ''}
            <p><strong>时间:</strong> ${data.detectionTime || new Date().toLocaleString()}</p>
          </div>
        `;
        
      case 'alert_status_update':
        return `
          <div style="line-height: 1.5;">
            <p><strong>告警ID:</strong> ${data.id || '未知'}</p>
            <p><strong>状态:</strong> ${data.status || '未知'}</p>
            ${data.processResult ? `<p><strong>处理结果:</strong> ${data.processResult}</p>` : ''}
            <p><strong>时间:</strong> ${data.detectionTime || new Date().toLocaleString()}</p>
          </div>
        `;
        
      case 'system_notification':
        return data.message || '系统通知';
        
      default:
        return '收到新消息';
    }
  };

  
  const getSeverityText = (level?: string): string => {
    switch (level) {
      case 'high':
        return '<span style="color: #f56c6c;">高风险</span>';
      case 'medium':
        return '<span style="color: #e6a23c;">中风险</span>';
      case 'low':
        return '<span style="color: #67c23a;">低风险</span>';
      default:
        return '未知';
    }
  };

  
  const acknowledgeAlert = (alertId: string, acknowledgment: string) => {
    wsClient.acknowledgeAlert(alertId, acknowledgment);
  };

  
  watch(connectionState, (newState) => {
    isConnected.value = newState === WS_STATE.OPEN;
  });

  
  onMounted(() => {
    
    wsClient.onAlert(handleAlert);
    wsClient.onMonitoring(handleMonitoring);
    wsClient.onConnection(handleConnection);
    wsClient.onError(handleError);

    
    if (autoConnect) {
      connect();
    }
  });

  
  onUnmounted(() => {
    disconnect();
  });

  return {
    
    isConnected,
    connectionState,
    onlineUserCount,
    lastAlert,
    lastMonitoringData,
    
    
    connect,
    disconnect,
    acknowledgeAlert,
    
    
    getSeverityText
  };
}
