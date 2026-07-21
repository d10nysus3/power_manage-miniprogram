<template>
  <div class="login-container">
    <div class="login-form-wrapper">
      <h2 class="login-title">用户注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            placeholder="请输入用户名"
            :class="{ 'error': errors.username }"
          />
          <div v-if="errors.username" class="error-message">{{ errors.username }}</div>
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            placeholder="请输入密码"
            :class="{ 'error': errors.password }"
          />
          <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
        </div>

        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input
            type="password"
            id="confirmPassword"
            v-model="form.confirmPassword"
            placeholder="请再次输入密码"
            :class="{ 'error': errors.confirmPassword }"
          />
          <div v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</div>
        </div>

        <div class="form-group">
          <label for="role">角色</label>
          <select
            id="role"
            v-model="form.role"
            :class="{ 'error': errors.role }"
          >
            <option value="">请选择角色</option>
            <option value="admin">电力公司人员</option>
            <option value="normal">社区人员</option>
          </select>
          <div v-if="errors.role" class="error-message">{{ errors.role }}</div>
        </div>

        <div v-if="form.role === 'normal'" class="form-group">
          <label for="managedCommunity">负责社区</label>
          <input
            type="text"
            id="managedCommunity"
            v-model="form.managedCommunity"
            placeholder="请输入该管理员负责的社区名称"
            :class="{ 'error': errors.managedCommunity }"
          />
          <div v-if="errors.managedCommunity" class="error-message">{{ errors.managedCommunity }}</div>
        </div>

        <button type="submit" class="login-button" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="login-link">
          <span>已有账号？</span>
          <a href="#" @click.prevent="goToLogin">返回登录</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import apiClient from '../utils/api';

const router = useRouter();
const loading = ref(false);

interface FormData {
  username: string;
  password: string;
  confirmPassword: string;
  role: string;
  managedCommunity: string;
}

interface Errors {
  username: string;
  password: string;
  confirmPassword: string;
  role: string;
  managedCommunity: string;
}

const form = reactive<FormData>({
  username: '',
  password: '',
  confirmPassword: '',
  role: '',
  managedCommunity: ''
});

const errors = reactive<Errors>({
  username: '',
  password: '',
  confirmPassword: '',
  role: '',
  managedCommunity: ''
});

// 表单验证函数
const validateForm = (): boolean => {
  let isValid = true;

  // 重置错误信息
  errors.username = '';
  errors.password = '';
  errors.confirmPassword = '';
  errors.role = '';
  errors.managedCommunity = '';

  // 验证用户名
  if (!form.username.trim()) {
    errors.username = '请输入用户名';
    isValid = false;
  } else if (form.username.trim().length < 3) {
    errors.username = '用户名长度至少为3个字符';
    isValid = false;
  }

  // 验证密码
  if (!form.password) {
    errors.password = '请输入密码';
    isValid = false;
  } else if (form.password.length < 6) {
    errors.password = '密码长度至少为6个字符';
    isValid = false;
  }

  // 验证确认密码
  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码';
    isValid = false;
  } else if (form.confirmPassword !== form.password) {
    errors.confirmPassword = '两次输入的密码不一致';
    isValid = false;
  }

  // 验证角色
  if (!form.role) {
    errors.role = '请选择角色';
    isValid = false;
  }

  if (form.role === 'normal' && !form.managedCommunity.trim()) {
    errors.managedCommunity = '请输入该管理员负责的社区名称';
    isValid = false;
  }

  return isValid;
};

// 处理注册
const handleRegister = async () => {
  // 表单验证
  if (!validateForm()) {
    return;
  }

  loading.value = true;

  try {
    // 直接使用apiClient调用注册接口
    const response = await apiClient.post('auth/register', {
      username: form.username,
      password: form.password,
      role: form.role,
      managedCommunity: form.role === 'normal' ? form.managedCommunity.trim() : undefined
    });
    ElMessage.success('注册成功，请登录');
    
    // 跳转到登录页面
    router.push('/login');
  } catch (error: any) {
    // 处理错误
    console.error('注册请求失败:', error);
    console.error('错误详情:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: error.config?.url,
      data: error.response?.data
    });
    
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(error.response.data.message);
    } else if (error.response && error.response.status === 409) {
      // 用户名已存在
      errors.username = '用户名已存在，请选择其他用户名';
    } else if (error.response && error.response.status === 404) {
      ElMessage.error('请求的资源不存在，请检查服务是否正常运行');
    } else {
      ElMessage.error('注册失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
};

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-form-wrapper {
  background-color: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #409eff;
}

.form-group input.error,
.form-group select.error {
  border-color: #f56c6c;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.login-button {
  width: 100%;
  padding: 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.login-button:hover:not(:disabled) {
  background-color: #66b1ff;
}

.login-button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

.login-link a {
  color: #409eff;
  text-decoration: none;
  margin-left: 5px;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
