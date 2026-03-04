<template>
  <div class="layout-example" :class="{ 'mobile-layout': isMobile }">
    <h2>设备布局示例</h2>
    
    <!-- 桌面端布局 -->
    <div v-if="!isMobile" class="desktop-layout">
      <div class="desktop-grid">
        <div class="grid-item">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>卡片 1</span>
              </div>
            </template>
            <p>这是桌面端的卡片布局</p>
          </el-card>
        </div>
        <div class="grid-item">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>卡片 2</span>
              </div>
            </template>
            <p>这是桌面端的卡片布局</p>
          </el-card>
        </div>
        <div class="grid-item">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>卡片 3</span>
              </div>
            </template>
            <p>这是桌面端的卡片布局</p>
          </el-card>
        </div>
      </div>
    </div>
    
    <!-- 移动端布局 -->
    <div v-else class="mobile-layout-content">
      <el-card class="mobile-card">
        <template #header>
          <div class="card-header">
            <span>卡片 1</span>
          </div>
        </template>
        <p>这是移动端的卡片布局</p>
      </el-card>
      <el-card class="mobile-card">
        <template #header>
          <div class="card-header">
            <span>卡片 2</span>
          </div>
        </template>
        <p>这是移动端的卡片布局</p>
      </el-card>
      <el-card class="mobile-card">
        <template #header>
          <div class="card-header">
            <span>卡片 3</span>
          </div>
        </template>
        <p>这是移动端的卡片布局</p>
      </el-card>
    </div>
    
    <!-- 设备信息显示 -->
    <div class="device-info">
      <p>当前设备类型: {{ isMobile ? '移动端' : '桌面端' }}</p>
      <p>屏幕宽度: {{ screenWidth }}px</p>
      <p>屏幕高度: {{ screenHeight }}px</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDevice } from '../composables/useDevice';

const { isMobile, screenWidth, screenHeight } = useDevice();
</script>

<style scoped>
.layout-example {
  padding: 20px;
  transition: all 0.3s ease;
}

.desktop-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.grid-item {
  width: 100%;
}

.mobile-layout-content {
  margin: 20px 0;
}

.mobile-card {
  margin-bottom: 15px;
}

.device-info {
  margin-top: 30px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
}

.device-info p {
  margin: 5px 0;
}

@media (max-width: 768px) {
  .layout-example {
    padding: 10px;
  }
  
  .device-info {
    font-size: 12px;
    padding: 10px;
  }
}
</style>