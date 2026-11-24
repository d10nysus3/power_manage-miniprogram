# 快速开始指南

## 一、环境准备

### 1. 安装 Node.js
确保已安装 Node.js 14+ 版本

### 2. 安装微信开发者工具
从 [微信官网](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html) 下载并安装

### 3. 注册小程序账号
在 [微信公众平台](https://mp.weixin.qq.com/) 注册小程序账号，获取 AppID

## 二、项目配置

### 步骤 1：安装依赖

```bash
cd miniprogram
npm install
```

### 步骤 2：配置后端 API 地址

接口地址由环境文件提供：`.env.development` 用于开发，`.env.production` 用于正式构建，当前均为 `https://pttxyd.cn/api`；Nginx 将 `/api/` 转发到后端的 `/power_manage/api/`。不要在微信后台填写 `/api` 路径。若需调试本地后端，可创建不提交的 `.env.development.local` 覆盖开发地址，并在开发者工具中临时关闭域名校验。

正式构建会检查地址必须是 HTTPS 域名，且不能带端口。

### 步骤 3：配置小程序 AppID

编辑 `manifest.json` 文件：

```json
{
  "mp-weixin": {
    "appid": "wxXXXXXXXXXXXXXXXX"  // 改为你的小程序 AppID
  }
}
```

### 步骤 4：配置服务器域名（重要！）

在微信公众平台配置合法域名：

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入：开发 → 开发管理 → 开发设置 → 服务器域名
3. 配置以下域名：
   - **request 合法域名**：`https://pttxyd.cn`
   - **socket 合法域名**：当前小程序未发起 WebSocket 连接，可先留空；启用时填 `wss://pttxyd.cn`
   - **uploadFile / downloadFile 合法域名**：当前未使用，可先留空

服务器必须能正常响应 `https://pttxyd.cn/api/`。如果出现 502，请检查后端 8081 端口及 Nginx 的 `/api/` 反向代理；小程序域名配置无法修复 502。

## 三、运行项目

### 开发模式

```bash
npm run dev:mp-weixin
```

### 导入微信开发者工具

1. 打开微信开发者工具
2. 点击「导入项目」
3. 选择项目目录：`miniprogram/dist/dev/mp-weixin`
4. 填写项目名称和 AppID
5. 点击「导入」

### 开发调试

- 在微信开发者工具中可以实时预览
- 修改代码后会自动编译刷新
- 可以使用真机调试功能

## 四、测试账号

默认测试账号（需要后端支持）：

```
账号：admin
密码：123456
```

## 五、功能测试清单

### ✅ 基础功能
- [ ] 登录功能
- [ ] 退出登录
- [ ] 记住密码

### ✅ 首页
- [ ] 查看统计数据
- [ ] 查看最近异常记录
- [ ] 下拉刷新

### ✅ 用户档案
- [ ] 查看用户列表
- [ ] 搜索用户
- [ ] 筛选用户类型
- [ ] 分页加载

### ✅ 异常检测
- [ ] 查看异常列表
- [ ] 筛选异常类型
- [ ] 筛选处理状态
- [ ] 标记已处理

### ✅ 个人中心
- [ ] 查看个人信息
- [ ] 修改密码
- [ ] 退出登录

## 六、常见问题

### Q1: 编译报错 "Cannot find module '@dcloudio/uni-app'"

**解决方案**：
```bash
npm install
```

### Q2: 请求失败 "request:fail url not in domain list"

**原因**：未配置服务器域名或使用了 HTTP 协议

**解决方案**：
1. 在微信公众平台配置合法域名
2. 开发阶段可以在微信开发者工具中勾选「不校验合法域名」

### Q3: 页面空白或报错

**检查步骤**：
1. 查看控制台错误信息
2. 检查 API 地址是否正确
3. 检查后端服务是否正常运行
4. 检查网络请求是否成功

### Q4: TabBar 不显示

**原因**：TabBar 图标未准备

**解决方案**：
1. 准备 TabBar 图标（建议 81x81 px）
2. 放置在 `static/tabbar/` 目录
3. 或暂时注释 `pages.json` 中的 tabBar 配置

## 七、下一步开发

### 1. 集成图表库

推荐使用 `lime-echart`：

```bash
npm install lime-echart echarts
```

在 `pages/dashboard/dashboard.vue` 中替换图表占位符。

### 2. 实现 WebSocket 实时推送

使用 `utils/websocket.ts` 工具类：

```javascript
import WebSocketClient from '@/utils/websocket'

const ws = new WebSocketClient({
  url: 'wss://your-domain.com/ws',
  onMessage: (data) => {
    // 处理实时消息
  }
})

ws.connect()
```

### 3. 完善其他页面

参考已实现的页面，完成：
- 用电分析页面
- 应急处理页面
- 系统设置页面
- 报告生成页面

### 4. 优化用户体验

- 添加加载动画
- 优化错误提示
- 添加空状态提示
- 优化列表性能

## 八、部署上线

### 1. 构建生产版本

```bash
npm run build:mp-weixin
```

### 2. 上传代码

1. 微信开发者工具导入 `miniprogram` 根目录时，确认「详情 → 本地设置」中的小程序目录为 `dist/build/mp-weixin/`；也可直接导入 `miniprogram/dist/build/mp-weixin`。
2. 确认构建产物 `dist/build/mp-weixin/utils/request.js` 中的接口域名为 `https://pttxyd.cn/api`。
3. 在微信开发者工具中点击「上传」，不要上传 `dist/dev/mp-weixin` 中的开发构建。
4. 填写版本号和项目备注，上传成功后在微信公众平台查看。

### 3. 提交审核

1. 登录微信公众平台
2. 进入：版本管理
3. 选择开发版本提交审核
4. 填写审核信息

### 4. 发布上线

审核通过后，点击「发布」即可上线。

## 九、技术支持

- **uni-app 文档**：https://uniapp.dcloud.net.cn/
- **微信小程序文档**：https://developers.weixin.qq.com/miniprogram/dev/framework/
- **项目改造指南**：查看 `MIGRATION_GUIDE.md`

## 十、项目结构说明

```
miniprogram/
├── pages/              # 页面
│   ├── login/         # 登录页
│   ├── dashboard/     # 首页
│   ├── user-profile/  # 用户档案
│   ├── anomaly-detection/  # 异常检测
│   └── personal-info/ # 个人中心
├── utils/             # 工具类
│   ├── request.ts     # 网络请求（重要）
│   ├── auth.ts        # 权限管理
│   ├── format.ts      # 格式化
│   └── websocket.ts   # WebSocket
├── static/            # 静态资源
├── App.vue            # 应用入口
├── pages.json         # 页面配置（重要）
├── manifest.json      # 应用配置（重要）
└── package.json       # 依赖配置
```

---

🎉 **恭喜！现在你可以开始开发微信小程序版本了！**

如有问题，请查看 `README.md` 和 `MIGRATION_GUIDE.md` 获取更多帮助。
