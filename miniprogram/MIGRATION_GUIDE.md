# Web 版本到小程序版本改造对照表

## 一、核心差异对比

| 功能 | Web 版 (Vue 3) | 小程序版 (uni-app) |
|------|----------------|-------------------|
| 路由 | Vue Router | pages.json + uni API |
| UI 框架 | Element Plus | 原生组件 + uni-ui |
| 网络请求 | axios | uni.request |
| 本地存储 | localStorage | uni.storage |
| WebSocket | 原生 WebSocket | uni.connectSocket |
| 图表 | ECharts | lime-echart / uCharts |

## 二、详细改造指南

### 1. 路由系统改造

#### Web 版 (router.ts)
```typescript
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Dashboard },
    { path: '/login', component: Login }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else {
    next()
  }
})
```

#### 小程序版 (pages.json)
```json
{
  "pages": [
    {
      "path": "pages/dashboard/dashboard",
      "style": { "navigationBarTitleText": "首页" }
    },
    {
      "path": "pages/login/login",
      "style": { "navigationBarTitleText": "登录" }
    }
  ]
}
```

**路由跳转对比：**
```javascript
// Web 版
router.push('/dashboard')
router.replace('/login')
router.back()

// 小程序版
uni.navigateTo({ url: '/pages/dashboard/dashboard' })
uni.redirectTo({ url: '/pages/login/login' })
uni.navigateBack()
uni.switchTab({ url: '/pages/dashboard/dashboard' })  // TabBar 页面
```

### 2. 网络请求改造

#### Web 版 (utils/api.ts)
```typescript
import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000
})

apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default apiClient
```

#### 小程序版 (utils/request.ts)
```typescript
const BASE_URL = 'https://your-domain.com/api'

async function request(options) {
  const token = uni.getStorageSync('token')
  
  return uni.request({
    url: BASE_URL + options.url,
    method: options.method,
    data: options.data,
    header: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export default {
  get: (url, data) => request({ url, method: 'GET', data }),
  post: (url, data) => request({ url, method: 'POST', data })
}
```

### 3. 本地存储改造

```javascript
// Web 版
localStorage.setItem('token', token)
const token = localStorage.getItem('token')
localStorage.removeItem('token')

// 小程序版
uni.setStorageSync('token', token)
const token = uni.getStorageSync('token')
uni.removeStorageSync('token')

// 异步版本
uni.setStorage({ key: 'token', data: token })
uni.getStorage({ key: 'token', success: (res) => {} })
```

### 4. UI 组件改造

#### Element Plus → 原生组件

**按钮：**
```vue
<!-- Web 版 -->
<el-button type="primary" @click="handleClick">按钮</el-button>

<!-- 小程序版 -->
<button class="btn-primary" @click="handleClick">按钮</button>
```

**输入框：**
```vue
<!-- Web 版 -->
<el-input v-model="form.username" placeholder="请输入" />

<!-- 小程序版 -->
<input v-model="form.username" placeholder="请输入" />
```

**选择器：**
```vue
<!-- Web 版 -->
<el-select v-model="value">
  <el-option label="选项1" value="1" />
</el-select>

<!-- 小程序版 -->
<picker :range="options" @change="onChange">
  <view>{{ options[index] }}</view>
</picker>
```

**表格：**
```vue
<!-- Web 版 -->
<el-table :data="tableData">
  <el-table-column prop="name" label="姓名" />
</el-table>

<!-- 小程序版 - 使用列表 -->
<view class="table">
  <view class="row" v-for="item in tableData" :key="item.id">
    <text>{{ item.name }}</text>
  </view>
</view>
```

**消息提示：**
```javascript
// Web 版
import { ElMessage } from 'element-plus'
ElMessage.success('操作成功')
ElMessage.error('操作失败')

// 小程序版
uni.showToast({ title: '操作成功', icon: 'success' })
uni.showToast({ title: '操作失败', icon: 'none' })
```

**对话框：**
```javascript
// Web 版
import { ElMessageBox } from 'element-plus'
ElMessageBox.confirm('确定删除吗？').then(() => {})

// 小程序版
uni.showModal({
  title: '提示',
  content: '确定删除吗？',
  success: (res) => {
    if (res.confirm) {}
  }
})
```

### 5. 生命周期改造

```javascript
// Web 版
import { onMounted, onUnmounted } from 'vue'

onMounted(() => {
  console.log('组件挂载')
})

onUnmounted(() => {
  console.log('组件卸载')
})

// 小程序版
import { onMounted, onUnmounted } from 'vue'

onMounted(() => {
  console.log('页面加载')
})

onUnmounted(() => {
  console.log('页面卸载')
})

// 小程序特有生命周期
onShow(() => {
  console.log('页面显示')
})

onHide(() => {
  console.log('页面隐藏')
})

onPullDownRefresh(() => {
  console.log('下拉刷新')
  uni.stopPullDownRefresh()
})
```

### 6. WebSocket 改造

#### Web 版
```typescript
const ws = new WebSocket('ws://localhost:8080/ws')

ws.onopen = () => console.log('连接成功')
ws.onmessage = (event) => console.log(event.data)
ws.onerror = (error) => console.error(error)
ws.onclose = () => console.log('连接关闭')

ws.send(JSON.stringify({ type: 'ping' }))
```

#### 小程序版
```typescript
const socketTask = uni.connectSocket({
  url: 'wss://your-domain.com/ws'
})

socketTask.onOpen(() => console.log('连接成功'))
socketTask.onMessage((res) => console.log(res.data))
socketTask.onError((error) => console.error(error))
socketTask.onClose(() => console.log('连接关闭'))

socketTask.send({
  data: JSON.stringify({ type: 'ping' })
})
```

### 7. 图表改造

#### Web 版 (ECharts)
```vue
<template>
  <div ref="chartRef" style="height: 400px"></div>
</template>

<script setup>
import * as echarts from 'echarts'
import { ref, onMounted } from 'vue'

const chartRef = ref()

onMounted(() => {
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    xAxis: { type: 'category', data: ['Mon', 'Tue'] },
    yAxis: { type: 'value' },
    series: [{ data: [120, 200], type: 'line' }]
  })
})
</script>
```

#### 小程序版 (lime-echart)
```vue
<template>
  <l-echart ref="chartRef" @finished="initChart" />
</template>

<script setup>
import { ref } from 'vue'

const chartRef = ref()

const initChart = () => {
  const chart = chartRef.value.chart
  chart.setOption({
    xAxis: { type: 'category', data: ['Mon', 'Tue'] },
    yAxis: { type: 'value' },
    series: [{ data: [120, 200], type: 'line' }]
  })
}
</script>
```

## 三、样式单位转换

```scss
// Web 版 - 使用 px
.container {
  padding: 20px;
  font-size: 14px;
}

// 小程序版 - 使用 rpx (响应式像素)
.container {
  padding: 40rpx;  // 1px = 2rpx (iPhone 6 基准)
  font-size: 28rpx;
}
```

## 四、权限处理

```javascript
// Web 版 - 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else {
    next()
  }
})

// 小程序版 - App.vue 或页面 onLoad
onLoad(() => {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})
```

## 五、常见问题

### 1. 跨域问题
- **Web 版**：需要配置代理或 CORS
- **小程序版**：无跨域问题，但需要配置合法域名

### 2. 文件上传
```javascript
// Web 版
const formData = new FormData()
formData.append('file', file)
axios.post('/upload', formData)

// 小程序版
uni.chooseImage({
  success: (res) => {
    uni.uploadFile({
      url: 'https://your-domain.com/api/upload',
      filePath: res.tempFilePaths[0],
      name: 'file'
    })
  }
})
```

### 3. 页面刷新
- **Web 版**：支持 F5 刷新
- **小程序版**：使用下拉刷新或重新进入页面

## 六、性能优化建议

1. **图片优化**
   - 使用 WebP 格式
   - 压缩图片大小
   - 懒加载

2. **列表优化**
   - 使用虚拟列表
   - 分页加载
   - 避免一次性渲染大量数据

3. **请求优化**
   - 合并请求
   - 缓存数据
   - 防抖节流

4. **包体积优化**
   - 分包加载
   - 按需引入
   - 删除无用代码

