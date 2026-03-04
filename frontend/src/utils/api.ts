import axios from 'axios';
import { ElMessage } from 'element-plus';


const MAX_RETRIES = 3;
function shouldRetry(status?: number) {
  if (!status) return true;
  
  
  if (status >= 400 && status < 500 && ![408, 429].includes(status)) {
    return false;
  }
  return [408, 429, 500, 502, 503, 504].includes(status);
}


const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 用于调试的API调用辅助函数
export const debugApiCall = async (method: 'get' | 'post', url: string, data?: any) => {
  try {
    console.log(`API调用调试 - 方法: ${method}, URL: ${url}, 数据:`, data);
    
    // 构建完整URL以便调试，不影响实际API调用
    const fullUrl = apiClient.defaults.baseURL + (url.startsWith('/') ? '' : '/') + url;
    console.log(`完整URL: ${fullUrl}`);
    
    let response;
    if (method === 'post') {
      response = await apiClient.post(url, data);
    } else {
      response = await apiClient.get(url);
    }
    
    console.log('API响应:', response);
    return response;
  } catch (error) {
    console.error('API调用错误:', error);
    throw error;
  }
};


apiClient.interceptors.request.use(
  (config) => {
    
    // 同时检查localStorage和sessionStorage中的token
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (token) {
        (config.headers as any).Authorization = `Bearer ${token}`;
      }
    }
    // 规范化路径，确保不会与baseURL重复
    if (config.url && config.url.startsWith('/api/')) {
      config.url = config.url.slice(4); // 移除'/api/'前缀
    }
    return config;
  },
  (error) => {
    console.error('请求配置错误:', error);
    return Promise.reject(error);
  }
);


apiClient.interceptors.response.use(
  (response) => {
    
    if (response.config.responseType === 'blob') {
      return response;
    }
    return response.data;
  },
  async (error) => {
    const config: any = error.config || {};
    const status = error?.response?.status;
    const url = config.url || 'unknown URL';
    
    
    if (status === 401) {
      // 同时清除localStorage和sessionStorage中的认证信息
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');
        localStorage.removeItem('username');
        sessionStorage.removeItem('username');
        localStorage.removeItem('role');
        sessionStorage.removeItem('role');
        localStorage.removeItem('userId');
        sessionStorage.removeItem('userId');
        
        if (window.location.pathname !== '/login') {
          ElMessage.warning('登录已过期，请重新登录');
          window.location.href = '/login';
        }
      }
      return Promise.reject(error);
    }
    
    
    if (status === 403) {
      console.error('API访问被拒绝 (403 Forbidden):', url);
      
      if (url.includes('dashboard/system-status') || url.includes('notifications')) {
        ElMessage.warning('当前用户权限不足，无法访问此功能');
      }
    }
    
    
    config.__retryCount = config.__retryCount || 0;
    if (config.__retryCount < MAX_RETRIES && shouldRetry(status)) {
      config.__retryCount += 1;
      const backoff = Math.min(1000 * config.__retryCount, 3000);
      await new Promise(res => setTimeout(res, backoff));
      console.log(`重试API请求 (${config.__retryCount}/${MAX_RETRIES}):`, url);
      return apiClient(config);
    }
    
    
    console.error('API请求错误:', {
      url: url,
      method: config.method,
      status: status,
      message: error.message,
      response: error.response?.data
    });
    
    
    if (status === 500) {
      ElMessage.error('服务器内部错误，请稍后再试');
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在');
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
