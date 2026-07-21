<template>
  <div class="container">
    <h2>系统设置</h2>
    <el-alert type="info" :closable="false" show-icon>
      说明：提供主要数据的增删改查入口，直接调用现有后端接口。
    </el-alert>

    <el-tabs v-model="active">
      <el-tab-pane label="系统用户" name="system-users">
        <div class="search-filters">
          <el-input
            v-model="sysUserSearchQuery"
            placeholder="请输入用户ID或用户名"
            class="search-input"
            :prefix-icon="Search"
            size="small"
            clearable
          />
          <el-select
            v-model="sysUserRole"
            placeholder="角色筛选"
            class="filter-select"
            @change="searchSystemUsers"
            size="small"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="电力公司人员" value="ADMIN" />
            <el-option label="社区人员" value="MANAGER" />
          </el-select>
          <el-select
            v-model="sysUserActive"
            placeholder="状态筛选"
            class="filter-select"
            @change="searchSystemUsers"
            size="small"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="激活" value="true" />
            <el-option label="禁用" value="false" />
          </el-select>
          <el-button type="primary" @click="searchSystemUsers" size="small">查询</el-button>
          <el-button @click="resetSystemUserFilters" size="small">重置</el-button>
        </div>

        <div class="user-table">
          <el-table :data="systemUserList" v-loading="sysUserLoading" style="width: 100%">
            <el-table-column prop="id" label="用户ID" width="100" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'"
                  size="small"
                >
                  {{ scope.row.role === 'ADMIN' ? '电力公司人员' : '社区人员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="managedCommunity" label="负责社区" width="140">
              <template #default="scope">
                {{ scope.row.role === 'MANAGER' ? (scope.row.managedCommunity || '未配置') : '全部社区' }}
              </template>
            </el-table-column>
            <el-table-column prop="active" label="状态" width="120">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.active ? 'success' : 'danger'"
                  size="small"
                >
                  {{ scope.row.active ? '激活' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column prop="updateTime" label="更新时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button link type="primary" size="small" @click="viewSysUserDetails(scope.row)">查看详情</el-button>
                <el-button link type="primary" size="small" @click="editSysUser(scope.row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="deleteSysUser(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination">
          <el-pagination
            v-model:current-page="sysUserCurrentPage"
            v-model:page-size="sysUserPageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="sysUserTotal"
            @size-change="handleSysUserSizeChange"
            @current-change="handleSysUserCurrentChange"
          />
        </div>

        <el-dialog v-model="showSysUserDetails" title="用户详情" width="600px">
          <div v-if="selectedSysUser" class="user-details">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户ID">{{ selectedSysUser.id }}</el-descriptions-item>
              <el-descriptions-item label="用户名">{{ selectedSysUser.username }}</el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="selectedSysUser.role === 'ADMIN' ? 'danger' : 'primary'">
                  {{ selectedSysUser.role === 'ADMIN' ? '电力公司人员' : '社区人员' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="负责社区">
                {{ selectedSysUser.role === 'MANAGER' ? (selectedSysUser.managedCommunity || '未配置') : '全部社区' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="selectedSysUser.active ? 'success' : 'danger'">
                  {{ selectedSysUser.active ? '激活' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ selectedSysUser.createTime }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ selectedSysUser.updateTime }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="showSysUserDetails = false">关闭</el-button>
            </span>
          </template>
        </el-dialog>

        <el-dialog v-model="showEditSysUser" title="编辑用户" width="600px">
          <el-form ref="editSysUserFormRef" :model="editSysUserForm" :rules="editSysUserRules" label-width="120px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="editSysUserForm.username" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="editSysUserForm.password" type="password" placeholder="留空则不修改密码" />
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <el-select v-model="editSysUserForm.role" placeholder="请选择角色">
                <el-option label="电力公司人员" value="ADMIN" />
                <el-option label="社区人员" value="MANAGER" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="editSysUserForm.role === 'MANAGER'" label="负责社区" prop="managedCommunity">
              <el-input v-model="editSysUserForm.managedCommunity" placeholder="请输入与居民档案一致的社区名称" />
            </el-form-item>
            <el-form-item label="状态" prop="active">
              <el-switch v-model="editSysUserForm.active" active-text="激活" inactive-text="禁用" />
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="showEditSysUser = false">取消</el-button>
              <el-button type="primary" @click="submitEditSysUserForm">确定</el-button>
            </span>
          </template>
        </el-dialog>
      </el-tab-pane>
      <el-tab-pane label="通用设置" name="general-settings">
        <div class="settings-panel">
          <div v-for="setting in settings" :key="setting.key" class="setting-item">
            <span class="setting-label">{{ setting.key }}</span>
            <el-input v-model="setting.value" class="setting-input" />
            <el-button type="primary" @click="updateSetting(setting)">保存</el-button>
          </div>
          
          
          <div class="system-management">
            <h3>系统管理功能</h3>
            <div class="management-buttons">
              <el-button type="danger" @click="clearAllData" :loading="clearLoading">
                一键清空数据
              </el-button>
              <el-button type="primary" @click="backupData">
                备份数据
              </el-button>
              <el-button type="success" @click="generateRandomData" :loading="generateLoading">
                生成随机数据
              </el-button>
            </div>
            <el-alert type="warning" :closable="false" show-icon style="margin-top: 20px;">
              警告：一键清空数据将删除所有表中的数据，请谨慎操作！
            </el-alert>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '../utils/api'
import CrudPanel from '../components/CrudPanel.vue'
const active = ref('system-users')
const clearLoading = ref(false)
const generateLoading = ref(false)


export interface Setting {
  key: string;
  value: string;
}


const settings = ref<Setting[]>([]);

const fetchSettings = async () => {
  try {
    const response = await apiClient.get('/settings');
    settings.value = response.data;
  } catch (error) {
    console.error('获取设置失败:', error);
    ElMessage.error('获取设置失败');
  }
};


const updateSetting = async (setting: Setting) => {
  try {
    const response = await apiClient.post('/settings', { key: setting.key, value: setting.value });
    if (response.data.success) {
      ElMessage.success('设置已更新');
    } else {
      ElMessage.error(response.data.message || '更新设置失败');
    }
  } catch (error) {
    console.error('更新设置失败:', error);
    ElMessage.error('更新设置失败');
  }
};


const clearAllData = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将永久删除所有数据，包括用户、用电记录、异常记录等。是否继续？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    clearLoading.value = true;
    const response = await apiClient.post('/system-management/clear-all-data');
    
    if (response.success) {
      ElMessage.success(response.message || '数据清空成功');
    } else {
      ElMessage.error(response.message || '数据清空失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空数据失败:', error);
      ElMessage.error('清空数据失败');
    }
  } finally {
    clearLoading.value = false;
  }
};


const backupData = async () => {
  try {
    
    const response = await apiClient.get('/system-management/backup-data', {
      responseType: 'blob'
    });
    
    
    const blob = new Blob([response.data], { type: 'application/sql; charset=UTF-8' });
    
    
    let filename = 'power_manage_backup.sql';
    const contentDisposition = response.headers['content-disposition'];
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1];
      }
    }
    
    
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    
    ElMessage.success('备份文件下载成功');
  } catch (error) {
    console.error('备份数据失败:', error);
    ElMessage.error('备份数据失败: ' + (error.message || '未知错误'));
  }
};


const generateRandomData = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将生成随机数据，可能会覆盖现有数据。是否继续？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    generateLoading.value = true;
    const response = await apiClient.post('/system-management/generate-random-data');
    
    if (response.success) {
      ElMessage.success(response.message || '随机数据生成成功');
    } else {
      ElMessage.error(response.message || '随机数据生成失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成随机数据失败:', error);
      ElMessage.error('生成随机数据失败');
    }
  } finally {
    generateLoading.value = false;
  }
};

onMounted(() => {
  fetchSettings();
});

const systemUserColumns = [
  { prop: 'id', label: 'ID', width: 80, editable: false },
  { prop: 'username', label: '用户名' },
  { prop: 'passwordHash', label: '密码哈希' },
  { prop: 'role', label: '角色' }
]


// 系统用户管理（从档案管理迁移）
interface SystemUser {
  id: number;
  username: string;
  password?: string;
  role: string;
  managedCommunity?: string;
  active: boolean;
  createTime: string;
  updateTime: string;
}

const sysUserSearchQuery = ref('');
const sysUserRole = ref('');
const sysUserActive = ref('');
const sysUserCurrentPage = ref(1);
const sysUserPageSize = ref(10);
const sysUserTotal = ref(0);
const systemUserList = ref<SystemUser[]>([]);
const sysUserLoading = ref(false);

const showSysUserDetails = ref(false);
const showEditSysUser = ref(false);
const selectedSysUser = ref<SystemUser | null>(null);
const editSysUserFormRef = ref<FormInstance>();
const editSysUserForm = reactive<Partial<SystemUser & { password?: string }>>({});
const editSysUserRules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
});

const loadSystemUsers = async () => {
  sysUserLoading.value = true;
  try {
    const params: any = { page: sysUserCurrentPage.value, pageSize: sysUserPageSize.value };
    if (sysUserRole.value) params.role = sysUserRole.value;
    if (sysUserActive.value) params.activeOnly = sysUserActive.value === 'true';

    const resp = await apiClient.get('system-users', { params });
    const records = Array.isArray(resp?.records) ? resp.records : [];
    systemUserList.value = records;
    sysUserTotal.value = Number(resp?.total ?? records.length ?? 0);
  } catch (error) {
    console.error('加载系统用户失败:', error);
    ElMessage.error('加载系统用户失败');
  } finally {
    sysUserLoading.value = false;
  }
};

const searchSystemUsers = () => {
  sysUserCurrentPage.value = 1;
  loadSystemUsers();
};

const resetSystemUserFilters = () => {
  sysUserSearchQuery.value = '';
  sysUserRole.value = '';
  sysUserActive.value = '';
  sysUserCurrentPage.value = 1;
  loadSystemUsers();
};

const handleSysUserSizeChange = (size: number) => {
  sysUserPageSize.value = size;
  loadSystemUsers();
};

const handleSysUserCurrentChange = (current: number) => {
  sysUserCurrentPage.value = current;
  loadSystemUsers();
};

const viewSysUserDetails = (user: SystemUser) => {
  selectedSysUser.value = { ...user };
  showSysUserDetails.value = true;
};

const editSysUser = (user: SystemUser) => {
  Object.assign(editSysUserForm, user);
  delete editSysUserForm.password;
  showEditSysUser.value = true;
};

const submitEditSysUserForm = async () => {
  if (!editSysUserFormRef.value) return;
  if (editSysUserForm.role === 'MANAGER' && !editSysUserForm.managedCommunity?.trim()) {
    ElMessage.error('区管理员必须配置负责社区');
    return;
  }
  await editSysUserFormRef.value.validate();
  const submitData: any = { ...editSysUserForm };
  submitData.managedCommunity = submitData.role === 'MANAGER'
    ? submitData.managedCommunity.trim()
    : null;
  if (!submitData.password) delete submitData.password;
  await apiClient.put(`/api/system-users/${editSysUserForm.id}`, submitData);
  loadSystemUsers();
  showEditSysUser.value = false;
};

const deleteSysUser = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除该系统用户？', '提示', { type: 'warning' });
    await apiClient.delete(`/api/system-users/${id}`);
    ElMessage.success('删除成功');
    loadSystemUsers();
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除系统用户失败:', e);
      ElMessage.error('删除失败');
    }
  }
};

// 进入页面时加载一次
loadSystemUsers();
</script>

<style scoped>
.container { padding: 20px; }

.settings-panel {
  padding: 20px;
}

.setting-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.setting-label {
  width: 200px;
  font-weight: bold;
}

.setting-input {
  flex: 1;
  margin: 0 15px;
}

.system-management {
  margin-top: 40px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.system-management h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.management-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.management-buttons .el-button {
  min-width: 120px;
}

/* 缩短并紧凑化搜索区域 */
.search-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 12px;
}

.search-input {
  width: 220px;
  max-width: 40vw;
}

.filter-select {
  width: 140px;
}

/* 窄屏自适应：输入和筛选元素占满宽度 */
@media (max-width: 768px) {
  .search-input,
  .filter-select {
    width: 100%;
    max-width: 100%;
  }
}
</style>

