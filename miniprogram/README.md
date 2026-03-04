# 特需用户用电监测与异常预警系统 - 微信小程序版

基于 uni-app 框架开发的微信小程序版本。

## 项目结构

```
miniprogram/
├── pages/                      # 页面目录
│   ├── login/                  # 登录页
│   ├── dashboard/              # 首页仪表盘
│   ├── user-profile/           # 用户档案管理
│   ├── anomaly-detection/      # 异常检测
│   ├── personal-info/          # 个人信息
│   └── ...
├── utils/                      # 工具类
│   ├── request.ts              # 网络请求封装
│   ├── auth.ts                 # 权限管理
│   ├── format.ts               # 格式化工具
│   └── websocket.ts            # WebSocket 封装
├── static/                     # 静态资源
│   └── tabbar/                 # 底部导航图标
├── App.vue                     # 应用入口
├── pages.json                  # 页面配置
├── manifest.json               # 应用配置
└── package.json                # 依赖配置
```

## 快速开始

### 1. 安装依赖

```bash
cd miniprogram
npm install
```

### 2. 配置后端地址

修改 `utils/request.ts` 中的 `BASE_URL`：

```typescript
const BASE_URL = 'https://your-domain.com/api'  // 改为你的后端地址
```

### 3. 配置微信小程序 AppID

修改 `manifest.json` 中的 `mp-weixin.appid`：

```json
{
  "mp-weixin": {
    "appid": "你的小程序AppID"
  }
}
```

### 4. 运行项目

```bash
# 开发模式
npm run dev:mp-weixin

# 构建生产版本
npm run build:mp-weixin
```

### 5. 导入微信开发者工具

1. 打开微信开发者工具
2. 导入项目，选择 `miniprogram/dist/dev/mp-weixin` 目录
3. 开始开发调试

## 主要功能

### ✅ 已实现功能

- **用户认证**
  - 登录/退出
  - 记住密码
  - Token 自动管理

- **首页仪表盘**
  - 系统状态统计卡片
  - 最近异常记录列表
  - 下拉刷新

- **用户档案管理**
  - 用户列表展示
  - 搜索和筛选
  - 分页加载

- **异常检测**
  - 异常记录列表
  - 类型和状态筛选
  - 标记已处理

- **个人中心**
  - 个人信息展示
  - 修改密码
  - 退出登录

### 🚧 待完成功能

- **图表展示**
  - 需要集成 `lime-echart` 或 `uCharts`
  - 用电量趋势图
  - 异常类型分布图

- **实时推送**
  - WebSocket 连接
  - 异常实时通知

- **更多页面**
  - 用电分析
  - 应急处理
  - 系统设置
  - 报告生成

## 关键改造点

### 1. 路由系统

**Web 版 (Vue Router):**
```javascript
router.push('/dashboard')
```

**小程序版 (uni-app):**
```javascript
uni.navigateTo({ url: '/pages/dashboard/dashboard' })
uni.switchTab({ url: '/pages/dashboard/dashboard' })  // TabBar 页面
```

### 2. 网络请求

**Web 版 (axios):**
```javascript
import axios from 'axios'
axios.get('/api/users')
```

**小程序版 (uni.request):**
```javascript
import request from '@/utils/request'
request.get('/users')
```

### 3. 本地存储

**Web 版:**
```javascript
localStorage.setItem('token', token)
```

**小程序版:**
```javascript
uni.setStorageSync('token', token)
```

### 4. UI 组件

**Web 版 (Element Plus):**
```vue
<el-button type="primary">按钮</el-button>
```

**小程序版 (原生组件):**
```vue
<button class="btn-primary">按钮</button>
```

## 图表集成指南

### 推荐方案：lime-echart

1. 安装依赖：
```bash
npm install lime-echart echarts
```

2. 在页面中使用：
```vue
<template>
  <l-echart ref="chartRef" @finished="handleFinished" />
</template>

<script setup>
import { ref } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref(null)

const handleFinished = () => {
  const chart = chartRef.value.chart
  chart.setOption({
    // ECharts 配置
  })
}
</script>
```

## WebSocket 配置

### 1. 配置合法域名

在微信公众平台配置 WebSocket 合法域名：
- 开发 > 开发管理 > 开发设置 > 服务器域名
- 添加 `wss://your-domain.com`

### 2. 使用 WebSocket

```javascript
import WebSocketClient from '@/utils/websocket'

const ws = new WebSocketClient({
  url: 'wss://your-domain.com/ws',
  onMessage: (data) => {
    console.log('收到消息:', data)
  }
})

ws.connect()
```

## 注意事项

### 1. 域名配置

小程序要求所有网络请求必须使用 HTTPS/WSS 协议，且域名需要在微信公众平台配置：
- request 合法域名
- socket 合法域名
- uploadFile 合法域名
- downloadFile 合法域名

### 2. 权限申请

某些功能需要用户授权：
- 位置信息
- 相机
- 相册
- 通知

### 3. 性能优化

- 图片使用 WebP 格式
- 列表使用虚拟滚动
- 合理使用分包加载
- 避免频繁 setData

### 4. 兼容性

- 测试不同机型和系统版本
- 处理低版本 API 兼容
- 提供降级方案

## 开发建议

1. **使用 TypeScript**：提高代码质量和可维护性
2. **组件化开发**：抽取公共组件，提高复用性
3. **状态管理**：复杂状态可使用 Pinia
4. **错误处理**：统一错误处理和提示
5. **日志记录**：便于问题排查

## 部署上线

### 1. 构建生产版本

```bash
npm run build:mp-weixin
```

### 2. 上传代码

使用微信开发者工具上传代码到微信公众平台

### 3. 提交审核

在微信公众平台提交版本审核

### 4. 发布上线

审核通过后发布上线

## 技术支持

如有问题，请参考：
- [uni-app 官方文档](https://uniapp.dcloud.net.cn/)
- [微信小程序官方文档](https://developers.weixin.qq.com/miniprogram/dev/framework/)

## License

MIT

