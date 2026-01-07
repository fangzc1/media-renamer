<template>
  <div class="progress-loading-overlay">
    <div class="loading-card">
      <!-- 动画图标 -->
      <div class="icon-wrapper">
        <el-icon class="loading-icon rotating" :size="56">
          <component :is="iconComponent" />
        </el-icon>
      </div>

      <!-- 主标题 -->
      <div class="loading-title">{{ title }}</div>

      <!-- 进度信息 -->
      <div class="progress-info">
        <span class="progress-text">{{ progressText }}</span>
        <span class="progress-count">{{ current }}/{{ total }}</span>
      </div>

      <!-- 进度条 -->
      <el-progress
        :percentage="percentage"
        :stroke-width="10"
        :show-text="false"
        class="progress-bar"
      />

      <!-- 额外提示 -->
      <div v-if="tip" class="loading-tip">{{ tip }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Loading, MagicStick, Check, Upload } from '@element-plus/icons-vue'

const props = defineProps({
  // 当前进度
  current: {
    type: Number,
    default: 0
  },
  // 总数
  total: {
    type: Number,
    default: 100
  },
  // 主标题
  title: {
    type: String,
    default: '正在处理...'
  },
  // 进度文本
  progressText: {
    type: String,
    default: '处理中'
  },
  // 图标类型
  icon: {
    type: String,
    default: 'loading',
    validator: (value) => ['loading', 'magic', 'check', 'upload'].includes(value)
  },
  // 额外提示
  tip: {
    type: String,
    default: ''
  }
})

// 计算百分比
const percentage = computed(() => {
  if (props.total === 0) return 0
  return Math.round((props.current / props.total) * 100)
})

// 动态图标组件
const iconComponent = computed(() => {
  const iconMap = {
    loading: Loading,
    magic: MagicStick,
    check: Check,
    upload: Upload
  }
  return iconMap[props.icon] || Loading
})
</script>

<style scoped>
/* 全屏遮罩 */
.progress-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 加载卡片 */
.loading-card {
  background: var(--surface-1, #ffffff);
  border-radius: var(--radius-lg, 16px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 48px 56px;
  min-width: 480px;
  text-align: center;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 图标区域 */
.icon-wrapper {
  margin-bottom: 24px;
}

.loading-icon {
  color: var(--primary-brand, #409eff);
  filter: drop-shadow(0 4px 12px rgba(64, 158, 255, 0.3));
}

.rotating {
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 标题 */
.loading-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-main, #303133);
  margin-bottom: 20px;
  letter-spacing: 0.02em;
}

/* 进度信息 */
.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}

.progress-text {
  font-size: 14px;
  color: var(--text-secondary, #606266);
  font-weight: 500;
}

.progress-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-brand, #409eff);
  font-variant-numeric: tabular-nums;
}

/* 进度条 */
.progress-bar {
  margin-bottom: 20px;
}

.progress-bar :deep(.el-progress-bar__outer) {
  background-color: var(--surface-2, #f5f7fa);
  border-radius: 8px;
}

.progress-bar :deep(.el-progress-bar__inner) {
  border-radius: 8px;
  background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
  transition: width 0.3s ease;
}

/* 提示文字 */
.loading-tip {
  font-size: 13px;
  color: var(--text-tertiary, #909399);
  margin-top: -8px;
  line-height: 1.6;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .loading-card {
    min-width: unset;
    width: calc(100vw - 32px);
    max-width: 400px;
    padding: 32px 24px;
  }

  .icon-wrapper {
    margin-bottom: 20px;
  }

  .loading-icon {
    font-size: 48px !important;
  }

  .loading-title {
    font-size: 18px;
    margin-bottom: 16px;
  }

  .progress-info {
    margin-bottom: 12px;
  }

  .progress-text {
    font-size: 13px;
  }

  .progress-count {
    font-size: 14px;
  }

  .loading-tip {
    font-size: 12px;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .loading-card {
    background: var(--surface-1, #1e1e1e);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
  }

  .progress-bar :deep(.el-progress-bar__outer) {
    background-color: var(--surface-2, #2a2a2a);
  }
}
</style>
