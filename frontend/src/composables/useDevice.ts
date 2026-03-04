 import { ref, onMounted, onUnmounted, computed } from 'vue';

// 定义移动端屏幕宽度阈值
const MOBILE_WIDTH_THRESHOLD = 768;

export function useDevice() {
  // 响应式状态：是否为移动端
  const isMobile = ref(false);

  // 检测当前设备类型的函数
  const checkDeviceType = () => {
    isMobile.value = window.innerWidth <= MOBILE_WIDTH_THRESHOLD;
  };

  // 计算属性：返回当前屏幕宽度
  const screenWidth = computed(() => window.innerWidth);

  // 计算属性：返回当前屏幕高度
  const screenHeight = computed(() => window.innerHeight);

  // 监听窗口 resize 事件
  onMounted(() => {
    // 初始化时检查一次
    checkDeviceType();
    // 添加 resize 事件监听器
    window.addEventListener('resize', checkDeviceType);
  });

  // 组件卸载时移除事件监听器
  onUnmounted(() => {
    window.removeEventListener('resize', checkDeviceType);
  });

  return {
    isMobile,
    screenWidth,
    screenHeight,
    checkDeviceType
  };
}