/**
 * WebSocket 工具类
 * 用于实时数据推送
 */

/// <reference types="@dcloudio/types" />

interface WebSocketOptions {
  url: string
  onMessage?: (data: any) => void
  onOpen?: () => void
  onClose?: () => void
  onError?: (error: any) => void
  reconnect?: boolean
  reconnectInterval?: number
  maxReconnectAttempts?: number
}

class WebSocketClient {
  private socketTask: UniApp.SocketTask | null = null
  private options: WebSocketOptions
  private reconnectAttempts = 0
  private reconnectTimer: number | null = null
  private isManualClose = false

  constructor(options: WebSocketOptions) {
    this.options = {
      reconnect: true,
      reconnectInterval: 3000,
      maxReconnectAttempts: 5,
      ...options
    }
  }

  // 连接 WebSocket
  connect() {
    const token = uni.getStorageSync('token')
    const wsUrl = `${this.options.url}?token=${token}`

    this.socketTask = uni.connectSocket({
      url: wsUrl,
      success: () => {
        console.log('WebSocket 连接中...')
      },
      fail: (error) => {
        console.error('WebSocket 连接失败:', error)
        this.handleReconnect()
      }
    })

    // 监听打开
    this.socketTask.onOpen(() => {
      console.log('WebSocket 已连接')
      this.reconnectAttempts = 0
      this.options.onOpen?.()
    })

    // 监听消息
    this.socketTask.onMessage((res) => {
      try {
        const data = JSON.parse(res.data as string)
        this.options.onMessage?.(data)
      } catch (error) {
        console.error('解析 WebSocket 消息失败:', error)
      }
    })

    // 监听关闭
    this.socketTask.onClose(() => {
      console.log('WebSocket 已关闭')
      this.options.onClose?.()
      
      if (!this.isManualClose && this.options.reconnect) {
        this.handleReconnect()
      }
    })

    // 监听错误
    this.socketTask.onError((error) => {
      console.error('WebSocket 错误:', error)
      this.options.onError?.(error)
    })
  }

  // 发送消息
  send(data: any) {
    if (this.socketTask) {
      this.socketTask.send({
        data: JSON.stringify(data),
        success: () => {
          console.log('消息发送成功')
        },
        fail: (error) => {
          console.error('消息发送失败:', error)
        }
      })
    } else {
      console.warn('WebSocket 未连接')
    }
  }

  // 关闭连接
  close() {
    this.isManualClose = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socketTask) {
      this.socketTask.close({
        success: () => {
          console.log('WebSocket 连接已关闭')
        }
      })
      this.socketTask = null
    }
  }

  // 处理重连
  private handleReconnect() {
    if (
      !this.options.reconnect ||
      this.reconnectAttempts >= (this.options.maxReconnectAttempts || 5)
    ) {
      console.log('达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    console.log(`尝试重连 (${this.reconnectAttempts}/${this.options.maxReconnectAttempts})`)

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.options.reconnectInterval) as unknown as number
  }
}

export default WebSocketClient

