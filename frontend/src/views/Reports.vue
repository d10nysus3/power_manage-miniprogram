<template>
  <div class="reports-page">
    <h2>生成报告</h2>
    <el-form :model="reportForm" label-width="120px">
      <el-form-item label="报告类型">
        <el-select v-model="reportForm.reportType" placeholder="选择报告类型">
          <el-option label="用电量分析报告" value="usage_analysis" />
          <el-option label="异常事件报告" value="anomaly_events" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="报告格式">
        <el-select v-model="reportForm.format" placeholder="选择报告格式">
          <el-option label="PDF格式" value="PDF" />
          <el-option label="HTML格式" value="HTML" />
          <el-option label="文本格式" value="TEXT" />
        </el-select>
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="generateReport">生成报告</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import apiClient from '../utils/api';
import { ElMessage } from 'element-plus';

const reportForm = ref({
  reportType: 'usage_analysis',
  format: 'PDF'
});

const generateReport = async () => {
  try {
    
    const requestData = {
      reportType: reportForm.value.reportType,
      format: reportForm.value.format
    };
    
    const response = await apiClient.post('/reports/generate', requestData);
    if (response.data.success) {
      
      window.open(response.data.data.reportUrl, '_blank');
    } else {
      ElMessage.error(response.data.message || '生成报告失败');
    }
  } catch (error) {
    console.error('生成报告失败:', error);
    ElMessage.error('生成报告失败');
  }
};
</script>

<style scoped>
.reports-page {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.el-form {
  margin-top: 20px;
}

.el-button {
  margin-top: 20px;
}
</style>