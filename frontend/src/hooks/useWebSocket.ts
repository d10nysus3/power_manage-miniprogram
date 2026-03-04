import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import type { IMessage, StompSubscription } from '@stomp/stompjs';
import { ElMessage } from 'element-plus';

interface UseWebSocketOptions {
  url?: string; 
  onMessage?: (data: any) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: any) => void;
  protocols?: string | string[];
  subscribeDestinations?: string[]; 
  retryAttempts?: number; 
  retryDelay?: number; 
}

interface UseWebSocketReturn {
  connect: () => void;
  disconnect: () => void;
  sendApp: (destination: string, body?: any, headers?: Record<string, string>) => void; 
  isConnected: boolean;
  onMessage: (callback: (data: any) => void) => void;
  
  getConnectionStatus: () => boolean;
}

const messageCallbacks = new Set<(data: any) => void>();

const useWebSocket = (options: UseWebSocketOptions = {}): UseWebSocketReturn => {
  const {
    url = ((): string => {
      const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
      
      const host = window.location.hostname;
      const port = import.meta.env.VITE_WS_PORT || '8080';
      
      
      return `${scheme}://${host}:${port}/power_manage/ws`;
    })(),
    onMessage: onMessageProp,
    onOpen,
    onClose,
    onError,
    
    
    subscribeDestinations = ['/topic/realtime-alerts', '/user/queue/user-alerts'],
    retryAttempts = 5,
    retryDelay = 3000
  } = options;

  const stompClient = ref<Client | null>(null);
  const isConnected = ref(false);
  let subscriptions: StompSubscription[] = [];
  let connectionAttempts = 0;

  
  const addMessageCallback = (callback: (data: any) => void) => {
    messageCallbacks.add(callback);
  };

  
  const triggerMessageCallbacks = (data: any) => {
    messageCallbacks.forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error('Error in WebSocket message callback:', error);
      }
    });
  };

  
  const connect = () => {
    try {
      connectionAttempts = 0;
      
      const token = (typeof window !== 'undefined') ? localStorage.getItem('token') : null;
      
      
      const client = new Client({
        brokerURL: url,
        reconnectDelay: retryDelay,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        
        logRawCommunication: false,
        
        
        
        connectHeaders: token ? {
          Authorization: `Bearer ${token}`
        } : {},
        onConnect: () => {
          isConnected.value = true;
          connectionAttempts = 0;
          console.log('WebSocket连接成功');
          
          subscriptions = subscribeDestinations.map(dest =>
            client.subscribe(dest, (message: IMessage) => {
              try {
                const payload = message.body ? JSON.parse(message.body) : {};
                triggerMessageCallbacks(payload);
                if (onMessageProp) onMessageProp(payload);
              } catch (error) {
                
                console.warn('无法解析WebSocket消息:', error);
                triggerMessageCallbacks(message.body);
                if (onMessageProp) onMessageProp(message.body);
              }
            })
          );
          if (onOpen) onOpen();
        },
        onStompError: (frame) => {
          console.error('STOMP错误:', frame);
          if (onError) onError(frame);
        },
        onWebSocketClose: (event) => {
          isConnected.value = false;
          console.log('WebSocket连接关闭:', event.code, event.reason);
          if (onClose) onClose();
        },
        onWebSocketError: (event) => {
          connectionAttempts++;
          console.error(`WebSocket连接错误 (尝试${connectionAttempts}/${retryAttempts}):`, event);
          
          
          if (connectionAttempts === 1) {
            
            ElMessage.warning('实时数据连接失败，将尝试重新连接');
          }
          
          
          if (connectionAttempts >= retryAttempts) {
            console.error(`已达到最大重试次数(${retryAttempts})，停止自动重连`);
            client.reconnectDelay = 0; 
          }
          
          if (onError) onError(event);
        }
      });
      
      stompClient.value = client;
      client.activate();
    } catch (error) {
      console.error('WebSocket连接初始化失败:', error);
      ElMessage.error('实时数据服务连接异常');
      if (onError) onError(error);
    }
  };

  
  const disconnect = () => {
    try {
      subscriptions.forEach(s => s.unsubscribe());
      subscriptions = [];
      if (stompClient.value) {
        stompClient.value.deactivate();
        stompClient.value = null;
      }
      isConnected.value = false;
    } catch (e) {
      console.error('Failed to disconnect STOMP', e);
    }
  };

  
  const sendApp = (destination: string, body?: any, headers?: Record<string, string>) => {
    if (!stompClient.value || !isConnected.value) {
      console.warn('STOMP is not connected');
      return;
    }
    const payload = body != null ? (typeof body === 'string' ? body : JSON.stringify(body)) : '';
    stompClient.value.publish({ destination, body: payload, headers: headers || {} });
  };

  
  const onMessage = (callback: (data: any) => void) => {
    addMessageCallback(callback);
  };

  return {
    connect,
    disconnect,
    sendApp,
    isConnected: isConnected.value,
    onMessage,
    
    getConnectionStatus: () => isConnected.value
  };
};

export { useWebSocket };