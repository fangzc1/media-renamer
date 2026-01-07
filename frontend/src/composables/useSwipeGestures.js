import { ref } from 'vue'

/**
 * 滑动手势 Composable
 * 提供左滑/右滑手势检测功能
 */
export function useSwipeGestures(options = {}) {
  const {
    threshold = 50, // 滑动阈值（像素）
    onSwipeLeft = null,
    onSwipeRight = null,
    onSwipeUp = null,
    onSwipeDown = null
  } = options

  const startX = ref(0)
  const startY = ref(0)
  const endX = ref(0)
  const endY = ref(0)
  const isSwiping = ref(false)

  /**
   * 处理触摸开始
   */
  const handleTouchStart = (event) => {
    const touch = event.touches[0]
    startX.value = touch.clientX
    startY.value = touch.clientY
    isSwiping.value = true
  }

  /**
   * 处理触摸移动
   */
  const handleTouchMove = (event) => {
    if (!isSwiping.value) return

    const touch = event.touches[0]
    endX.value = touch.clientX
    endY.value = touch.clientY
  }

  /**
   * 处理触摸结束
   */
  const handleTouchEnd = (event, payload) => {
    if (!isSwiping.value) return

    const diffX = startX.value - endX.value
    const diffY = startY.value - endY.value
    const absDiffX = Math.abs(diffX)
    const absDiffY = Math.abs(diffY)

    // 判断是水平滑动还是垂直滑动
    if (absDiffX > absDiffY) {
      // 水平滑动
      if (absDiffX > threshold) {
        if (diffX > 0) {
          // 左滑
          onSwipeLeft?.(payload, event)
        } else {
          // 右滑
          onSwipeRight?.(payload, event)
        }
      }
    } else {
      // 垂直滑动
      if (absDiffY > threshold) {
        if (diffY > 0) {
          // 上滑
          onSwipeUp?.(payload, event)
        } else {
          // 下滑
          onSwipeDown?.(payload, event)
        }
      }
    }

    // 重置状态
    isSwiping.value = false
    startX.value = 0
    startY.value = 0
    endX.value = 0
    endY.value = 0
  }

  /**
   * 取消滑动
   */
  const handleTouchCancel = () => {
    isSwiping.value = false
  }

  return {
    isSwiping,
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
    handleTouchCancel
  }
}
