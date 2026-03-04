

import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: string;
}

export interface AlertMessage extends WebSocketMessage {
  type: 'anomaly_alert' | 'urgent_alert' | 'alert_status_update' | 'system_notification';
  data: {
    id?: string;
    userId?: string;
    userName?: string;
    alertType?: string;
    severityLevel?: string;
    anomalyScore?: number;
    detectionTime?: string;
    notes?: string;
    status?: string;
    processResult?: string;
    message?: string;
  };
}

export interface MonitoringMessage extends WebSocketMessage {
  type: 'monitoring_data';
  data: {
    currentUsage: number;
    status: 'normal' | 'warning' | 'critical';
    timestamp: string;
    onlineUserCount: number;
  };
}

export interface ConnectionMessage extends WebSocketMessage {
  type: 'connection_established' | 'connection_closed' | 'connection_error' | 'heartbeat_response';
  data: {
    userId?: string;
    onlineUserCount?: number;
    error?: string;
  };
}

export type AllWebSocketMessage = AlertMessage | MonitoringMessage | ConnectionMessage;

export class WebSocketClient {
  private stompClient: Client | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 6;
  private reconnectInterval = 2000;
  private isConnecting = false;
  private userId: string | null = null;
  
  
  private alertSubscription: StompSubscription | null = null;
  private monitoringSubscription: StompSubscription | null = null;
  private connectionSubscription: StompSubscription | null = null;
  
  
  private onAlertCallback: ((message: AlertMessage) => void) | null = null;
  private onMonitoringCallback: ((message: MonitoringMessage) => void) | null = null;
  private onConnectionCallback: ((message: ConnectionMessage) => void) | null = null;
  private onErrorCallback: ((error: any) => void) | null = null;

  constructor() {
    
    this.handleMessage = this.handleMessage.bind(this);
  }

  
  connect(userId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.isConnecting || (this.stompClient && this.stompClient.connected)) {
        resolve();
        return;
      }

      this.isConnecting = true;
      this.userId = userId;

      try {
        
        
        const token = localStorage.getItem('token');
        
        
        const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const host = window.location.hostname;
        const port = import.meta.env.VITE_WS_PORT || '8080';
        const brokerURL = `${protocol}://${host}:${port}/power_manage/ws`;
        
        this.stompClient = new Client({
          brokerURL: brokerURL,
          reconnectDelay: this.reconnectInterval,
          heartbeatIncoming: 30000,
          heartbeatOutgoing: 30000,
          debug: (str) => {
            console.log(str);
          },
          
          connectHeaders: token ? {
            'Authorization': `Bearer ${token}`
          } : {}
        });

        
        this.stompClient.onConnect = () => {
          console.log('STOMP连接已建立');
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          
          
          this.subscribeToTopics();
          
          
          this.sendMessage({
            type: 'connect',
            userId: userId
          });
          
          resolve();
        };

        
        this.stompClient.onStompError = (frame) => {
          console.error('STOMP错误:', frame);
          this.isConnecting = false;
          
          if (this.onErrorCallback) {
            this.onErrorCallback(frame);
          }
          
          reject(frame);
        };

        
        this.stompClient.onWebSocketError = (event) => {
          console.error('WebSocket错误:', event);
          this.isConnecting = false;
          
          if (this.onErrorCallback) {
            this.onErrorCallback(event);
          }
          
          reject(event);
        };

        
        this.stompClient.onDisconnect = () => {
          console.log('STOMP连接已断开');
          this.isConnecting = false;
          
          
          this.clearSubscriptions();
          
          
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const jitter = Math.floor(Math.random() * 400);
            const delay = Math.min(this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1), 15000) + jitter;
            console.log(`尝试重连STOMP (${this.reconnectAttempts}/${this.maxReconnectAttempts})，${delay}ms 后`);
            
            setTimeout(() => {
              if (this.userId) {
                this.connect(this.userId).catch(error => {
                  console.error('STOMP重连失败:', error);
                });
              }
            }, delay);
          }
        };

        
        this.stompClient.activate();

      } catch (error) {
        this.isConnecting = false;
        reject(error);
      }
    });
  }

  
  private subscribeToTopics(): void {
    if (!this.stompClient || !this.stompClient.connected || !this.userId) {
      return;
    }

    
    this.alertSubscription = this.stompClient.subscribe(`/user/${this.userId}/queue/user-alerts`, this.handleMessage);
    
    
    this.monitoringSubscription = this.stompClient.subscribe(`/topic/monitoring`, this.handleMessage);
    
    
    this.connectionSubscription = this.stompClient.subscribe(`/topic/connection`, this.handleMessage);
  }

  
  private clearSubscriptions(): void {
    if (this.alertSubscription) {
      this.alertSubscription.unsubscribe();
      this.alertSubscription = null;
    }
    
    if (this.monitoringSubscription) {
      this.monitoringSubscription.unsubscribe();
      this.monitoringSubscription = null;
    }
    
    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
      this.connectionSubscription = null;
    }
  }

  
  disconnect(): void {
    if (this.stompClient) {
      
      if (this.userId) {
        this.sendMessage({
          type: 'disconnect',
          userId: this.userId
        });
      }

      
      this.clearSubscriptions();
      
      
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.userId = null;
  }

  
  sendMessage(message: any): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/message',
        body: JSON.stringify(message)
      });
    } else {
      console.warn('STOMP连接未建立，无法发送消息:', message);
    }
  }

  
  private handleMessage(message: any): void {
    try {
      const parsedMessage: AllWebSocketMessage = JSON.parse(message.body);
      
      switch (parsedMessage.type) {
        case 'anomaly_alert':
        case 'urgent_alert':
        case 'alert_status_update':
        case 'system_notification':
          if (this.onAlertCallback) {
            this.onAlertCallback(parsedMessage as AlertMessage);
          }
          break;
          
        case 'monitoring_data':
          if (this.onMonitoringCallback) {
            this.onMonitoringCallback(parsedMessage as MonitoringMessage);
          }
          break;
          
        case 'connection_established':
        case 'connection_closed':
        case 'connection_error':
          if (this.onConnectionCallback) {
            this.onConnectionCallback(parsedMessage as ConnectionMessage);
          }
          break;
          
        case 'heartbeat_response':
          
          break;
          
        default:
          console.log('收到未知类型的STOMP消息:', parsedMessage);
      }
    } catch (error) {
      console.error('解析STOMP消息失败:', error);
    }
  }

  
  onAlert(callback: (message: AlertMessage) => void): void {
    this.onAlertCallback = callback;
  }

  
  onMonitoring(callback: (message: MonitoringMessage) => void): void {
    this.onMonitoringCallback = callback;
  }

  
  onConnection(callback: (message: ConnectionMessage) => void): void {
    this.onConnectionCallback = callback;
  }

  
  onError(callback: (error: any) => void): void {
    this.onErrorCallback = callback;
  }

  
  getConnectionState(): number {
    if (!this.stompClient) {
      return 3; 
    }
    
    if (this.stompClient.connected) {
      return 1; 
    } else if (this.stompClient.active) {
      return 0; 
    } else {
      return 3; 
    }
  }

  
  isConnected(): boolean {
    return this.stompClient ? this.stompClient.connected : false;
  }

  
  acknowledgeAlert(alertId: string, acknowledgment: string): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/acknowledge-alert',
        body: JSON.stringify({
          alertId: alertId,
          userId: this.userId,
          acknowledgment: acknowledgment
        })
      });
    } else {
      console.warn('STOMP连接未建立，无法发送告警确认');
    }
  }
}


export const wsClient = new WebSocketClient();


export const WS_STATE = {
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3
};
