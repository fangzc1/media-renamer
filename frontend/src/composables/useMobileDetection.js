import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * 移动端检测 Composable
 * 提供响应式的移动端设备和屏幕尺寸检测
 */
export function useMobileDetection() {
  const windowWidth = ref(window.innerWidth)

  // 断点定义
  const BREAKPOINTS = {
    mobile: 480,
    tablet: 768,
    desktop: 1024,
    large: 1440
  }

  // 响应式计算属性
  const isMobile = computed(() => windowWidth.value <= BREAKPOINTS.tablet)
  const isSmallMobile = computed(() => windowWidth.value <= BREAKPOINTS.mobile)
  const isTablet = computed(() =>
    windowWidth.value > BREAKPOINTS.mobile &&
    windowWidth.value <= BREAKPOINTS.tablet
  )
  const isDesktop = computed(() => windowWidth.value > BREAKPOINTS.tablet)
  const isLargeDesktop = computed(() => windowWidth.value > BREAKPOINTS.large)

  // 触摸设备检测
  const isTouchDevice = computed(() => {
    return (
      'ontouchstart' in window ||
      navigator.maxTouchPoints > 0 ||
      window.matchMedia('(pointer: coarse)').matches
    )
  })

  // 移动设备检测（移动端尺寸 + 触摸支持）
  const isMobileDevice = computed(() => {
    return isMobile.value && isTouchDevice.value
  })

  // 处理窗口大小变化
  const handleResize = () => {
    windowWidth.value = window.innerWidth
  }

  // 生命周期钩子
  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
  })

  return {
    // 窗口尺寸
    windowWidth,

    // 断点常量
    BREAKPOINTS,

    // 响应式状态
    isMobile,
    isSmallMobile,
    isTablet,
    isDesktop,
    isLargeDesktop,
    isTouchDevice,
    isMobileDevice
  }
}
