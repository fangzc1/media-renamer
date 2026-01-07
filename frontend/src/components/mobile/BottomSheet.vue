<template>
  <teleport to="body">
    <transition name="fade">
      <div
        v-if="modelValue"
        class="bottom-sheet-overlay"
        @click="handleOverlayClick"
      ></div>
    </transition>

    <transition name="slide-up">
      <div
        v-if="modelValue"
        class="bottom-sheet"
        :class="[`size-${size}`, { dragging: isDragging }]"
        :style="{ transform: `translateY(${dragOffset}px)` }"
      >
        <!-- 拖拽手柄 -->
        <div
          class="bottom-sheet-handle"
          @touchstart="handleDragStart"
          @touchmove="handleDragMove"
          @touchend="handleDragEnd"
        ></div>

        <!-- 头部 -->
        <div v-if="title || $slots.header" class="bottom-sheet-header">
          <slot name="header">
            <div class="header-title">{{ title }}</div>
            <el-button
              text
              circle
              @click="handleClose"
              class="close-btn"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </slot>
        </div>

        <!-- 内容 -->
        <div class="bottom-sheet-body">
          <slot></slot>
        </div>

        <!-- 底部操作区 -->
        <div v-if="$slots.footer" class="bottom-sheet-footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large', 'full'].includes(value)
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  },
  closeOnSwipeDown: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'close', 'opened', 'closed'])

// 拖拽相关
const isDragging = ref(false)
const dragOffset = ref(0)
const startY = ref(0)
const currentY = ref(0)

// 拖拽开始
const handleDragStart = (event) => {
  if (!props.closeOnSwipeDown) return

  isDragging.value = true
  startY.value = event.touches[0].clientY
  currentY.value = startY.value
}

// 拖拽中
const handleDragMove = (event) => {
  if (!isDragging.value) return

  currentY.value = event.touches[0].clientY
  const diff = currentY.value - startY.value

  // 只允许向下拖拽
  if (diff > 0) {
    dragOffset.value = diff
  }
}

// 拖拽结束
const handleDragEnd = () => {
  if (!isDragging.value) return

  isDragging.value = false

  // 如果拖拽超过 100px，关闭抽屉
  if (dragOffset.value > 100) {
    handleClose()
  }

  // 重置偏移
  dragOffset.value = 0
}

// 点击遮罩关闭
const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    handleClose()
  }
}

// 关闭抽屉
const handleClose = () => {
  emit('update:modelValue', false)
  emit('close')
}

// 监听打开/关闭状态
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      // 禁用背景滚动
      document.body.style.overflow = 'hidden'
      emit('opened')
    } else {
      // 恢复背景滚动
      document.body.style.overflow = ''
      emit('closed')
    }
  }
)
</script>

<style scoped>
/* 遮罩层 */
.bottom-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  backdrop-filter: blur(2px);
}

/* 抽屉容器 */
.bottom-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--surface-1);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  z-index: 1001;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}

/* 拖拽状态 */
.bottom-sheet.dragging {
  transition: none;
}

/* 尺寸变体 */
.bottom-sheet.size-small {
  max-height: 40vh;
}

.bottom-sheet.size-medium {
  max-height: 60vh;
}

.bottom-sheet.size-large {
  max-height: 80vh;
}

.bottom-sheet.size-full {
  max-height: 95vh;
  border-radius: 0;
}

/* 拖拽手柄 */
.bottom-sheet-handle {
  width: 40px;
  height: 4px;
  background: var(--border-focus);
  border-radius: 2px;
  margin: 12px auto 8px;
  cursor: grab;
  touch-action: none;
}

.bottom-sheet-handle:active {
  cursor: grabbing;
}

/* 头部 */
.bottom-sheet-header {
  padding: var(--space-md) var(--mobile-gutter);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.header-title {
  font-size: var(--mobile-font-lg);
  font-weight: 600;
  color: var(--text-main);
  text-align: center;
  flex: 1;
}

.close-btn {
  position: absolute;
  right: var(--mobile-gutter);
  width: var(--touch-target-min);
  height: var(--touch-target-min);
  font-size: 20px;
}

/* 内容区 */
.bottom-sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--mobile-gutter);
  -webkit-overflow-scrolling: touch;
}

/* 底部操作区 */
.bottom-sheet-footer {
  padding: var(--space-md) var(--mobile-gutter);
  border-top: 1px solid var(--border-subtle);
  background: var(--surface-2);
  flex-shrink: 0;
  /* iOS 安全区域适配 */
  padding-bottom: calc(var(--space-md) + env(safe-area-inset-bottom));
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

/* 深色模式优化 */
[data-theme="dark"] .bottom-sheet {
  background: var(--surface-1);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.5);
}

/* 滚动条样式 */
.bottom-sheet-body::-webkit-scrollbar {
  width: 4px;
}

.bottom-sheet-body::-webkit-scrollbar-thumb {
  background: var(--border-focus);
  border-radius: 2px;
}

.bottom-sheet-body::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
