export enum SpecialUserType {
  SINGLE_ELDERLY = 'SINGLE_ELDERLY',
  DOUBLE_ELDERLY = 'DOUBLE_ELDERLY',
  MOBILITY_IMPAIRED = 'MOBILITY_IMPAIRED',
  MEDICAL_DEVICE_DEPENDENT = 'MEDICAL_DEVICE_DEPENDENT',
  RECOVERY_CHRONIC_PATIENT = 'RECOVERY_CHRONIC_PATIENT',
  ENVIRONMENT_DEPENDENT = 'ENVIRONMENT_DEPENDENT',
  SMALL_PRODUCTION_USER = 'SMALL_PRODUCTION_USER'
} 


export const SpecialUserTypeLabels: Record<SpecialUserType, string> = {
  [SpecialUserType.SINGLE_ELDERLY]: '单人独居老人',
  [SpecialUserType.DOUBLE_ELDERLY]: '双人独居老人',
  [SpecialUserType.MOBILITY_IMPAIRED]: '行动不便老人',
  [SpecialUserType.MEDICAL_DEVICE_DEPENDENT]: '医疗设备依赖用户',
  [SpecialUserType.RECOVERY_CHRONIC_PATIENT]: '康复/慢病患者',
  [SpecialUserType.ENVIRONMENT_DEPENDENT]: '环境依赖用户',
  [SpecialUserType.SMALL_PRODUCTION_USER]: '小型生产用户'
}


export const SpecialUserTypeDescriptions: Record<SpecialUserType, string> = {
  [SpecialUserType.SINGLE_ELDERLY]: '独居老人，无他人同住',
  [SpecialUserType.DOUBLE_ELDERLY]: '两位老人共同居住',
  [SpecialUserType.MOBILITY_IMPAIRED]: '行动能力受限的老人',
  [SpecialUserType.MEDICAL_DEVICE_DEPENDENT]: '依赖医疗设备维持生活的用户',
  [SpecialUserType.RECOVERY_CHRONIC_PATIENT]: '处于康复期或患有慢性病的用户',
  [SpecialUserType.ENVIRONMENT_DEPENDENT]: '对环境条件有特殊要求的用户',
  [SpecialUserType.SMALL_PRODUCTION_USER]: '从事小型生产活动的用户'
}


export function getSpecialUserTypeOptions() {
  return Object.entries(SpecialUserTypeLabels).map(([value, label]) => ({
    value,
    label
  }))
}


export function determineSpecialUserTypes(userFeatures: {
  isSingleAlone?: boolean;
  isCoupleAlone?: boolean;
  isMobilityImpaired?: boolean;
  isRelianceEquipment?: boolean;
  isChronicDisease?: boolean;
  isRelianceEnvironment?: boolean;
  isProduction?: boolean;
}): SpecialUserType[] {
  const types: SpecialUserType[] = []
  
  if (userFeatures.isSingleAlone) types.push(SpecialUserType.SINGLE_ELDERLY)
  if (userFeatures.isCoupleAlone) types.push(SpecialUserType.DOUBLE_ELDERLY)
  if (userFeatures.isMobilityImpaired) types.push(SpecialUserType.MOBILITY_IMPAIRED)
  if (userFeatures.isRelianceEquipment) types.push(SpecialUserType.MEDICAL_DEVICE_DEPENDENT)
  if (userFeatures.isChronicDisease) types.push(SpecialUserType.RECOVERY_CHRONIC_PATIENT)
  if (userFeatures.isRelianceEnvironment) types.push(SpecialUserType.ENVIRONMENT_DEPENDENT)
  if (userFeatures.isProduction) types.push(SpecialUserType.SMALL_PRODUCTION_USER)
  
  return types
}


export function getSpecialUserTypeText(types: SpecialUserType[]): string {
  if (types.length === 0) return '普通用户'
  return types.map(type => SpecialUserTypeLabels[type]).join('、')
}