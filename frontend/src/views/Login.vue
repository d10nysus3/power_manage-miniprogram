<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">系统登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">登录</el-button>
        </el-form-item>
        <el-form-item>
          <span>还没有账号？</span>
          <a href="#" @click.prevent="goToRegister">立即注册</a>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import apiClient from '../utils/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref<FormInstance>()
const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 页面加载时检查是否有记住的登录信息
const initLoginForm = () => {
  // 检查localStorage中是否有保存的登录信息
  const savedUsername = localStorage.getItem('rememberedUsername');
  const savedPassword = localStorage.getItem('rememberedPassword');
  if (savedUsername && savedPassword) {
    form.username = savedUsername;
    form.password = savedPassword;
    form.rememberMe = true;
  }
};

// 初始化表单
initLoginForm();

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const resp = await apiClient.post('auth/login', { username: form.username, password: form.password }) as any
      
      // 根据记住我选项决定使用localStorage还是sessionStorage
      const storage = form.rememberMe ? localStorage : sessionStorage;
      storage.setItem('token', resp.token)
      storage.setItem('username', resp.username)
      storage.setItem('role', resp.role)
      storage.setItem('userId', resp.userId.toString())
      
      // 如果勾选了记住我，保存用户名和密码
      if ((form as any).rememberMe) {
        localStorage.setItem('rememberedUsername', form.username);
        localStorage.setItem('rememberedPassword', form.password);
      } else {
        // 未勾选时清除可能存在的记住信息
        localStorage.removeItem('rememberedUsername');
        localStorage.removeItem('rememberedPassword');
      }
      
      window.dispatchEvent(new Event('login-success'))
      
      if (resp.role === 'MANAGER') {
          router.push('/user-profile')
      } else {
        router.push('/')
      }
      ElMessage.success('登录成功')
    } catch (e) {
      ElMessage.error('登录失败')
    }
  })
};

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.login-container { display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f7fa; }
.login-card { width: 380px; }
.title { text-align: center; margin-bottom: 10px; }
</style>