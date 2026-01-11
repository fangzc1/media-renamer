<template>
  <div class="mobile-bottom-bar">
    <el-button
      v-if="showPrev"
      class="prev-btn"
      @click="handlePrev"
      :disabled="prevDisabled"
    >
      {{ prevText }}
    </el-button>
    <el-button
      type="primary"
      class="next-btn"
      @click="handleNext"
      :loading="loading"
      :disabled="nextDisabled"
    >
      {{ nextText }}
    </el-button>
  </div>
</template>

<script setup>
const props = defineProps({
  showPrev: {
    type: Boolean,
    default: true
  },
  prevText: {
    type: String,
    default: '上一步'
  },
  prevDisabled: {
    type: Boolean,
    default: false
  },
  nextText: {
    type: String,
    default: '下一步'
  },
  nextDisabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['prev', 'next'])

const handlePrev = () => {
  if (!props.prevDisabled) {
    emit('prev')
  }
}

const handleNext = () => {
  if (!props.nextDisabled && !props.loading) {
    emit('next')
  }
}
</script>

<style scoped>
.mobile-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--mobile-bottom-bar-height);
  background: var(--surface-1);
  border-top: 1px solid var(--border-subtle);
  padding: var(--space-md) var(--mobile-gutter);
  display: flex;
  align-items: center; /* 确保按钮垂直居中对齐 */
  gap: var(--space-md);
  z-index: 100;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);

  /* iOS 安全区域适配 */
  padding-bottom: calc(var(--space-md) + env(safe-area-inset-bottom));
}

.prev-btn {
  flex: 1;
  height: var(--touch-target-min);
  min-height: var(--touch-target-min);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-size: var(--mobile-font-base);
  border-radius: var(--radius-md);
}

.next-btn {
  flex: 2;
  height: var(--touch-target-min);
  min-height: var(--touch-target-min);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-size: var(--mobile-font-base);
  font-weight: 600;
  border-radius: var(--radius-md);
}

/* 触控反馈 */
.prev-btn:active:not(:disabled),
.next-btn:active:not(:disabled) {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

/* 深色模式优化 */
[data-theme="dark"] .mobile-bottom-bar {
  background: var(--surface-1);
  border-top-color: var(--border-subtle);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.3);
}

/* 小屏手机优化 */
@media (max-width: 480px) {
  .mobile-bottom-bar {
    padding: var(--space-sm) var(--mobile-gutter);
    gap: var(--space-sm);
  }

  .prev-btn,
  .next-btn {
    font-size: var(--mobile-font-sm);
  }
}
</style>
