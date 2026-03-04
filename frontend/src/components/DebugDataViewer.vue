<template>
  <div class="debug-container">
    <h3>数据调试查看器</h3>
    <div class="debug-section">
      <h4>当前路由信息</h4>
      <div class="route-info">
        <p><strong>路径:</strong> {{ currentRoute }}</p>
        <p><strong>名称:</strong> {{ routeName }}</p>
        <p><strong>参数:</strong> {{ JSON.stringify(routeParams) }}</p>
      </div>
    </div>
    <div class="debug-section">
      <h4>异常记录数据预览</h4>
      <div v-if="anomalyData.length > 0" class="data-preview">
        <p><strong>数据量:</strong> {{ anomalyData.length }}</p>
        <pre>{{ JSON.stringify(anomalyData.slice(0, 3), null, 2) }}</pre>
      </div>
      <div v-else class="no-data">暂无数据</div>
    </div>
    <div class="debug-section">
      <h4>已处理的异常记录</h4>
      <div v-if="processedData.length > 0" class="data-preview">
        <p><strong>已处理数据量:</strong> {{ processedData.length }}</p>
        <pre>{{ JSON.stringify(processedData, null, 2) }}</pre>
      </div>
      <div v-else class="no-data">暂无已处理数据</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const currentRoute = ref(route.path);
const routeName = ref(route.name || 'unnamed');
const routeParams = ref(route.params);
const anomalyData = ref<any[]>([]);


const processedData = computed(() => {
  return anomalyData.value.filter(item => item.isProcessed);
});




const loadSampleData = async () => {
  console.log('直接加载模拟数据');
  anomalyData.value = [
    {
      id: 1,
      userId: 1001,
      userName: '张三',
      anomalyType: '高峰异常',
      alertType: '高峰异常',
      alertTime: '2024-01-25 14:30:00',
      timestamp: '2024-01-25 14:30:00',
      description: '用电量在30分钟内突增300%，超过正常范围',
      alertContent: '用电量在30分钟内突增300%，超过正常范围',
      isProcessed: false,
      severityLevel: 'high'
    },
    {
      id: 2,
      userId: 1002,
      userName: '李四',
      anomalyType: '低谷异常',
      alertType: '低谷异常',
      alertTime: '2024-01-24 08:15:00',
      timestamp: '2024-01-24 08:15:00',
      description: '凌晨3点用电量异常高，可能存在安全隐患',
      alertContent: '凌晨3点用电量异常高，可能存在安全隐患',
      isProcessed: false,
      severityLevel: 'medium'
    },
    {
      id: 3,
      userId: 1003,
      userName: '王五',
      anomalyType: '持续异常',
      alertType: '持续异常',
      alertTime: '2024-01-23 16:45:00',
      timestamp: '2024-01-23 16:45:00',
      description: '连续3天用电量低于历史平均值50%',
      alertContent: '连续3天用电量低于历史平均值50%',
      isProcessed: true,
      severityLevel: 'low',
      processTime: '2024-01-23 17:00:00',
      processor: '管理员'
    },
    {
      id: 4,
      userId: 1004,
      userName: '赵六',
      anomalyType: '突变异常',
      alertType: '突变异常',
      alertTime: '2024-01-22 10:20:00',
      timestamp: '2024-01-22 10:20:00',
      description: '用电量在5分钟内从正常水平骤降至几乎为零',
      alertContent: '用电量在5分钟内从正常水平骤降至几乎为零',
      isProcessed: false,
      severityLevel: 'high'
    },
    {
      id: 5,
      userId: 1005,
      userName: '孙七',
      anomalyType: '高峰异常',
      alertType: '高峰异常',
      alertTime: '2024-01-21 19:30:00',
      timestamp: '2024-01-21 19:30:00',
      description: '晚间用电高峰时段用电量超出历史峰值20%',
      alertContent: '晚间用电高峰时段用电量超出历史峰值20%',
      isProcessed: true,
      severityLevel: 'medium',
      processTime: '2024-01-21 20:00:00',
      processor: '管理员'
    }
  ];
}

onMounted(() => {
  console.log('调试组件已挂载，路由信息:', route);
  loadSampleData();
});
</script>

<style scoped>
.debug-container {
  padding: 20px;
  background-color: #f0f2f5;
  border-radius: 8px;
  margin: 20px;
}

.debug-section {
  margin-bottom: 20px;
  padding: 15px;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.route-info {
  line-height: 1.8;
}

.data-preview {
  max-height: 400px;
  overflow-y: auto;
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
}

.no-data {
  color: #909399;
  text-align: center;
  padding: 20px;
}

pre {
  margin: 0;
  font-size: 12px;
}
</style>