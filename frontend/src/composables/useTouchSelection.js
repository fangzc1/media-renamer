import { ref } from 'vue'

/**
 * 触屏多选 Composable
 * 提供移动端长按进入选择模式的功能
 */
export function useTouchSelection(options = {}) {
  const {
    longPressDuration = 500, // 长按持续时间（毫秒）
    enableVibration = true // 是否启用振动反馈
  } = options

  const isSelectionMode = ref(false)
  const selectedItems = ref(new Set())
  const longPressTimer = ref(null)

  /**
   * 触发振动反馈
   */
  const vibrate = (duration = 50) => {
    if (enableVibration && 'vibrate' in navigator) {
      navigator.vibrate(duration)
    }
  }

  /**
   * 进入选择模式
   */
  const enterSelectionMode = (item) => {
    isSelectionMode.value = true
    if (item) {
      selectedItems.value.add(item.id || item)
    }
    vibrate()
  }

  /**
   * 退出选择模式
   */
  const exitSelectionMode = () => {
    isSelectionMode.value = false
    selectedItems.value.clear()
  }

  /**
   * 切换项目选中状态
   */
  const toggleItem = (item) => {
    const itemId = item.id || item
    if (selectedItems.value.has(itemId)) {
      selectedItems.value.delete(itemId)
    } else {
      selectedItems.value.add(itemId)
      vibrate(30)
    }

    // 如果没有选中项，退出选择模式
    if (selectedItems.value.size === 0) {
      exitSelectionMode()
    }
  }

  /**
   * 检查项目是否被选中
   */
  const isItemSelected = (item) => {
    const itemId = item.id || item
    return selectedItems.value.has(itemId)
  }

  /**
   * 全选
   */
  const selectAll = (items) => {
    items.forEach(item => {
      const itemId = item.id || item
      selectedItems.value.add(itemId)
    })
  }

  /**
   * 取消全选
   */
  const deselectAll = () => {
    selectedItems.value.clear()
  }

  /**
   * 处理触摸开始（长按检测）
   */
  const handleTouchStart = (item, event) => {
    // 如果已经在选择模式，直接切换选中状态
    if (isSelectionMode.value) {
      event.preventDefault()
      toggleItem(item)
      return
    }

    // 启动长按计时器
    longPressTimer.value = setTimeout(() => {
      enterSelectionMode(item)
    }, longPressDuration)
  }

  /**
   * 处理触摸移动（取消长按）
   */
  const handleTouchMove = () => {
    if (longPressTimer.value) {
      clearTimeout(longPressTimer.value)
      longPressTimer.value = null
    }
  }

  /**
   * 处理触摸结束（清除计时器）
   */
  const handleTouchEnd = () => {
    if (longPressTimer.value) {
      clearTimeout(longPressTimer.value)
      longPressTimer.value = null
    }
  }

  /**
   * 处理点击（在选择模式下切换选中状态）
   */
  const handleClick = (item, event) => {
    if (isSelectionMode.value) {
      event.preventDefault()
      event.stopPropagation()
      toggleItem(item)
    }
  }

  return {
    // 状态
    isSelectionMode,
    selectedItems,
    selectedCount: () => selectedItems.value.size,

    // 方法
    enterSelectionMode,
    exitSelectionMode,
    toggleItem,
    isItemSelected,
    selectAll,
    deselectAll,
    vibrate,

    // 事件处理
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
    handleClick
  }
}
