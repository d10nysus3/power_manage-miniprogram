export interface User {
  id: number
  name: string
  phoneNumber: string
  address: string
  specialUserType: string
  deviceCount?: number
}

export interface AnomalyRecord {
  id: number
  userName: string
  alertType: string
  triggerReason: string
  alertTime: string
  severityLevel: string
  alertContent: string
  isProcessed: boolean
  processTime?: string
  processor?: string
}

export interface SystemStatus {
  totalUsers: number
  todayAnomalies: number
  processingRate: string
  todayElectricity: string
  userTrend: string
  anomalyTrend: string
  rateTrend: string
  electricityTrend: string
}

export interface UserInfo {
  token: string
  username: string
  role: string
  userId: string
}

export interface PageData<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

