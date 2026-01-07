import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 文本滚动动画 Composable
 * 用于检测文本溢出并自动触发滚动播放（跑马灯效果）
 */
export function useTextScroll(textRef) {
  const isOverflow = ref(false)
  const scrollDuration = ref('10s')
  const containerWidth = ref('200px')

  /**
   * 检测文本是否溢出
   */
  const checkOverflow = () => {
    if (!textRef.value) return

    const element = textRef.value
    const parent = element.parentElement

    if (parent) {
      containerWidth.value = `${parent.clientWidth}px`
    }

    // 检测文本是否超出容器宽度
    isOverflow.value = element.scrollWidth > element.clientWidth

    if (isOverflow.value) {
      // 根据文本长度动态计算滚动时长（速度约 50px/s）
      const distance = element.scrollWidth - element.clientWidth
      scrollDuration.value = `${Math.max(distance / 50, 3)}s`
    }
  }

  onMounted(() => {
    // 延迟检测，确保 DOM 已完全渲染
    setTimeout(checkOverflow, 100)
    window.addEventListener('resize', checkOverflow)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkOverflow)
  })

  return {
    isOverflow,
    scrollDuration,
    containerWidth,
    checkOverflow
  }
}
