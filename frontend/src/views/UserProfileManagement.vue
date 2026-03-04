<template>
  <div class="container">
    <h2>档案管理</h2>
    <el-tabs v-model="activeTab">



  

      <!-- 用户管理选项卡 -->
      <el-tab-pane label="用户管理" name="users">
        <crud-panel
          :columns="userColumns"
          fetch-url="/api/users"
        create-url="/api/users"
        update-url="/api/users"
        delete-url="/api/users"
          :show-community-filter="true"
        />
      </el-tab-pane>
  
      <!-- 用电数据选项卡 -->
      <el-tab-pane label="用电数据" name="electricity">
        <crud-panel
          :columns="elecColumns"
          fetch-url="/api/electricity-data"
        create-url="/api/electricity-data"
        update-url="/api/electricity-data"
        delete-url="/api/electricity-data"
        />
      </el-tab-pane>
  
      
    </el-tabs>
  
    <!-- 用户详情对话框 -->
    <el-dialog v-model="showUserDetails" title="用户详情" width="600px">
      <div v-if="selectedUser" class="user-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ selectedUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ selectedUser.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="selectedUser.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ selectedUser.role === 'ADMIN' ? '电力公司人员' : '社区人员' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedUser.active ? 'success' : 'danger'">
              {{ selectedUser.active ? '激活' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedUser.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ selectedUser.updateTime }}</el-descriptions-item>
        </el-descriptions>

        <div class="profile-section" style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 20px;" v-loading="profileLoading">
            <h3 style="margin-bottom: 15px;">用户用电画像</h3>
            <div v-if="userProfile">
                 <el-descriptions :column="2" border>
                    <el-descriptions-item label="模式类型">{{ userProfile.patternType }}</el-descriptions-item>
                    <el-descriptions-item label="置信度">{{ userProfile.confidence ? (userProfile.confidence * 100).toFixed(1) + '%' : '-' }}</el-descriptions-item>
                    <el-descriptions-item label="分析时间">{{ userProfile.analysisDate ? userProfile.analysisDate.replace('T', ' ').substring(0, 19) : '-' }}</el-descriptions-item>
                    <el-descriptions-item label="数据周期">{{ userProfile.dataPeriod }}</el-descriptions-item>
                 </el-descriptions>
                 <div style="margin-top: 15px;">
                    <div style="margin-bottom: 8px; font-weight: bold; color: #606266;">特征标签：</div>
                    <div v-if="userProfile.featureTags && userProfile.featureTags.length">
                      <el-tag v-for="tag in userProfile.featureTags" :key="tag" style="margin-right: 8px; margin-bottom: 8px;">{{ tag }}</el-tag>
                    </div>
                    <div v-else class="text-gray-400">无特征标签</div>
                 </div>
                 <div style="margin-top: 15px; text-align: right;">
                     <el-button type="primary" size="small" @click="generateUserProfile">重新生成画像</el-button>
                 </div>
            </div>
            <div v-else style="text-align: center; padding: 20px;">
                <p style="color: #909399; margin-bottom: 15px;">该用户暂无画像数据</p>
                <el-button type="primary" @click="generateUserProfile">立即生成画像</el-button>
            </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUserDetails = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditUser" title="编辑用户" width="600px">
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="editForm.password" type="password" placeholder="留空则不修改密码" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" placeholder="请选择角色">
            <el-option label="电力公司人员" value="ADMIN" />
            <el-option label="社区人员" value="MANAGER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="active">
          <el-switch v-model="editForm.active" active-text="激活" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditUser = false">取消</el-button>
          <el-button type="primary" @click="submitEditForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { Search } from '@element-plus/icons-vue';
import type { FormInstance } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import apiClient from '../utils/api';
import CrudPanel from '../components/CrudPanel.vue';
import { SpecialUserType, SpecialUserTypeLabels, determineSpecialUserTypes, getSpecialUserTypeText } from '../enums/SpecialUserType';


interface User {
  id: number;
  username: string;
  password?: string;
  role: string;
  active: boolean;
  createTime: string;
  updateTime: string;
}

interface EditForm {
  id?: number;
  username?: string;
  password?: string;
  role?: string;
  active?: boolean;
}


const searchQuery = ref('');
const userType = ref('');
const specialNeed = ref('');
const activeTab = ref('users');


const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);


const userList = ref<User[]>([]);


const showUserDetails = ref(false);
const showEditUser = ref(false);
const selectedUser = ref<User | null>(null);
const userProfile = ref<any>(null);
const profileLoading = ref(false);


const editFormRef = ref<FormInstance>();
const editForm = reactive<EditForm>({});

const editFormRules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
});


const loading = ref(false);


const loadUsers = async () => {
  loading.value = true;
  try {
    let endpoint = 'system-users';
    const params: any = {};

    // 角色筛选
    if (userType.value) {
      params.role = userType.value;
    }
    // 状态筛选
    if (specialNeed.value) {
      params.activeOnly = specialNeed.value === 'true';
    }
    // 关键词筛选
    if (searchQuery.value) {
      params.keyword = searchQuery.value.trim();
    }

    // 恢复分页参数
    params.page = currentPage.value;
    params.pageSize = pageSize.value;

    const response = await apiClient.get(endpoint, { params });
    const records = Array.isArray(response?.records) ? response.records : [];
    userList.value = records;
    total.value = Number(response?.total ?? records.length ?? 0);
  } catch (error: any) {
    console.error('加载用户列表失败:', error);
    ElMessage.error('加载用户列表失败');
  } finally {
    loading.value = false;
  }
};


const searchSystemUsers = () => {
  console.log('触发搜索 - 用户类型:', userType.value, '特需标识:', specialNeed.value, '搜索关键词:', searchQuery.value);
  currentPage.value = 1;
  loadUsers();
};


const resetFilters = () => {
  searchQuery.value = '';
  userType.value = '';
  specialNeed.value = '';
  currentPage.value = 1;
  loadUsers();
};


const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadUsers();
};

const handleCurrentChange = (current: number) => {
  currentPage.value = current;
  loadUsers();
};


const loadUserProfile = async (userId: number) => {
  profileLoading.value = true;
  userProfile.value = null;
  try {
    const response = await apiClient.get(`/api/pattern-analysis/profile/${userId}`);
    userProfile.value = response;
  } catch (error) {
    // Ignore 404, it just means no profile
    console.log('No profile found or error', error);
  } finally {
    profileLoading.value = false;
  }
};

const generateUserProfile = async () => {
  if (!selectedUser.value) return;
  profileLoading.value = true;
  try {
    const response = await apiClient.post(`/api/pattern-analysis/generate/${selectedUser.value.id}`);
    userProfile.value = response;
    ElMessage.success('用户画像生成成功');
  } catch (error: any) {
    console.error('生成画像失败:', error);
    ElMessage.error('生成画像失败: ' + (error.response?.data?.error || error.message));
  } finally {
    profileLoading.value = false;
  }
};


const viewUserDetails = (user: User) => {
  selectedUser.value = { ...user };
  showUserDetails.value = true;
  loadUserProfile(user.id);
};


const editUser = (user: User) => {
  
  Object.assign(editForm, user);
  
  delete editForm.password;

  showEditUser.value = true;
};


const submitEditForm = async () => {
  if (!editFormRef.value) return;
  
  try {
    await editFormRef.value.validate();
    
    
    const submitData = { ...editForm };
    if (!submitData.password) {
      delete submitData.password;

    };
        
    console.log('Submitting user data:', submitData);
    await apiClient.put(`/api/system-users/${editForm.id}`, submitData);
    
    loadUsers();
    showEditUser.value = false;
    
    Object.keys(editForm).forEach((key) => {
      delete (editForm as Record<string, any>)[key];
    });
    
    ElMessage.success('用户信息更新成功');
  } catch (error) {
    console.error('更新用户信息失败:', error);
        ElMessage.error('更新用户信息失败，请稍后重试');

  }
};


const searchUsers = () => {
  searchSystemUsers();
};

const deleteUser = (userId: number) => {
  ElMessageBox.confirm('确定要删除这个用户吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      
      await apiClient.delete(`/api/system-users/${userId}`);
      
      loadUsers();
      ElMessage.success('用户删除成功');
    } catch (error) {
      console.error('删除用户失败:', error);
      ElMessage.error('删除用户失败，请稍后重试');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};


onMounted(() => {
  loadUsers();
});


const getSpecialUserTypes = (user: any) => {
  const types = determineSpecialUserTypes({
    isSingleAlone: user.isSingleAlone,
    isCoupleAlone: user.isCoupleAlone,
    isMobilityImpaired: user.isMobilityImpaired,
    isRelianceEquipment: user.isRelianceEquipment,
    isChronicDisease: user.isChronicDisease,
    isRelianceEnvironment: user.isRelianceEnvironment,
    isProduction: user.isProduction
  });
  return getSpecialUserTypeText(types);
};


const userColumns = [
  { prop: 'id', label: 'ID', width: 80, editable: false },
  { prop: 'name', label: '姓名', required: true },
  { prop: 'idCard', label: '身份证号', required: true, width: 180 },
  { prop: 'gender', label: '性别', type: 'select', options: [{ label: '男', value: '男' }, { label: '女', value: '女' }], width: 80 },
  { prop: 'birthDate', label: '出生日期', type: 'date', width: 120 },
  { prop: 'phoneNumber', label: '联系电话', width: 130 },
  { prop: 'address', label: '家庭住址', width: 200 },
  { prop: 'emergencyContactName', label: '紧急联系人', width: 120 },
  { prop: 'emergencyContactPhone', label: '紧急联系电话', width: 130 },
  { prop: 'userType', label: '用户类型', type: 'select', options: [{ label: '住宅用户', value: '住宅用户' }, { label: '商业用户', value: '商业用户' }, { label: '工业用户', value: '工业用户' }], width: 120 },
  { prop: 'powerSupplyArea', label: '供电片区', width: 120 },
  { prop: 'community', label: '社区归属', width: 120 },
  { prop: 'meterNumber', label: '电表号', required: true, width: 130 },
  { prop: 'communityManagerName', label: '社区责任人', width: 120 },
  { prop: 'communityManagerPhone', label: '责任人电话', width: 130 },
  { prop: 'isSingleAlone', label: '单人独居老人', type: 'boolean', width: 80 },
  { prop: 'isCoupleAlone', label: '双人独居老人', type: 'boolean', width: 80 },
  { prop: 'isMobilityImpaired', label: '行动不便老人', type: 'boolean', width: 80 },
  { prop: 'isRelianceEquipment', label: '医疗设备依赖', type: 'boolean', width: 80 },
  { prop: 'isChronicDisease', label: '康复/慢病', type: 'boolean', width: 80 },
  { prop: 'isRelianceEnvironment', label: '环境依赖', type: 'boolean', width: 80 },
  { prop: 'isProduction', label: '小型生产用户', type: 'boolean', width: 80 },
  { prop: 'deviceUsageNotes', label: '设备使用习惯', type: 'textarea', width: 200 },
  { prop: 'isActive', label: '是否激活', type: 'boolean', width: 100 },
  { prop: 'createTime', label: '创建时间', type: 'date', editable: false, width: 120 }
]

const elecColumns = [
  { prop: 'username', label: '用户姓名', width: 120, computed: true, formatter: (row: any) => row.user?.name || '-' },
  { prop: 'recordTime', label: '时间' },
  { prop: 'electricityConsumption', label: '电量' },
  { prop: 'power', label: '功率' }
]


</script>

<style scoped>
.container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
  width: 100%;
  max-width: none;
}

h2 {
  margin-bottom: 20px;
  color: #303133;
}

.search-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-input {
  width: 250px;
}

.filter-select {
  width: 150px;
}

.user-table {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-details {
  padding: 10px 0;
}

.detail-row {
  margin-bottom: 10px;
}

.detail-label {
  display: inline-block;
  width: 100px;
  color: #606266;
}

.special-needs-section {
  margin-top: 20px;
}

.special-needs-section h4 {
  margin-bottom: 10px;
  color: #303133;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>