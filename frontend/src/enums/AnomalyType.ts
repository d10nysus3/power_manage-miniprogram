
export enum AnomalyType {
  POWER_OUTAGE = 'POWER_OUTAGE',
  POWER_SURGE = 'POWER_SURGE',
  POWER_DROP = 'POWER_DROP'
}


export const AnomalyTypeLabels: Record<AnomalyType, string> = {
  [AnomalyType.POWER_OUTAGE]: '瞬时断电',
  [AnomalyType.POWER_SURGE]: '功率突增',
  [AnomalyType.POWER_DROP]: '功率突降'
}


export const AnomalyTypeDescriptions: Record<AnomalyType, string> = {
  [AnomalyType.POWER_OUTAGE]: '电力供应瞬间中断，可能影响用户正常用电',
  [AnomalyType.POWER_SURGE]: '用电功率突然大幅增加，可能存在用电安全隐患',
  [AnomalyType.POWER_DROP]: '用电功率突然大幅下降，可能表示设备故障或异常用电行为'
}


export const AnomalyTypeColors: Record<AnomalyType, string> = {
  [AnomalyType.POWER_OUTAGE]: '#f56c6c',
  [AnomalyType.POWER_SURGE]: '#ff7875',
  [AnomalyType.POWER_DROP]: '#ffa940'
}


export function getAnomalyTypeOptions() {
  return Object.entries(AnomalyTypeLabels).map(([value, label]) => ({
    value,
    label
  }))
}


export function getAnomalyTypeColor(type: AnomalyType): string {
  return AnomalyTypeColors[type] || '#909399'
}


export function getAnomalyTypeText(type: AnomalyType): string {
  return AnomalyTypeLabels[type] || '未知类型'
}