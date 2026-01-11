<template>
  <div class="mobile-step-navigator">
    <div class="steps-container">
      <div
        v-for="step in steps"
        :key="step.value"
        class="step-item"
        :class="{
          'is-active': step.value === currentStep,
          'is-completed': step.value < currentStep,
          'is-future': step.value > currentStep
        }"
        @click="handleStepClick(step)"
      >
        <div class="step-circle">
          <el-icon v-if="step.value < currentStep" class="step-icon">
            <Check />
          </el-icon>
          <span v-else class="step-number">{{ step.value }}</span>
        </div>
        <div v-if="step.value < steps.length" class="step-line"></div>
      </div>
    </div>
    <div class="step-title">{{ currentStepTitle }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  currentStep: {
    type: Number,
    required: true,
    validator: (value) => value >= 1 && value <= 4
  },
  allowJump: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['change'])

const steps = [
  { value: 1, title: '扫描文件' },
  { value: 2, title: '匹配媒体信息' },
  { value: 3, title: '预览重命名' },
  { value: 4, title: '执行重命名' }
]

const currentStepTitle = computed(() => {
  return steps.find(s => s.value === props.currentStep)?.title || ''
})

const handleStepClick = (step) => {
  // 仅允许跳转到已完成的步骤
  if (props.allowJump && step.value < props.currentStep) {
    // 传递索引（从 0 开始）而不是步骤值（从 1 开始）
    emit('change', step.value - 1)
  }
}
</script>

<style scoped>
.mobile-step-navigator {
  background: var(--surface-1);
  border-bottom: 1px solid var(--border-subtle);
  padding: var(--space-md) var(--mobile-gutter);
}

.steps-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: var(--space-sm);
}

.step-item {
  display: flex;
  align-items: center;
  cursor: default;
}

.step-item.is-completed {
  cursor: pointer;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--mobile-font-sm);
  font-weight: 600;
  background: var(--surface-2);
  color: var(--text-tertiary);
  border: 2px solid var(--border-subtle);
  transition: all var(--transition-base);
  position: relative;
  z-index: 1;
}

.step-item.is-active .step-circle {
  background: var(--primary-brand);
  color: white;
  border-color: var(--primary-brand);
  box-shadow: 0 0 0 4px var(--primary-surface);
}

.step-item.is-completed .step-circle {
  background: var(--success-text);
  color: white;
  border-color: var(--success-text);
}

.step-item.is-future .step-circle {
  background: var(--surface-1);
  color: var(--text-tertiary);
  border-color: var(--border-subtle);
}

.step-icon {
  font-size: 18px;
}

.step-number {
  font-size: 14px;
}

.step-line {
  width: 40px;
  height: 2px;
  background: var(--border-subtle);
  transition: background var(--transition-base);
}

.step-item.is-completed .step-line {
  background: var(--success-text);
}

.step-item.is-active .step-line {
  background: linear-gradient(
    to right,
    var(--primary-brand),
    var(--border-subtle)
  );
}

.step-title {
  text-align: center;
  font-size: var(--mobile-font-base);
  font-weight: 500;
  color: var(--text-main);
  line-height: var(--mobile-line-height-tight);
}

/* 深色模式优化 */
[data-theme="dark"] .step-circle {
  background: var(--surface-2);
}

[data-theme="dark"] .step-item.is-future .step-circle {
  background: var(--surface-1);
}

/* 点击反馈 */
.step-item.is-completed:active .step-circle {
  transform: scale(0.95);
}

@media (max-width: 480px) {
  .step-circle {
    width: 28px;
    height: 28px;
  }

  .step-number {
    font-size: 12px;
  }

  .step-line {
    width: 32px;
  }

  .step-title {
    font-size: var(--mobile-font-sm);
  }
}
</style>
