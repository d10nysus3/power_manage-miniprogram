<template>
  <div class="container">
    <h2>个人信息</h2>
    <el-alert type="info" :closable="false" show-icon>
      管理员个人信息和账户安全设置
    </el-alert>
    
    <el-card class="info-card">
      <el-form :model="userInfo" label-width="120px">
        <el-form-item label="用户名">
          <el-input v-model="userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input v-model="userInfo.role" disabled />
        </el-form-item>
        <el-form-item label="最近登录时间">
          <el-input v-model="userInfo.lastLoginTime" disabled />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="security-card">
      <template #header>
        <div class="card-header">
          <span>账户安全</span>
        </div>
      </template>
      <el-form :model="securityForm" :rules="rules" ref="securityFormRef" label-width="120px">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="securityForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="securityForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="securityForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updatePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'



const userInfo = reactive({
  username: '',
  role: '系统管理员',
  lastLoginTime: ''
})

const securityForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const securityFormRef = ref<FormInstance>()

const rules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== securityForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  
  const username = localStorage.getItem('username')
  if (username) {
    userInfo.username = username
  }
  
  
  userInfo.lastLoginTime = new Date().toLocaleString('zh-CN')
})

const updatePassword = async () => {
  if (!securityFormRef.value) return
  
  await securityFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      
      
      
      
      ElMessage.success('密码修改成功，请重新登录')
      
      
      securityForm.currentPassword = ''
      securityForm.newPassword = ''
      securityForm.confirmPassword = ''
      securityFormRef.value?.resetFields()
      
      
      
      
    } catch (error) {
      ElMessage.error('密码修改失败，请重试')
      console.error('修改密码失败:', error)
    }
  })
}
</script>

<style scoped>
.container {
  padding: 20px;
}

.info-card,
.security-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>