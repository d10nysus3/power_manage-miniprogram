<template>
  <div>
    <crud-panel title="审计日志" :columns="columns" :search-options="searchOptions" :get-data="fetchData" fetch-url="/audit-logs"/>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import CrudPanel from '../components/CrudPanel.vue';
import apiClient from '../utils/api';

const columns = ref([
  { prop: 'username', label: '用户名' },
  { prop: 'action', label: '动作' },
  { prop: 'timestamp', label: '时间' },
]);

const searchOptions = ref([]);

const fetchData = async (params: any) => {
  const response = await apiClient.get('/audit-logs', { params });
  return response.data;
};
</script>