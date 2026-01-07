<template>
  <div class="vertical-steps">
    <div
      v-for="(step, index) in steps"
      :key="index"
      class="step-item"
      :class="{
        'is-active': index === currentStep,
        'is-finished': index < currentStep,
        'is-future': index > currentStep,
        'is-clickable': index < currentStep
      }"
      @click="handleStepClick(index)"
    >
      <div class="step-icon">
        <el-icon v-if="index < currentStep" class="check-icon"><CircleCheck /></el-icon>
        <span v-else class="step-number">{{ index + 1 }}</span>
      </div>
      <div class="step-content">
        <div class="step-title">{{ step.title }}</div>
        <div class="step-description">{{ step.description }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { CircleCheck } from '@element-plus/icons-vue'

const props = defineProps({
  currentStep: {
    type: Number,
    default: 0
  },
  steps: {
    type: Array,
    default: () => [
      { title: '扫描文件', description: '选择媒体目录' },
      { title: '匹配媒体', description: '智能识别信息' },
      { title: '预览重命名', description: '确认命名规则' },
      { title: '执行重命名', description: '批量处理文件' }
    ]
  }
})

const emit = defineEmits(['step-click'])

// 处理步骤点击
const handleStepClick = (index) => {
  // 仅允许点击已完成的步骤 (index < currentStep)
  if (index < props.currentStep) {
    emit('step-click', index)
  }
}
</script>

<style scoped>
.vertical-steps {
  padding: var(--spacing-xl) var(--spacing-lg);
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  border-radius: var(--border-radius-md);
  transition: all var(--transition-base);
  cursor: default;
  position: relative;
}

/* 已完成步骤 - 可点击 */
.step-item.is-clickable {
  cursor: pointer;
}

/* 当前步骤 - 默认光标 */
.step-item.is-active {
  cursor: default;
}

/* 未来步骤 - 禁止光标 */
.step-item.is-future {
  cursor: not-allowed;
}

.step-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 27px;
  top: 60px;
  width: 2px;
  height: calc(100% - 30px);
  background-color: var(--border-color);
  transition: background-color var(--transition-base);
}

.step-item.is-finished::after {
  background-color: var(--success-color);
}

.step-item.is-active::after {
  background-color: var(--primary-color);
}

/* Hover 效果 - 只对已完成步骤生效 */
.step-item.is-clickable:hover {
  background-color: var(--bg-color);
}

.step-item.is-active {
  background-color: var(--primary-color-light);
  box-shadow: var(--shadow-sm);
}

.step-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all var(--transition-base);
  background-color: var(--border-color);
  color: var(--text-color-secondary);
}

.step-item.is-active .step-icon {
  background-color: var(--primary-color);
  color: white;
  box-shadow: 0 0 0 4px var(--primary-color-lighter);
}

.step-item.is-finished .step-icon {
  background-color: var(--success-color);
  color: white;
}

.step-number {
  font-size: 16px;
}

.check-icon {
  font-size: 20px;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color-primary);
  margin-bottom: 4px;
  transition: color var(--transition-base);
}

.step-item.is-active .step-title {
  color: var(--primary-color);
}

.step-description {
  font-size: 13px;
  color: var(--text-color-secondary);
  line-height: 1.4;
}

/* 深色模式 */
[data-theme="dark"] .step-item.is-clickable:hover {
  background-color: var(--bg-color-dark);
}

[data-theme="dark"] .step-item.is-active {
  background-color: rgba(66, 165, 245, 0.15);
}
</style>
