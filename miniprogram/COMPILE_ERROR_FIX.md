# 编译错误修复指南

## 问题原因

你遇到的错误是：
```
SystemError [ERR_SYSTEM_ERROR]: A system error occurred: uv_interface_addresses returned Unknown system error 1
```

这是因为：
1. **Node.js v24 兼容性问题**：uni-app 在 Node.js v24 上获取网络接口信息时出错
2. **系统权限问题**：macOS 可能限制了网络接口访问

## 解决方案

### 方案一：降级 Node.js 版本（推荐）

使用 Node.js v18 或 v20 LTS 版本：

```bash
# 使用 nvm 切换版本
nvm install 20
nvm use 20

# 或者使用 n
n 20

# 然后重新运行
cd miniprogram
npm run dev:mp-weixin
```

### 方案二：使用 HBuilderX（最简单）

1. 下载 [HBuilderX](https://www.dcloud.io/hbuilderx.html)
2. 打开 HBuilderX
3. 文件 → 导入 → 从本地目录导入
4. 选择 `miniprogram` 目录
5. 运行 → 运行到小程序模拟器 → 微信开发者工具

### 方案三：手动修复（如果必须使用 Node.js v24）

1. 编辑 `node_modules/@dcloudio/uni-cli-shared/dist/checkUpdate.js`

找到第 37 行左右的代码：
```javascript
const data = getInterfaceAddresses();
```

修改为：
```javascript
let data;
try {
  data = getInterfaceAddresses();
} catch (error) {
  console.warn('Warning: networkInterfaces failed');
  data = {};
}
```

2. 保存后重新运行：
```bash
npm run dev:mp-weixin
```

### 方案四：使用微信开发者工具直接开发

如果编译一直失败，你可以：

1. 使用微信开发者工具创建一个新的小程序项目
2. 手动复制我们创建的页面和工具类
3. 在微信开发者工具中直接编写和调试

## 验证编译成功

编译成功后，你应该看到：
```
✔ 编译完成
```

并且会生成 `dist/dev/mp-weixin/` 目录，包含：
- app.js
- app.json
- app.wxss
- pages/ 目录

## 导入微信开发者工具

1. 打开微信开发者工具
2. 点击「导入项目」
3. 选择目录：`miniprogram/dist/dev/mp-weixin`
4. 填写 AppID（测试可以使用测试号）
5. 点击「导入」

## 开发建议

由于 Node.js v24 是最新版本，可能存在兼容性问题，建议：
- **生产环境**：使用 Node.js v20 LTS
- **开发工具**：使用 HBuilderX（官方推荐）

## 需要帮助？

如果以上方案都不行，请：
1. 检查 Node.js 版本：`node --version`
2. 尝试使用 HBuilderX
3. 或者等待 uni-app 更新以支持 Node.js v24

