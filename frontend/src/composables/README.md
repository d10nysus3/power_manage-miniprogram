# useDevice 钩子使用说明

## 简介

`useDevice` 是一个 Vue3 Composition API 钩子，用于检测当前设备类型（桌面端/移动端）并提供响应式的设备信息。

## 功能特性

- 根据屏幕宽度自动判断是否为移动端（宽度 <= 768px 视为移动端）
- 监听窗口 resize 事件，支持动态切换设备类型
- 提供响应式的 `isMobile` 状态
- 提供当前屏幕宽度和高度的计算属性

## 安装

该钩子已内置在项目中，位于 `src/composables/useDevice.ts`。

## 使用方法

### 基本使用

```vue
<template>
  <div :class="{ 'mobile-layout': isMobile }">
    <!-- 桌面端布局 -->
    <div v-if="!isMobile">
      <!-- 桌面端内容 -->
    </div>
    
    <!-- 移动端布局 -->
    <div v-else>
      <!-- 移动端内容 -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDevice } from './composables/useDevice';

const { isMobile } = useDevice();
</script>

<style scoped>
/* 基础样式 */

/* 移动端样式 */
.mobile-layout {
  /* 移动端特定样式 */
}

/* 响应式媒体查询 */
@media (max-width: 768px) {
  /* 移动端样式 */
}
</style>
```

### 完整使用示例

```vue
<template>
  <div class="app-container">
    <h1>设备布局示例</h1>
    
    <!-- 根据设备类型显示不同内容 -->
    <div v-if="isMobile">
      <p>当前为移动端设备</p>
      <p>屏幕尺寸: {{ screenWidth }}x{{ screenHeight }}</p>
    </div>
    <div v-else>
      <p>当前为桌面端设备</p>
      <p>屏幕尺寸: {{ screenWidth }}x{{ screenHeight }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDevice } from './composables/useDevice';

const { isMobile, screenWidth, screenHeight } = useDevice();
</script>
```

## 实际应用

### 在 App.vue 中的应用

在 `App.vue` 中，我们使用 `useDevice` 钩子来控制侧边栏的显示和折叠状态：

```vue
<script setup lang="ts">
import { useDevice } from './composables/useDevice';

const isCollapse = ref(false);
const { isMobile } = useDevice();

// 初始化时，如果是移动端则自动折叠侧边栏
onMounted(() => {
  if (isMobile.value) {
    isCollapse.value = true;
  }
});

// 点击菜单项时，如果是移动端则自动折叠侧边栏
const handleMenuClick = (path: string) => {
  router.push(path);
  if (isMobile.value) {
    isCollapse.value = true;
  }
};
</script>
```

### 在组件中的应用

在 `DeviceLayoutExample.vue` 组件中，我们展示了如何根据设备类型切换不同的布局：

```vue
<template>
  <div class="layout-example" :class="{ 'mobile-layout': isMobile }">
    <!-- 桌面端布局 -->
    <div v-if="!isMobile" class="desktop-layout">
      <!-- 桌面端内容 -->
    </div>
    
    <!-- 移动端布局 -->
    <div v-else class="mobile-layout-content">
      <!-- 移动端内容 -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDevice } from '../composables/useDevice';

const { isMobile } = useDevice();
</script>
```

## 响应式设计最佳实践

1. **使用媒体查询**：结合 CSS 媒体查询，为不同屏幕尺寸设置不同的样式
2. **流式布局**：使用百分比、flex 或 grid 布局，使页面元素能够自适应不同屏幕尺寸
3. **图片响应式**：使用 `max-width: 100%` 确保图片不会超出容器宽度
4. **字体大小**：使用相对单位（如 `rem`、`em`）或 `clamp()` 函数，使字体大小能够自适应屏幕尺寸
5. **触摸友好**：在移动端布局中，确保按钮和可点击元素的尺寸足够大，便于触摸操作
6. **性能优化**：在移动端，考虑减少不必要的动画和过渡效果，提高页面性能

## 注意事项

- 当屏幕宽度在 768px 附近切换时，可能会触发多次 `resize` 事件，这是正常现象
- 在使用 `isMobile` 状态控制布局时，建议使用 `v-if`/`v-else` 或动态类名，避免不必要的 DOM 元素渲染
- 对于复杂的布局，建议结合 CSS 媒体查询和 `isMobile` 状态，以获得最佳的用户体验