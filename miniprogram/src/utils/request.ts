/**
 * 网络请求工具类
 * uni.request
 */
  
/// <reference types="@dcloudio/types" />

// 配置基础URL - 需要根据实际后端地址修改
const BASE_URL = 'http://8.152.214.222:8081/power_manage/api' // 远程服务器
// const BASE_URL = 'http://127.0.0.1:8081/power_manage/api' // 本地开发


const MAX_RETRIES = 3

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: any
  showLoading?: boolean
  loadingText?: string
  /** 设为 true 时，401 不触发全局跳转登录逻辑（用于登录/注册接口） */
  skipAuthRedirect?: boolean
}

// 判断是否需要重试
function shouldRetry(statusCode?: number): boolean {
  if (!statusCode) return true
  
  // 4xx 客户端错误一般不重试（除了 408 和 429）
  if (statusCode >= 400 && statusCode < 500 && ![408, 429].includes(statusCode)) {
    return false
  }
  return [408, 429, 500, 502, 503, 504].includes(statusCode)
}

// 请求拦截器 - 添加 token
function addAuthHeader(header: any = {}) {
  const token = uni.getStorageSync('token')
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  return header
}

// 响应拦截器 - 处理错误
function handleResponse(res: any, url: string, skipAuthRedirect = false) {
  const { statusCode, data } = res
  
  // 401 未授权
  if (statusCode === 401) {
    if (skipAuthRedirect) {
      // 登录/注册接口：直接返回服务端错误消息，不跳转
      const msg = data?.message || data?.error || '账号或密码错误'
      return Promise.reject(new Error(msg))
    }
    // 其他接口：清除 token 并跳转登录页
    uni.removeStorageSync('token')
    uni.removeStorageSync('username')
    uni.removeStorageSync('role')
    uni.removeStorageSync('userId')
    
    uni.showToast({
      title: '登录已过期',
      icon: 'none'
    })
    
    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/login/login'
      })
    }, 1500)
    
    return Promise.reject(new Error('未授权'))
  }
  
  // 403 权限不足
  if (statusCode === 403) {
    uni.showToast({
      title: '权限不足',
      icon: 'none'
    })
    return Promise.reject(new Error('权限不足'))
  }
  
  // 404 资源不存在
  if (statusCode === 404) {
    uni.showToast({
      title: '请求的资源不存在',
      icon: 'none'
    })
    return Promise.reject(new Error('资源不存在'))
  }
  
  // 500 服务器错误
  if (statusCode === 500) {
    uni.showToast({
      title: '服务器内部错误',
      icon: 'none'
    })
    return Promise.reject(new Error('服务器错误'))
  }
  
  // 200-299 成功
  if (statusCode >= 200 && statusCode < 300) {
    return data
  }
  
  return Promise.reject(new Error(`请求失败: ${statusCode}`))
}

// 主请求函数
async function request(options: RequestOptions, retryCount = 0): Promise<any> {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    showLoading = false,
    loadingText = '加载中...',
    skipAuthRedirect = false
  } = options
  
  // 显示加载提示
  if (showLoading) {
    uni.showLoading({
      title: loadingText,
      mask: true
    })
  }
  
  try {
    // 对于 GET 请求，将 data 转换为 URL 查询参数
    let requestUrl = BASE_URL + url
    let requestData = data
    
    if (method === 'GET' && data && Object.keys(data).length > 0) {
      // 手动构建查询参数，避免使用 URLSearchParams（微信小程序不支持）
      const queryParams: string[] = []
      Object.keys(data).forEach(key => {
        if (data[key] !== undefined && data[key] !== null) {
          queryParams.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(data[key]))}`)
        }
      })
      const queryString = queryParams.join('&')
      if (queryString) {
        requestUrl += (url.includes('?') ? '&' : '?') + queryString
      }
      requestData = undefined // GET 请求不需要 body
    }
    
    const res = await uni.request({
      url: requestUrl,
      method,
      data: requestData,
      header: {
        'Content-Type': 'application/json',
        ...addAuthHeader(header)
      },
      timeout: 15000
    })
    
    if (showLoading) {
      uni.hideLoading()
    }
    
    return handleResponse(res, url, skipAuthRedirect)
  } catch (error: any) {
    if (showLoading) {
      uni.hideLoading()
    }
    
    console.error('请求错误:', { url, method, error })
    
    // 重试逻辑
    const statusCode = error?.statusCode
    if (retryCount < MAX_RETRIES && shouldRetry(statusCode)) {
      const backoff = Math.min(1000 * (retryCount + 1), 3000)
      await new Promise(resolve => setTimeout(resolve, backoff))
      console.log(`重试请求 (${retryCount + 1}/${MAX_RETRIES}):`, url)
      return request(options, retryCount + 1)
    }
    
    // 网络错误
    if (!statusCode) {
      uni.showToast({
        title: '网络连接失败',
        icon: 'none'
      })
    }
    
    return Promise.reject(error)
  }
}

// 导出便捷方法
export default {
  get(url: string, data?: any, showLoading = false) {
    return request({ url, method: 'GET', data, showLoading })
  },
  
  post(url: string, data?: any, showLoading = false) {
    return request({ url, method: 'POST', data, showLoading })
  },
  
  put(url: string, data?: any, showLoading = false) {
    return request({ url, method: 'PUT', data, showLoading })
  },
  
  delete(url: string, data?: any, showLoading = false) {
    return request({ url, method: 'DELETE', data, showLoading })
  },
  
  // 自定义请求
  request
}

