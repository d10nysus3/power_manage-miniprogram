/**
 * 权限管理工具
 */

/// <reference types="@dcloudio/types" />

// 获取用户信息
export function getUserInfo() {
  return {
    token: uni.getStorageSync('token'),
    username: uni.getStorageSync('username'),
    role: uni.getStorageSync('role'),
    userId: uni.getStorageSync('userId')
  }
}

// 设置用户信息
export function setUserInfo(userInfo: {
  token: string
  username: string
  role: string
  userId: string
}) {
  uni.setStorageSync('token', userInfo.token)
  uni.setStorageSync('username', userInfo.username)
  uni.setStorageSync('role', userInfo.role)
  uni.setStorageSync('userId', userInfo.userId)
}

// 清除用户信息
export function clearUserInfo() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('username')
  uni.removeStorageSync('role')
  uni.removeStorageSync('userId')
}

// 检查是否登录
export function isLoggedIn(): boolean {
  return !!uni.getStorageSync('token')
}

// 检查用户角色权限
export function hasRole(requiredRoles: string[]): boolean {
  const role = uni.getStorageSync('role')
  return requiredRoles.includes(role)
}

// 获取角色显示名称
export function getRoleDisplayName(role: string): string {
  const roleMap: Record<string, string> = {
    'ADMIN': '电力公司人员',
    'MANAGER': '社区人员'
  }
  return roleMap[role] || '未知角色'
}

